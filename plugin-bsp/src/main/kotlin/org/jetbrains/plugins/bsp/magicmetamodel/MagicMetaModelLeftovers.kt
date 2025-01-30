package org.jetbrains.plugins.bsp.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import ch.epfl.scala.bsp4j.SourcesItem
import org.jetbrains.bsp.protocol.DependenciesExportedItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

typealias TargetNameReformatProvider = (BuildTargetInfo) -> String

object DefaultNameProvider : TargetNameReformatProvider {
  override fun invoke(targetInfo: BuildTargetInfo): String = targetInfo.id.uri
}

data class ProjectDetails(
  val targetIds: List<BuildTargetIdentifier>,
  val targets: Set<BuildTarget>,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesExported: List<DependenciesExportedItem>,
  val dependenciesSources: List<DependencySourcesItem>,
  val javacOptions: List<JavacOptionsItem>,
  val scalacOptions: List<ScalacOptionsItem>,
  val libraries: List<LibraryItem>?,
  val nonModuleTargets: List<BuildTarget>,
  var defaultJdkName: String? = null,
  var jvmBinaryJars: List<JvmBinaryJarsItem> = emptyList(),
)
