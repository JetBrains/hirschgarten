package com.intellij.bazel.python.backend

import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.toPath
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Converts the [org.jetbrains.bsp.protocol.PythonBuildTarget.imports] attribute to workspace-root-relative paths.
 */
internal fun BuildTarget.assembleImportsPaths(): List<Path> {
  val label = id as? ResolvedLabel ?: return listOf()
  val ideInfo = extractPythonBuildTarget(this) ?: return listOf()
  val buildParentPath = label
    .packagePath
    .toPath()
    .takeUnless { it.nameCount == 0 }
    ?: Path(".")   // In the case of an external repo the build path could be `/BUILD.bazel`
                   // which has a basedir of `/`. In this case we translate this to `.` so
                   // that it works in the sub file-system.
  return ideInfo.imports.map { buildParentPath.resolve(it).normalize() }
}
