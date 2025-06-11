package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bsp.protocol.AndroidTargetType
import java.nio.file.Path

data class JavaSourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val packagePrefix: String,
  val rootType: SourceRootTypeId,
) : WorkspaceModelEntity()

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
  val androidAddendum: AndroidAddendum? = null,
  val workspaceModelEntitiesFolderMarker: Boolean = false,
) : WorkspaceModelEntity(),
  Module {
  override fun getModuleName(): String = genericModuleInfo.name
}

data class KotlinAddendum(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
)

data class ScalaAddendum(
  val scalaVersion: String,
  val scalacOptions: List<String>,
  val sdkJars: List<Path>,
) : WorkspaceModelEntity()

data class JavaAddendum(val languageVersion: String, val javacOptions: List<String>)

data class AndroidAddendum(
  val androidSdkName: String,
  val androidTargetType: AndroidTargetType,
  val manifest: Path?,
  val manifestOverrides: Map<String, String>,
  val resourceDirectories: List<Path>,
  val resourceJavaPackage: String?,
  val assetsDirectories: List<Path>,
  val apk: Path?,
) : WorkspaceModelEntity()
