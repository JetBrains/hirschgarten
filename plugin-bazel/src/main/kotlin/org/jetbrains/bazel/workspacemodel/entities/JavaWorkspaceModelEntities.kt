package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
data class JavaSourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val packagePrefix: String,
  val rootType: SourceRootTypeId,
) : WorkspaceModelEntity()

@ApiStatus.Internal
data class JavaModule(
  val genericModuleInfo: GenericModuleInfo,
  // TODO: rename to buildFileDirectory
  val baseDirContentRoot: ContentRoot?,
  val sourceRoots: List<JavaSourceRoot>,
  val resourceRoots: List<ResourceRoot>,
  val jvmJdkName: String? = null,
  val jvmBinaryJars: List<Path> = emptyList(),
  val kotlinAddendum: KotlinAddendum? = null,
  val scalaAddendum: ScalaAddendum? = null,
  val javaAddendum: JavaAddendum? = null,
  val workspaceModelEntitiesFolderMarker: Boolean = false,
) : WorkspaceModelEntity(),
  Module {
  override fun getModuleName(): String = genericModuleInfo.name
}

@ApiStatus.Internal
data class KotlinAddendum(
  val languageVersion: String?,
  val apiVersion: String?,
  val moduleName: String?,
  val kotlincOptions: List<String>,
)

@ApiStatus.Internal
data class ScalaAddendum(
  val scalaVersion: String,
  val scalacOptions: List<String>,
  val sdkJars: List<Path>,
) : WorkspaceModelEntity()

@ApiStatus.Internal
data class JavaAddendum(val languageVersion: String, val javacOptions: List<String>)
