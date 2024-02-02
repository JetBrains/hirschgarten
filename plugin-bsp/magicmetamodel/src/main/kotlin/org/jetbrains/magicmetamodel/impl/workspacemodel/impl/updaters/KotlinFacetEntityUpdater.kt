package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import org.jetbrains.bsp.jpsCompilation.utils.JpsPaths
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.config.KotlinModuleKind
import org.jetbrains.kotlin.config.serializeComponentPlatforms
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.workspaceModel.CompilerArgumentsSerializer
import org.jetbrains.kotlin.idea.workspaceModel.CompilerSettingsData
import org.jetbrains.kotlin.idea.workspaceModel.KotlinSettingsEntity
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.KotlinAddendum
import java.nio.file.Path

// TODO: Move this logic to `bazel-bsp`
// https://youtrack.jetbrains.com/issue/BAZEL-833
internal object RulesKotlinKotlincOpts {
  const val INCLUDE_STDLIBS = "include_stdlibs"
  const val JAVA_PARAMETERS = "java_parameters"
  const val JVM_TARGET = "jvm_target"
  const val WARN = "warn"
  const val X_ALLOW_RESULT_RETURN_TYPE = "x_allow_result_return_type"
  const val X_BACKEND_THREADS = "x_backend_threads"
  const val X_EMIT_JVM_TYPE_ANNOTATIONS = "x_emit_jvm_type_annotations"
  const val X_ENABLE_INCREMENTAL_COMPILATION = "x_enable_incremental_compilation"
  const val X_EXPLICIT_API_MODE = "x_explicit_api_mode"
  const val X_INLINE_CLASSES = "x_inline_classes"
  const val X_JVM_DEFAULT = "x_jvm_default"
  const val X_LAMBDAS = "x_lambdas"
  const val X_MULTI_PLATFORM = "x_multi_platform"
  const val X_NO_CALL_ASSERTIONS = "x_no_call_assertions"
  const val X_NO_OPTIMIZE = "x_no_optimize"
  const val X_NO_OPTIMIZED_CALLABLE_REFERENCES = "x_no_optimized_callable_references"
  const val X_NO_PARAM_ASSERTIONS = "x_no_param_assertions"
  const val X_NO_RECEIVER_ASSERTIONS = "x_no_receiver_assertions"
  const val X_OPTIN = "x_optin"
  const val X_REPORT_PERF = "x_report_perf"
  const val X_SAM_CONVERSIONS = "x_sam_conversions"
  const val X_SKIP_PRERELEASE_CHECK = "x_skip_prerelease_check"
  const val X_USE_FIR_LT = "x_use_fir_lt"
  const val X_USE_K2 = "x_use_k2"
}

private fun K2JVMCompilerArguments.configure(
  kotlincOptsMap: Map<String, String>,
  kotlinAddendum: KotlinAddendum,
): K2JVMCompilerArguments {
  with(RulesKotlinKotlincOpts) {
    // Common compiler arguments
    languageVersion = kotlinAddendum.languageVersion
    apiVersion = kotlinAddendum.apiVersion
    kotlincOptsMap[X_ALLOW_RESULT_RETURN_TYPE]?.toBooleanStrictOrNull()?.also { allowResultReturnType = it }
    kotlincOptsMap[X_EXPLICIT_API_MODE]?.also {
      explicitApi = when (it) {
        "off" -> "disable"
        else -> it
      }
    }
    kotlincOptsMap[X_INLINE_CLASSES]?.toBooleanStrictOrNull()?.also { inlineClasses = it }
    kotlincOptsMap[X_MULTI_PLATFORM]?.toBooleanStrictOrNull()?.also { multiPlatform = it }
    kotlincOptsMap[X_OPTIN]?.toList()?.also { optIn = it.toTypedArray() }
    kotlincOptsMap[X_REPORT_PERF]?.toBooleanStrictOrNull()?.also { reportPerf = it }
    kotlincOptsMap[X_SKIP_PRERELEASE_CHECK]?.toBooleanStrictOrNull()?.also { skipPrereleaseCheck = it }
    kotlincOptsMap[X_USE_FIR_LT]?.toBooleanStrictOrNull()?.also { useFirLT = it }
    kotlincOptsMap[X_USE_K2]?.toBooleanStrictOrNull()?.also { useK2 = it }

    // specific JVM arguments
    kotlincOptsMap[INCLUDE_STDLIBS]?.also {
      when (it) {
        "stdlib" -> noReflect = true
        "none" -> noStdlib = true
        else -> {}
      }
    }
    kotlincOptsMap[JAVA_PARAMETERS]?.toBooleanStrictOrNull()?.also { javaParameters = it }
    kotlincOptsMap[X_ALLOW_RESULT_RETURN_TYPE]?.toBooleanStrictOrNull()?.also { allowResultReturnType = it }
    kotlincOptsMap[X_BACKEND_THREADS]?.also { backendThreads = it }
    kotlincOptsMap[X_EMIT_JVM_TYPE_ANNOTATIONS]?.toBooleanStrictOrNull()?.also { emitJvmTypeAnnotations = it }
    kotlincOptsMap[X_JVM_DEFAULT]?.also {
      when (it) {
        "disable", "all-compatibility", "all" -> jvmDefault = it
        else -> {}
      }
    }
    kotlincOptsMap[X_LAMBDAS]?.also { lambdas = it }
    kotlincOptsMap[X_NO_CALL_ASSERTIONS]?.toBooleanStrictOrNull()?.also { noCallAssertions = it }
    kotlincOptsMap[X_NO_OPTIMIZE]?.toBooleanStrictOrNull()?.also { noOptimize = it }
    kotlincOptsMap[X_NO_OPTIMIZED_CALLABLE_REFERENCES]?.toBooleanStrictOrNull()
      ?.also { noOptimizedCallableReferences = it }
    kotlincOptsMap[X_NO_PARAM_ASSERTIONS]?.toBooleanStrictOrNull()?.also { noParamAssertions = it }
    kotlincOptsMap[X_NO_RECEIVER_ASSERTIONS]?.toBooleanStrictOrNull()?.also { noReceiverAssertions = it }
    kotlincOptsMap[X_SAM_CONVERSIONS]?.also { samConversions = it }
  }
  return this
}

public fun K2JVMCompilerArguments.toKotlincOptions(jdkPlatform: JdkPlatform?): List<String> =
  with(RulesKotlinKotlincOpts) {
    listOf(
      "$X_ALLOW_RESULT_RETURN_TYPE=$allowResultReturnType",
      "$X_EXPLICIT_API_MODE=$explicitApi",
      "$X_INLINE_CLASSES=$inlineClasses",
      "$X_MULTI_PLATFORM=$multiPlatform",
      "$X_OPTIN=${optIn.orEmpty().toList().toArgsString()}",
      "$X_REPORT_PERF=$reportPerf",
      "$X_SKIP_PRERELEASE_CHECK=$skipPrereleaseCheck",
      "$X_USE_FIR_LT=$useFirLT",
      "$X_USE_K2=$useK2",
      "$JVM_TARGET=${jdkPlatform?.targetVersion?.toString() ?: ""}",
      "$INCLUDE_STDLIBS=${
        when {
          noReflect -> "stdlib"
          noStdlib -> "none"
          else -> "all"
        }
      }",
      "$JAVA_PARAMETERS=$javaParameters",
      "$X_ALLOW_RESULT_RETURN_TYPE=$allowResultReturnType",
      "$X_BACKEND_THREADS=$backendThreads",
      "$X_EMIT_JVM_TYPE_ANNOTATIONS=$emitJvmTypeAnnotations",
      "$X_JVM_DEFAULT=$jvmDefault",
      "$X_LAMBDAS=$lambdas",
      "$X_NO_CALL_ASSERTIONS=$noCallAssertions",
      "$X_NO_OPTIMIZE=$noOptimize",
      "$X_NO_OPTIMIZED_CALLABLE_REFERENCES=$noOptimizedCallableReferences",
      "$X_NO_PARAM_ASSERTIONS=$noParamAssertions",
      "$X_NO_RECEIVER_ASSERTIONS=$noReceiverAssertions",
      "$X_SAM_CONVERSIONS=$samConversions"
    )
  }

private fun List<String>.toKotlincOptsDict(): Map<String, String> =
  this.associate {
    val parts = it.split("=")
    parts[0] to parts[1]
  }

private fun String.toList(): List<String> =
  this.split(",")

private fun List<String>.toArgsString(): String =
  this.joinToString(",")

internal class KotlinFacetEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val projectBasePath: Path,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaModule, KotlinSettingsEntity> {
  override fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): KotlinSettingsEntity {
    val kotlinAddendum = entityToAdd.kotlinAddendum
    val kotlincOpts = kotlinAddendum?.kotlincOptions?.toKotlincOptsDict()
    val kotlinSettingsEntity =
      calculateKotlinSettingsEntity(entityToAdd, kotlinAddendum, kotlincOpts, parentModuleEntity)
    return addKotlinSettingsEntity(kotlinSettingsEntity)
  }

  private fun calculateKotlinSettingsEntity(
    entityToAdd: JavaModule,
    kotlinAddendum: KotlinAddendum?,
    kotlincOpts: Map<String, String>?,
    parentModuleEntity: ModuleEntity,
  ) = KotlinSettingsEntity(
    name = KotlinFacetType.NAME,
    moduleId = parentModuleEntity.symbolicId,
    sourceRoots = emptyList(),
    configFileItems = emptyList(),
    useProjectSettings = false,
    implementedModuleNames = emptyList(),
    dependsOnModuleNames = emptyList(), // Gradle specific
    additionalVisibleModuleNames = entityToAdd.toAssociateModules().toMutableSet(),
    productionOutputPath = "",
    testOutputPath = "",
    sourceSetNames = emptyList(),
    isTestModule = entityToAdd.genericModuleInfo.capabilities.canTest,
    externalProjectId = "",
    isHmppEnabled = false,
    pureKotlinSourceFolders = emptyList(),
    kind = KotlinModuleKind.DEFAULT,
    compilerArguments = if (kotlincOpts != null && kotlinAddendum != null)
      CompilerArgumentsSerializer.serializeToString(
        K2JVMCompilerArguments().configure(kotlincOpts, kotlinAddendum)
      ) else "",
    compilerSettings = CompilerSettingsData(
      additionalArguments = entityToAdd.toFriendPaths(projectBasePath),
      scriptTemplates = "",
      scriptTemplatesClasspath = "",
      copyJsLibraryFiles = false,
      outputDirectoryForJsLibraryFiles = "",
      isInitialized = true
    ),
    targetPlatform = kotlincOpts?.get("jvm_target")?.let { jvmTargetString ->
      JvmTarget.fromString(jvmTargetString)?.let { jvmTarget ->
        JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget).serializeComponentPlatforms()
      }
    } ?: "",
    entitySource = parentModuleEntity.entitySource,
    externalSystemRunTasks = emptyList(),
    version = KotlinFacetSettings.CURRENT_VERSION,
    flushNeeded = true
  ) {
    module = parentModuleEntity
  }

  private fun JavaModule.toFriendPaths(projectBasePath: Path): String {
    val associateModules = toAssociateModules()

    if (associateModules.isEmpty()) return ""

    val friendPaths = associateModules.map { module ->
      JpsPaths.getJpsCompiledProductionDirectory(projectBasePath, module)
    }

    return "-Xfriend-paths=${friendPaths.joinToString(",")}"
  }

  private fun JavaModule.toAssociateModules(): Set<String> =
    this.genericModuleInfo.associates.map { it.moduleName }.toSet()

  private fun addKotlinSettingsEntity(
    kotlinSettingsEntity: KotlinSettingsEntity,
  ): KotlinSettingsEntity {
    return workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
      kotlinSettingsEntity
    )
  }
}
