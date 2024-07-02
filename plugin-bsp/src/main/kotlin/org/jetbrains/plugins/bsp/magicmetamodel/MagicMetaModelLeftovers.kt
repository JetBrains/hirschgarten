package org.jetbrains.plugins.bsp.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import java.nio.file.Path

public data class MagicMetaModelProjectConfig(
  val workspaceModel: WorkspaceModel,
  val virtualFileUrlManager: VirtualFileUrlManager,
  val projectBasePath: Path,
  val project: Project,
  val moduleNameProvider: TargetNameReformatProvider,
  val libraryNameProvider: TargetNameReformatProvider,
  val isPythonSupportEnabled: Boolean,
  val hasDefaultPythonInterpreter: Boolean,
  val isAndroidSupportEnabled: Boolean,
) {
  public constructor(
    workspaceModel: WorkspaceModel,
    virtualFileUrlManager: VirtualFileUrlManager,
    moduleNameProvider: TargetNameReformatProvider?,
    libraryNameProvider: TargetNameReformatProvider?,
    projectBasePath: Path,
    project: Project,
    isPythonSupportEnabled: Boolean = false,
    hasDefaultPythonInterpreter: Boolean = false,
    isAndroidSupportEnabled: Boolean = false,
  ) : this(
    workspaceModel,
    virtualFileUrlManager,
    projectBasePath,
    project,
    moduleNameProvider ?: DefaultModuleNameProvider,
    libraryNameProvider ?: DefaultLibraryNameProvider,
    isPythonSupportEnabled,
    hasDefaultPythonInterpreter,
    isAndroidSupportEnabled
  )
}

public typealias TargetNameReformatProvider = (BuildTargetInfo) -> String

public object DefaultModuleNameProvider : TargetNameReformatProvider {
  override fun invoke(targetInfo: BuildTargetInfo): String = targetInfo.id
}

public object DefaultLibraryNameProvider : TargetNameReformatProvider {
  override fun invoke(targetInfo: BuildTargetInfo): String = targetInfo.id
}

public data class ProjectDetails(
  val targetIds: List<BuildTargetIdentifier>,
  val targets: Set<BuildTarget>,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
  val javacOptions: List<JavacOptionsItem>,
  val scalacOptions: List<ScalacOptionsItem>,
  val pythonOptions: List<PythonOptionsItem>,
  val outputPathUris: List<String>,
  val libraries: List<LibraryItem>?,
  val directories: WorkspaceDirectoriesResult = WorkspaceDirectoriesResult(emptyList(), emptyList()),
  var defaultJdkName: String? = null,
  var jvmBinaryJars: List<JvmBinaryJarsItem> = emptyList(),
)
