package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.facet.impl.FacetUtil
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.workspace.jps.entities.FacetEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.Freezable
import org.jetbrains.kotlin.cli.common.arguments.copyCommonCompilerArguments
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.KotlincOpts

internal class CommonCompilerArgsHolder: CommonCompilerArguments() {

  override fun copyOf(): Freezable =
    copyCommonCompilerArguments(this, CommonCompilerArgsHolder())

  fun configure(kotlincOpts: KotlincOpts) {
    allowResultReturnType = kotlincOpts.xAllowResultReturnType
    explicitApi = kotlincOpts.xExplicitApiMode
    inlineClasses = kotlincOpts.xInlineClasses
    multiPlatform = kotlincOpts.xMultiPlatform
    optIn = kotlincOpts.xOptinList.toTypedArray()
    reportPerf = kotlincOpts.xReportPerf
    skipPrereleaseCheck = kotlincOpts.xSkipPrereleaseCheck
    useFirLT = kotlincOpts.xUseFirLt
    useK2 = kotlincOpts.xUseK2
  }
}

internal class KotlinFacetEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig
) : WorkspaceModelEntityWithParentModuleUpdater<JavaModule, FacetEntity> {

  override fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): FacetEntity {
    val facetType = KotlinFacetType.INSTANCE
    val facet = facetType.createDefaultConfiguration()
    facet.settings.useProjectSettings = false
    facet.settings.additionalVisibleModuleNames =
      entityToAdd.module.associates.map { it.moduleName }.toSet()
    entityToAdd.kotlinAddendum?.let { kotlinAddendum ->
      kotlinAddendum.kotlincOptions?.let { kotlincOpts ->
        val commonCompilerArguments = CommonCompilerArgsHolder()
        commonCompilerArguments.configure(kotlincOpts)
      }
      facet.settings.languageLevel = LanguageVersion.fromVersionString(kotlinAddendum.languageVersion)
      facet.settings.apiLevel = LanguageVersion.fromVersionString(kotlinAddendum.apiVersion)
    }
    val facetConfigurationXml = FacetUtil.saveFacetConfiguration(facet)?.let { JDOMUtil.write(it) }
    return workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
      FacetEntity(
        name = "Kotlin",
        moduleId = parentModuleEntity.symbolicId,
        facetType = facetType.id.toString(),
        entitySource = parentModuleEntity.entitySource
      ) {
        this.configurationXmlTag = facetConfigurationXml
        this.module = parentModuleEntity
      }
    )
  }
}
