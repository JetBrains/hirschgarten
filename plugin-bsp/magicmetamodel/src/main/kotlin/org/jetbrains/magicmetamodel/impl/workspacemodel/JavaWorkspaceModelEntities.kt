package org.jetbrains.magicmetamodel.impl.workspacemodel

import org.jetbrains.magicmetamodel.impl.ModuleState
import org.jetbrains.magicmetamodel.impl.toState
import java.nio.file.Path

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
  val compilerOutput: Path?,
  val jvmJdkName: String? = null,
  val kotlinAddendum: KotlinAddendum? = null,
) : WorkspaceModelEntity(), Module {
  override fun toState(): ModuleState = ModuleState(
    module = genericModuleInfo.toState(),
    baseDirContentRoot = baseDirContentRoot?.let(ContentRoot::toState),
    sourceRoots = sourceRoots.map { it.toState() },
    resourceRoots = resourceRoots.map { it.toString() },
    libraries = moduleLevelLibraries?.map { it.toState() },
    compilerOutput = compilerOutput?.toString(),
    jvmJdkName = jvmJdkName,
    kotlinAddendum = kotlinAddendum?.toState(),
  )

  override fun getModuleName(): String = genericModuleInfo.name
}

public data class KotlinAddendum(
  val languageVersion: String,
  val apiVersion: String,
  val kotlincOptions: List<String>,
)
