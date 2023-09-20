package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.facet.FacetType
import com.intellij.facet.impl.FacetUtil
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.workspace.jps.entities.FacetEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.Freezable
import org.jetbrains.kotlin.cli.common.arguments.copyCommonCompilerArguments
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule

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

internal class CommonCompilerArgsHolder : CommonCompilerArguments() {
  override fun copyOf(): Freezable =
    copyCommonCompilerArguments(this, CommonCompilerArgsHolder())

  fun configure(kotlincOptsMap: Map<String, String>) {
    with(RulesKotlinKotlincOpts) {
      kotlincOptsMap[X_ALLOW_RESULT_RETURN_TYPE]?.toBooleanStrictOrNull()?.also { allowResultReturnType = it }
      kotlincOptsMap[X_EXPLICIT_API_MODE]?.also { explicitApi = it }
      kotlincOptsMap[X_INLINE_CLASSES]?.toBooleanStrictOrNull()?.also { inlineClasses = it }
      kotlincOptsMap[X_MULTI_PLATFORM]?.toBooleanStrictOrNull()?.also { multiPlatform = it }
      kotlincOptsMap[X_OPTIN]?.toList()?.also { optIn = it.toTypedArray() }
      kotlincOptsMap[X_REPORT_PERF]?.toBooleanStrictOrNull()?.also { reportPerf = it }
      kotlincOptsMap[X_SKIP_PRERELEASE_CHECK]?.toBooleanStrictOrNull()?.also { skipPrereleaseCheck = it }
      kotlincOptsMap[X_USE_FIR_LT]?.toBooleanStrictOrNull()?.also { useFirLT = it }
      kotlincOptsMap[X_USE_K2]?.toBooleanStrictOrNull()?.also { useK2 = it }
    }
  }
}

public fun CommonCompilerArguments.toKotlincOptions(jdkPlatform: JdkPlatform?): List<String> =
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
      "$JVM_TARGET=${jdkPlatform?.targetVersion?.toString() ?: ""}"
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
) : WorkspaceModelEntityWithParentModuleUpdater<JavaModule, FacetEntity> {
  override fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): FacetEntity {
    val facetType = KotlinFacetType.INSTANCE
    val facet = facetType.createDefaultConfiguration()
    facet.settings.useProjectSettings = false
    facet.settings.additionalVisibleModuleNames =
      entityToAdd.genericModuleInfo.associates.map { it.moduleName }.toSet()
    val kotlinAddendum = entityToAdd.kotlinAddendum
    val kotlincOpts = kotlinAddendum?.kotlincOptions ?: return addFacetEntity(facet, parentModuleEntity, facetType)
    val kotlincOptsMap = kotlincOpts.toKotlincOptsDict()
    val jvmTarget = kotlincOptsMap["jvm_target"]
    if (!jvmTarget.isNullOrBlank())
      facet.settings.targetPlatform =
        JvmTarget.fromString(jvmTarget)?.let { JvmPlatforms.jvmPlatformByTargetVersion(it) }
    val commonCompilerArguments = CommonCompilerArgsHolder()
    commonCompilerArguments.configure(kotlincOptsMap)
    facet.settings.compilerArguments = commonCompilerArguments
    facet.settings.languageLevel = LanguageVersion.fromVersionString(kotlinAddendum.languageVersion)
    facet.settings.apiLevel = LanguageVersion.fromVersionString(kotlinAddendum.apiVersion)
    return addFacetEntity(facet, parentModuleEntity, facetType)
  }

  private fun addFacetEntity(
    facet: KotlinFacetConfiguration,
    parentModuleEntity: ModuleEntity,
    facetType: FacetType<KotlinFacet, KotlinFacetConfiguration>,
  ): FacetEntity {
    val facetConfigurationXml = FacetUtil.saveFacetConfiguration(facet)?.let { JDOMUtil.write(it) }
    return workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
      FacetEntity(
        name = "Kotlin",
        moduleId = parentModuleEntity.symbolicId,
        facetType = facetType.id.toString(),
        entitySource = parentModuleEntity.entitySource,
      ) {
        this.configurationXmlTag = facetConfigurationXml
        this.module = parentModuleEntity
      },
    )
  }
}
