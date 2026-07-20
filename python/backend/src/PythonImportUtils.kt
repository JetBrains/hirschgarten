package com.intellij.bazel.python.backend

import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.toPath
import org.jetbrains.bazel.python.lang.extractPythonBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.file.Path
import kotlin.io.path.Path

internal object PythonImportUtils {
  /**
   * Converts the [org.jetbrains.bazel.python.lang.PythonBuildTarget.imports] attribute to workspace-root-relative paths
   */
  fun assembleExplicitImportsPaths(buildTarget: BuildTarget): List<Path> {
    val label = buildTarget.id as? ResolvedLabel ?: return listOf()
    val ideInfo = extractPythonBuildTarget(buildTarget) ?: return listOf()
    if (ideInfo.imports.isEmpty()) return listOf()

    val buildParentPath = label
                            .packagePath
                            .toPath()
                            .takeUnless { it.nameCount == 0 }
                          ?: Path(".")   // In the case of an external repo the build path could be `/BUILD.bazel`
    // which has a basedir of `/`. In this case we translate this to `.` so
    // that it works in the sub file-system.
    return ideInfo.imports.map { buildParentPath.resolve(it).normalize() }
  }

  /**
   * **DO NOT** use it for workspace model content root calculation -
   * Targets without explicit `imports` defined will be based in the project root,
   * which could result in multiple modules containing whole project.
   *
   * For content root calculations use `assembleExplicitImportsPaths`
   */
  fun assembleQualifiedNameImportPaths(explicitImportsPaths: List<Path>): List<ImportsPath> =
    explicitImportsPaths.ifEmpty { listOf(null) }.map { ImportsPath(it) }

  // not ImportPath - it refers to "imports" clause in Python targets
  data class ImportsPath(val importedPath: Path?) {
    fun relativizePath(rootPath: Path): Path? =
      when {
        importedPath == null -> rootPath
        rootPath.nameCount <= importedPath.nameCount || !rootPath.startsWith(importedPath) -> null // cannot assemble non-empty relative path
        else -> rootPath.subpath(importedPath.nameCount, rootPath.nameCount)
      }
  }
}
