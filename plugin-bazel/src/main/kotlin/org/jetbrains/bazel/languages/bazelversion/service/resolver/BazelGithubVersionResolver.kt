package org.jetbrains.bazel.languages.bazelversion.service.resolver

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import com.intellij.util.text.SemVer
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.future
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.withNewVersionWhenPossible
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionResolver
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

class BazelGithubVersionResolver : BazelVersionResolver {
  val githubGson =
    GsonBuilder()
      .setPrettyPrinting()
      .serializeNulls()
      .disableHtmlEscaping()
      .create()

  override val id: String = ID
  override val name: String = "Github"

  fun getForkOfSupportedBazelVersion(version: BazelVersionLiteral?) =
    when (version) {
      is BazelVersionLiteral.Forked -> version.fork
      is BazelVersionLiteral.Specific, null -> DEFAULT_FORK_NAME
      else -> null
    }

  override suspend fun resolveLatestBazelVersion(project: Project, currentVersion: BazelVersionLiteral?): BazelVersionLiteral? {
    val githubService = project.service<BazelGithubGlobalCacheService>()
    githubService.optimizeCache()
    val fork = getForkOfSupportedBazelVersion(currentVersion) ?: return null
    val version =
      githubService.getCachedVersion(fork, System.currentTimeMillis()) { name, _ ->
        getLatestBazelVersion(project, name)
      } ?: return null
    return currentVersion?.withNewVersionWhenPossible(version)
  }

  suspend fun getLatestBazelVersion(project: Project, forkName: String): String? {
    val response =
      try {
        HttpRequests
          .request("https://api.github.com/repos/$forkName/$DEFAULT_REPO_NAME/releases")
          .productNameAsUserAgent()
          .readString()
      } catch (_: IOException) {
        return null
      }
    val cacheService = project.service<BazelGithubGlobalCacheService>()

    val releases =
      try {
        githubGson.fromJson(response, Array<GithubReleaseResponse>::class.java)
      } catch (_: JsonSyntaxException) {
        return null
      }
    val comparator = compareBy<GithubReleaseResponse> { response -> response.tagName?.let { SemVer.parseFromText(it) } }
    return releases
      .toList()
      .filter {
        if (!cacheService.includePrelease && it.prerelease == true) {
          return@filter false
        }
        if (!cacheService.includeReleaseCandidate && it.tagName?.contains("-rc") == true) {
          return@filter false
        }
        true
      }.maxWithOrNull(comparator)
      ?.tagName
  }

  companion object {
    const val ID = "github"
    const val DEFAULT_FORK_NAME = "bazelbuild"
    const val DEFAULT_REPO_NAME = "bazel"
  }

  data class GithubReleaseResponse(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("tag_name") var tagName: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("prerelease") var prerelease: Boolean? = null,
  )
}

val GITHUB_CACHE_EVICTION_TIME = 24.hours

@Service(Service.Level.PROJECT)
@State(name = "BazelGithubResolverVersionCache", storages = [Storage(StoragePathMacros.CACHE_FILE, roamingType = RoamingType.DISABLED)])
class BazelGithubGlobalCacheService(val coroutineScope: CoroutineScope) : PersistentStateComponent<BazelGithubGlobalCacheService.State> {
  class State : BaseState() {
    @get:XMap
    val forks by map<String, ForkState>()
    var includePrelease by property(false)
    var includeReleaseCandidate by property(false)
  }

  @Tag("fork_state")
  data class ForkState(
    @JvmField var name: String = "",
    @JvmField var version: String = "",
    @JvmField var lastUpdated: Long = 0,
  )

  sealed interface CachedVersion {
    data class Cached(val version: String, val lastUpdated: Long) : CachedVersion

    data object Unavailable : CachedVersion
  }

  val cache: AsyncCache<String, CachedVersion> =
    Caffeine
      .newBuilder()
      .expireAfter(
        Expiry.writing<String, CachedVersion> { _, value ->
          return@writing when (value) {
            is CachedVersion.Cached -> {
              val currentTime = System.currentTimeMillis().milliseconds
              val whenExpire = value.lastUpdated.milliseconds + GITHUB_CACHE_EVICTION_TIME
              return@writing (whenExpire - currentTime).toJavaDuration()
            }

            CachedVersion.Unavailable -> {
              return@writing Duration.ZERO.toJavaDuration()
            }
          }
        },
      ).executor { coroutineScope.future { it.run() } }
      .buildAsync()

  var includePrelease = false
  var includeReleaseCandidate = false

  override fun getState(): BazelGithubGlobalCacheService.State =
    State().also {
      it.includePrelease = includePrelease
      it.includeReleaseCandidate = includeReleaseCandidate

      val newMap =
        cache
          .synchronous()
          .asMap()
          .mapNotNull { (key, value) ->
            when (value) {
              is CachedVersion.Cached -> {
                key to
                  ForkState().apply {
                    name = key
                    version = value.version
                    lastUpdated = value.lastUpdated
                  }
              }

              CachedVersion.Unavailable -> null
            }
          }.toMap()
      it.forks.putAll(newMap)
    }

  override fun loadState(state: BazelGithubGlobalCacheService.State) {
    includePrelease = state.includePrelease
    includeReleaseCandidate = state.includeReleaseCandidate

    cache.synchronous().invalidateAll()
    val newMap =
      state.forks.mapValues { (_, value) ->
        CachedVersion.Cached(value.version, value.lastUpdated)
      }
    cache.synchronous().putAll(newMap)
  }

  suspend fun getCachedVersion(
    forkName: String,
    currentTimeMs: Long,
    supplier: suspend (forkName: String, lastUpdated: Long) -> String?,
  ): String? {
    val cachedVersion =
      cache
        .get(forkName) { forkName, _ ->
          coroutineScope.future {
            val version =
              supplier(forkName, currentTimeMs)
                ?: return@future CachedVersion.Unavailable
            return@future CachedVersion.Cached(version, currentTimeMs)
          }
        }.asDeferred()
        .await()
    return when (cachedVersion) {
      is CachedVersion.Cached -> cachedVersion.version
      CachedVersion.Unavailable -> null
    }
  }

  fun optimizeCache() {
    cache.synchronous().cleanUp()
  }
}
