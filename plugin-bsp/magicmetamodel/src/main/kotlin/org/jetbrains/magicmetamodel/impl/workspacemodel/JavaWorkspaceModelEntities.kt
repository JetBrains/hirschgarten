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
  val kotlincOptions: KotlincOpts?,
)

public data class KotlincOpts(
  val includeStdlibs: String? = null,
  val javaParameters: Boolean? = null,
  val jvmTarget: String? = null,
  val warn: String? = null,
  val xAllowResultReturnType: Boolean? = null,
  val xBackendThreads: Int? = null,
  val xEmitJvmTypeAnnotations: Boolean? = null,
  val xEnableIncrementalCompilation: Boolean? = null,
  val xExplicitApiMode: String? = null,
  val xInlineClasses: Boolean? = null,
  val xJvmDefault: String? = null,
  val xLambdas: String? = null,
  val xMultiPlatform: Boolean? = null,
  val xNoCallAssertions: Boolean? = null,
  val xNoOptimize: Boolean? = null,
  val xNoOptimizedCallableReferences: Boolean? = null,
  val xNoParamAssertions: Boolean? = null,
  val xNoReceiverAssertions: Boolean? = null,
  val xOptinList: List<String>? = null,
  val xReportPerf: Boolean? = null,
  val xSamConversions: String? = null,
  val xSkipPrereleaseCheck: Boolean? = null,
  val xUseFirLt: Boolean? = null,
  val xUseK2: Boolean? = null,
)
