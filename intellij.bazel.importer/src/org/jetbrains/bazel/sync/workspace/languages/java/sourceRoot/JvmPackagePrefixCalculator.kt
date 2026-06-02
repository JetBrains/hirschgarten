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
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.JavaSourceRootPatterns
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix.SourcePatternEval
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
sealed interface SourceRootOptimizationMode {
  /**
   * Disable source root optimization for all source files.
   */
  data object Disabled : SourceRootOptimizationMode

  /**
   * Enable source root optimization for files according to [patterns].
   * Optimization algorithm assumes that the source root layout follows a multi-root maven directory structure.
   */
  data class MavenLayout(val patterns: JavaSourceRootPatterns) : SourceRootOptimizationMode

  companion object {
    fun createFromProject(project: Project): SourceRootOptimizationMode = if (project.projectView().javaSROEnable) {
      MavenLayout(
        patterns = JavaSourceRootPatternContributor.allSourceRootPatterns(project),
      )
    }
    else {
      Disabled
    }
  }
}

@ApiStatus.Internal
class DefaultJvmPackagePrefixCalculator(
  val sourceRootOptimizationMode: SourceRootOptimizationMode,
) : JvmPackagePrefixCalculator {
  private val packageResolver = DefaultJvmPackageResolver()
  private val packageInference = JavaSourceRootPackageInference(packageResolver)

  private val cache = ConcurrentHashMap<Label, JvmPackagePrefixes>()

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
    val sources = target.sources.filter { it.extension != "srcjar" }

    val result = HashMap<Path, String>()
    when (sourceRootOptimizationMode) {
      SourceRootOptimizationMode.Disabled -> {
        for (src in sources) {
          val prefix = packageResolver.calculateJvmPackagePrefix(src)
          if (prefix != null)
            result[src] = prefix
        }
      }

      is SourceRootOptimizationMode.MavenLayout -> {
        val patterns = sourceRootOptimizationMode.patterns
        val (matched, unmatched) = SourcePatternEval.eval(
          items = sources,
          includes = patterns.includes.map { { src -> it.matches(src) } },
          excludes = patterns.excludes.map { { src -> it.matches(src) } },
        )
        if (matched.isNotEmpty()) {
          matched
            .groupBy { it.extension }
            .forEach { result.putAll(packageInference.inferPackages(it.value)) }
        }

        for (src in unmatched) {
          val prefix = packageResolver.calculateJvmPackagePrefix(src)
          if (prefix != null)
            result[src] = prefix
        }
      }
    }
    return result
  }
}
