package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot

import com.intellij.openapi.project.Project
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.JavaSourceRootPatternContributor
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.SourcePatternEval
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.SourceRootPattern
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview.javaSROEnable
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension

@ApiStatus.Internal
class JvmPackagePrefixes(private val prefixes: Map<Path, String>) {
  operator fun get(path: Path): String? = prefixes[path]
}

@ApiStatus.Internal
interface JvmPackagePrefixCalculator {
  fun get(target: RawBuildTarget): JvmPackagePrefixes
}

@ApiStatus.Internal
class DefaultJvmPackagePrefixCalculator(project: Project): JvmPackagePrefixCalculator {
  private val packageResolver = DefaultJvmPackageResolver()
  private val packageInference = JavaSourceRootPackageInference(packageResolver)

  private val cachedJavaSROEnable = project.projectView().javaSROEnable
  private val cachedSROIncludeMatchers: List<SourceRootPattern>
  private val cachedSROExcludeMatchers: List<SourceRootPattern>

  private val cache = ConcurrentHashMap<Label, JvmPackagePrefixes>()

  init {
    val patterns = JavaSourceRootPatternContributor.ep
      .extensionList
      .map { it.getPatterns(project) }
    cachedSROIncludeMatchers = patterns.flatMap { it.includes }
    cachedSROExcludeMatchers = patterns.flatMap { it.excludes }
  }


  suspend fun calculate(targets: Collection<RawBuildTarget>) {
    coroutineScope {
      targets
        .filter { target ->
          target.data.any { it is JvmBuildTarget }
        }.map { target ->
          async {
            val prefixes = JvmPackagePrefixes(calculateForTarget(target))
            cache[target.id] = prefixes
          }
        }.awaitAll()
    }
  }

  override fun get(target: RawBuildTarget): JvmPackagePrefixes {
    return cache[target.id] ?: JvmPackagePrefixes(emptyMap())
  }

  private fun calculateForTarget(target: RawBuildTarget): Map<Path, String> {
    val sources = target.sources.filter { !it.generated && it.path.extension != "srcjar" }

    val result = HashMap<Path, String>()
    if (cachedJavaSROEnable) {
      val (matched, unmatched) = SourcePatternEval.eval(
        items = sources,
        includes = cachedSROIncludeMatchers.map { { src -> it.matches(src.path) } },
        excludes = cachedSROExcludeMatchers.map { { src -> it.matches(src.path) } },
      )
      if (matched.isNotEmpty()) {
        matched
          .groupBy { it.path.extension }
          .forEach { result.putAll(packageInference.inferPackages(it.value.map { it.path })) }
      }

      for (src in unmatched) {
        val prefix = packageResolver.calculateJvmPackagePrefix(src.path)
        if (prefix != null)
          result[src.path] = prefix
      }
    }
    else {
      for (src in sources) {
        val prefix = packageResolver.calculateJvmPackagePrefix(src.path)
        if (prefix != null)
          result[src.path] = prefix
      }
    }

    return result
  }
}
