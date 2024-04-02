package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel

import org.jetbrains.bsp.protocol.AndroidTargetType
import org.jetbrains.plugins.bsp.magicmetamodel.impl.ModuleState
import org.jetbrains.plugins.bsp.magicmetamodel.impl.toState
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import java.nio.file.Path
import com.intellij.openapi.module.Module as IdeaModule

public data class JavaSourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val packagePrefix: String,
  val rootType: String,
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
) : WorkspaceModelEntity(), Module {
  override fun toState(): ModuleState = ModuleState(
    module = genericModuleInfo.toState(),
    baseDirContentRoot = baseDirContentRoot?.let(ContentRoot::toState),
    sourceRoots = sourceRoots.map { it.toState() },
    resourceRoots = resourceRoots.map { it.toState() },
    libraries = moduleLevelLibraries?.map { it.toState() },
    jvmJdkName = jvmJdkName,
    jvmBinaryJars = jvmBinaryJars.map { it.toString() },
    kotlinAddendum = kotlinAddendum?.toState(),
    scalaAddendum = scalaAddendum?.toState(),
    androidAddendum = androidAddendum?.toState(),
  )

  override fun getModuleName(): String = genericModuleInfo.name
}

public data class KotlinAddendum(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
)

public data class ScalaAddendum(
  val scalaSdkName: String,
)

public data class JavaAddendum(
  val languageVersion: String,
)

public data class AndroidAddendum(
  val androidSdkName: String,
  val androidTargetType: AndroidTargetType,
  val manifest: Path?,
  val resourceFolders: List<Path>,
) : WorkspaceModelEntity()

public val IdeaModule.javaModule: JavaModule?
  get() {
    val magicMetaModel = MagicMetaModelService.getInstance(this.project).value
    val module = magicMetaModel.getDetailsForTargetId(this.name)
    if (module !is JavaModule) return null
    return module
  }

public val IdeaModule.androidAddendum: AndroidAddendum?
  get() = javaModule?.androidAddendum
