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
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.KotlincOpts

internal class CommonCompilerArgsHolder : CommonCompilerArguments() {
  override fun copyOf(): Freezable =
    copyCommonCompilerArguments(this, CommonCompilerArgsHolder())

  fun configure(kotlincOpts: KotlincOpts) {
    if (kotlincOpts.xAllowResultReturnType != null) allowResultReturnType = kotlincOpts.xAllowResultReturnType
    if (kotlincOpts.xExplicitApiMode != null) explicitApi = kotlincOpts.xExplicitApiMode
    if (kotlincOpts.xInlineClasses != null) inlineClasses = kotlincOpts.xInlineClasses
    if (kotlincOpts.xMultiPlatform != null) multiPlatform = kotlincOpts.xMultiPlatform
    if (kotlincOpts.xOptinList != null) optIn = kotlincOpts.xOptinList.toTypedArray()
    if (kotlincOpts.xReportPerf != null) reportPerf = kotlincOpts.xReportPerf
    if (kotlincOpts.xSkipPrereleaseCheck != null) skipPrereleaseCheck = kotlincOpts.xSkipPrereleaseCheck
    if (kotlincOpts.xUseFirLt != null) useFirLT = kotlincOpts.xUseFirLt
    if (kotlincOpts.xUseK2 != null) useK2 = kotlincOpts.xUseK2
  }
}

public fun CommonCompilerArguments.toKotlinCOption(): KotlincOpts = KotlincOpts(
  xAllowResultReturnType = allowResultReturnType,
  xExplicitApiMode = explicitApi,
  xInlineClasses = inlineClasses,
  xMultiPlatform = multiPlatform,
  xOptinList = optIn?.toList().orEmpty(),
  xReportPerf = reportPerf,
  xSkipPrereleaseCheck = skipPrereleaseCheck,
  xUseFirLt = useFirLT,
  xUseK2 = useK2,
)

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
    if (!kotlincOpts.jvmTarget.isNullOrBlank())
      facet.settings.targetPlatform =
        JvmTarget.fromString(kotlincOpts.jvmTarget)?.let { JvmPlatforms.jvmPlatformByTargetVersion(it) }
    val commonCompilerArguments = CommonCompilerArgsHolder()
    commonCompilerArguments.configure(kotlincOpts)
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
