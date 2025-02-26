package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bsp.protocol.AndroidTargetType
import java.nio.file.Path

public data class JavaSourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val packagePrefix: String,
  val rootType: SourceRootTypeId,
  val excludedPaths: List<Path> = ArrayList(),
) : WorkspaceModelEntity()

public data class JavaModule(
  val genericModuleInfo: GenericModuleInfo,
  val baseDirContentRoot: ContentRoot?,
  val sourceRoots: List<JavaSourceRoot>,
  val resourceRoots: List<ResourceRoot>,
  // we will calculate this value only if there are no libraries in MagicMetamodelImpl.libraries,
  // otherwise it will be null
  val moduleLevelLibraries: List<Library>?,
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

public data class KotlinAddendum(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
)

public data class ScalaAddendum(val scalaSdkName: String)

public data class JavaAddendum(val languageVersion: String, val javacOptions: List<String>)

public data class AndroidAddendum(
  val androidSdkName: String,
  val androidTargetType: AndroidTargetType,
  val manifest: Path?,
  val manifestOverrides: Map<String, String>,
  val resourceDirectories: List<Path>,
  val resourceJavaPackage: String?,
  val assetsDirectories: List<Path>,
  val apk: Path?,
) : WorkspaceModelEntity()
