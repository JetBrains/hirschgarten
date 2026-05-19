package org.jetbrains.bazel.sync.workspace.languages.python

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.PythonTargetInfo
import org.jetbrains.bazel.label.label
import java.nio.file.Path
import kotlin.collections.plus
import kotlin.io.path.nameWithoutExtension

@ApiStatus.Internal
object MainSourceFinder {
  fun findMainFile(
    target: TargetIdeInfo,
    pythonTarget: PythonTargetInfo,
    pathsResolver: BazelPathsResolver,
    localRepositories: LocalRepositoryMapping,
  ): Path? {
    val mainFileDeclared = pythonTarget.main?.relativePath?.isNotEmpty() == true
    val artifactLocation =
      if (mainFileDeclared) {
        pythonTarget.main
      } else {
        findMainFileAmongSources(target)
      }
    return artifactLocation?.let { pathsResolver.resolve(it, localRepositories) }
  }

  private fun findMainFileAmongSources(target: TargetIdeInfo): ArtifactLocation? {
    val sources = target.srcsList
    if (sources.size == 1) {
      return sources.single()
    } else {
      val label = target.label()
      return sources.firstOrNull {
        val path = Path.of(it.relativePath)
        // when having multiple sources, target //aaa/bbb:ccc/ddd will choose aaa/bbb/ccc/ddd.py as its main file
        val allLabelSegments = label.packagePath.pathSegments + label.targetName.split('/')
        val allPathSegments = path.parent.segments() + listOf(path.nameWithoutExtension)
        return@firstOrNull allLabelSegments == allPathSegments
      }
    }
  }

  private fun Path?.segments(): List<String> {
    if (this == null || nameCount == 1 && toString().isEmpty()) return emptyList()
    return (0 until nameCount).map { getName(it).toString() }
  }
}
