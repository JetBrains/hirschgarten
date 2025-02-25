package org.jetbrains.bazel.magicmetamodel

import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.ScalacOptionsItem
import org.jetbrains.bsp.protocol.SourcesItem

typealias TargetNameReformatProvider = (BuildTargetInfo) -> String

object DefaultNameProvider : TargetNameReformatProvider {
  override fun invoke(targetInfo: BuildTargetInfo): String = targetInfo.id.uri
}

data class ProjectDetails(
  val targetIds: List<BuildTargetIdentifier>,
  val targets: Set<BuildTarget>,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val libraries: List<LibraryItem>?,
  val nonModuleTargets: List<BuildTarget>,
  var defaultJdkName: String? = null,
  var jvmBinaryJars: List<JvmBinaryJarsItem> = emptyList(),
)
