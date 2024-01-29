package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.android.AndroidProjectTypes
import com.intellij.facet.FacetType
import com.intellij.facet.impl.FacetUtil
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.workspace.jps.entities.FacetEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.AndroidFacetConfiguration
import org.jetbrains.bsp.AndroidTargetType
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule

internal interface AndroidFacetEntityUpdaterExtension {
  fun createAndroidFacetEntityUpdater(
    workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  ): WorkspaceModelEntityWithParentModuleUpdater<JavaModule, FacetEntity>
}

private val ep =
  ExtensionPointName.create<AndroidFacetEntityUpdaterExtension>(
    "org.jetbrains.bsp.androidFacetEntityUpdaterExtension",
  )

internal fun androidFacetEntityUpdaterExtension(): AndroidFacetEntityUpdaterExtension? =
  ep.extensionList.firstOrNull()

@Suppress("UnusedPrivateClass")
private class AndroidFacetEntityUpdaterExtensionImpl : AndroidFacetEntityUpdaterExtension {
  override fun createAndroidFacetEntityUpdater(
    workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  ): WorkspaceModelEntityWithParentModuleUpdater<JavaModule, FacetEntity> =
    AndroidFacetEntityUpdater(workspaceModelEntityUpdaterConfig)
}

private class AndroidFacetEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<JavaModule, FacetEntity> {
  override fun addEntity(entityToAdd: JavaModule, parentModuleEntity: ModuleEntity): FacetEntity {
    val facetType = AndroidFacet.getFacetType()
    val facet = facetType.createDefaultConfiguration()
    val facetState = facet.state

    facetState.PROJECT_TYPE = getAndroidProjectType(entityToAdd.androidAddendum?.androidTargetType)
    facetState.MANIFEST_FILE_RELATIVE_PATH = ""
    facetState.RES_FOLDER_RELATIVE_PATH = ""
    facetState.ASSETS_FOLDER_RELATIVE_PATH = ""

    return addFacetEntity(facet, parentModuleEntity, facetType)
  }

  private fun getAndroidProjectType(androidTargetType: AndroidTargetType?): Int = when (androidTargetType) {
    AndroidTargetType.APP -> AndroidProjectTypes.PROJECT_TYPE_APP
    AndroidTargetType.LIBRARY -> AndroidProjectTypes.PROJECT_TYPE_LIBRARY
    AndroidTargetType.TEST -> AndroidProjectTypes.PROJECT_TYPE_TEST
    null -> AndroidProjectTypes.PROJECT_TYPE_LIBRARY
  }

  private fun addFacetEntity(
    facet: AndroidFacetConfiguration,
    parentModuleEntity: ModuleEntity,
    facetType: FacetType<AndroidFacet, AndroidFacetConfiguration>,
  ): FacetEntity {
    val facetConfigurationXml = FacetUtil.saveFacetConfiguration(facet)?.let { JDOMUtil.write(it) }
    return workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(
      FacetEntity(
        name = "Android",
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
