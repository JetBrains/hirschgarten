package org.jetbrains.bazel.sync.workspace.importer

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.magicmetamodel.formatAsLibraryName
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameProducer
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameSpace
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.utils.StringUtils
import java.util.concurrent.ConcurrentHashMap

@ApiStatus.Internal
interface GlobalNamingContext {
  fun find(space: NameSpace, producer: NameProducer, key: WorkspaceTargetKey): String?
  fun findOrFallback(space: NameSpace, producer: NameProducer, key: WorkspaceTargetKey): String

  enum class NameSpace { MODULE, LIBRARY }

  /**
   * Name producer, usually each language will provide its own
   *
   * A name claimed by more than one producer goes to none of them, each falls back to an [id] suffixed name instead
   *
   * @property id Unique language identifier e.g., jvm, python
   */
  data class NameProducer(val id: String)
}

@ApiStatus.Internal
interface GlobalNamingContextBuilder {
  /**
   * Declare name in global naming context, after resolution it can be retrieved
   * using [GlobalNamingContext.find] or [GlobalNamingContext.findOrFallback]
   *
   * @param space Namespace, each namespace is resolved separately
   * @param producer Unique producer owning name
   * @param key Key used to derive name during resolve
   */
  fun declare(space: NameSpace, producer: NameProducer, key: WorkspaceTargetKey, preferred: String? = null)

  fun build(): GlobalNamingContext

  companion object {
    fun create(repoMapping: RepoMapping): GlobalNamingContextBuilder = GlobalNamingContextBuilderImpl(repoMapping)
  }
}

private data class NameRequest(val space: NameSpace, val producer: NameProducer, val key: WorkspaceTargetKey)

private class GlobalNamingContextBuilderImpl(private val repoMapping: RepoMapping) : GlobalNamingContextBuilder {
  private val declared = linkedSetOf<NameRequest>()
  private val preferred = hashMapOf<NameRequest, String>()

  override fun declare(space: NameSpace, producer: NameProducer, key: WorkspaceTargetKey, preferred: String?) {
    val request = NameRequest(space, producer, key)
    declared += request
    if (preferred != null) {
      this.preferred.putIfAbsent(request, preferred)
    }
  }

  override fun build(): GlobalNamingContext =
    ResolvedNamingContext(
      repoMapping = repoMapping,
      names = buildMap {
        // namespaces are separate, so resolve them on their own
        for (space in NameSpace.entries) {
          val requests = declared.filterTo(mutableListOf()) { it.space == space }
          if (requests.isNotEmpty()) {
            putAll(resolve(requests.sortedWith(REQUEST_ORDER)))
          }
        }
      },
    )

  // worklist based algorithm for efficient conflict resolution between requested names
  // overall idea is simple, each `NameRequest` provides a list of lazy name candidates,
  // then we're trying each candidate for `NameRequest` until one is not taken, move to next candidate for
  // all requests and go through worklist until each name is resolved, as fallback hash-based names are selected
  private fun resolve(ordered: List<NameRequest>): Map<NameRequest, String> {
    val candidates = ordered.associateWith { candidates(repoMapping, it, preferred[it]) }
    val result = hashMapOf<NameRequest, String>()
    val taken = hashSetOf<String>()

    // most recent candidates
    val lastName = hashMapOf<NameRequest, String>()

    var pending = ordered
    var index = 0
    while (pending.isNotEmpty()) {
      // being absent from this map means request has run out of candidates
      val nameOf = hashMapOf<NameRequest, String>()
      for (request in pending) {
        val name = candidates.getValue(request).getOrNull(index)?.invoke() ?: continue
        nameOf[request] = name
        lastName[request] = name
      }

      // assign fallback names, usually shouldn't happen with the default candidates
      // but is kept here to provide soundness of name resolution
      for (request in pending) {
        if (request !in nameOf) {
          result[request] = fallbackName(request, lastName.getValue(request), taken).also { taken += it }
        }
      }

      val stillPending = mutableListOf<NameRequest>()
      // a name goes to the request that is the only one asking for it, everyone else moves to the next worklist
      // this is mainly about targets shared between languages, e.g. a target imported as both a jvm and a python
      // module leaves the plain name behind and both end up producer suffixed
      for ((name, group) in nameOf.entries.groupBy({ it.value }, { it.key })) {
        val winner = group.singleOrNull()?.takeIf { name !in taken }
        if (winner != null) {
          result[winner] = name
          taken += name
        }
        group.filterTo(stillPending) { it != winner }
      }
      // swap worklist and move to next candidate
      pending = stillPending
      index++
    }
    return result
  }
}

private class ResolvedNamingContext(
  private val repoMapping: RepoMapping,
  private val names: Map<NameRequest, String>,
) : GlobalNamingContext {
  private val taken: Map<NameSpace, Set<String>> =
    names.entries.groupBy({ it.key.space }, { it.value }).mapValues { (_, names) -> names.toSet() }
  private val fallbacks = ConcurrentHashMap<NameRequest, String>()

  override fun find(space: NameSpace, producer: NameProducer, key: WorkspaceTargetKey): String? =
    names[NameRequest(space, producer, key)]

  override fun findOrFallback(space: NameSpace, producer: NameProducer, key: WorkspaceTargetKey): String {
    val found = find(space, producer, key)
    if (found != null) {
      return found
    }
    return fallbacks.computeIfAbsent(NameRequest(space, producer, key)) { request ->
      fallbackName(
        request = request,
        base = candidates(repoMapping, request, preferred = null).first().invoke(),
        taken = taken[space].orEmpty(),
      )
    }
  }
}

private fun candidates(repoMapping: RepoMapping, request: NameRequest, preferred: String?): List<() -> String> {
  val key = request.key
  val producer = request.producer.id
  val (plain, decorated) = when (request.space) {
    NameSpace.MODULE -> Pair(
      { key.formatAsModuleName(repoMapping, withConfiguration = false) },
      { key.formatAsModuleName(repoMapping, withConfiguration = true) },
    )

    NameSpace.LIBRARY -> Pair(
      { key.formatAsLibraryName(repoMapping, withFullKey = false) },
      { key.formatAsLibraryName(repoMapping, withFullKey = true) },
    )
  }
  // intellij.bazel.commons   preferred, only when one was declared
  // a.b                      plain
  // a.b-python               plain + producer
  // a.b-a1b2c3d              plain + configuration
  // a.b-a1b2c3d-python       plain + configuration + producer
  return listOfNotNull(
    preferred?.let { name -> { name } },
    plain,
    { "${plain()}-$producer" },
    decorated,
    { "${decorated()}-$producer" },
  )
}

private fun fallbackName(request: NameRequest, base: String, taken: Set<String>): String {
  val decorated = "$base-${StringUtils.md5Hash("${request.producer.id}|${request.key}", 5)}"
  if (decorated !in taken) {
    return decorated
  }
  log.warn("naming collision disambiguation failed for $request, falling back to counter name")
  return generateSequence(1) { it + 1 }
    .map { "$decorated-$it" }
    .first { it !in taken }
}

private val log = logger<GlobalNamingContext>()

private val REQUEST_ORDER: Comparator<NameRequest> =
  compareBy<NameRequest> { it.producer.id }
    .thenBy { it.key.label }
    .thenBy { it.key.configuration.shortChecksum }
    .thenBy { it.key.aspectIds.ids.joinToString(",") }
