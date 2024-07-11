package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.android.AndroidProjectTypes
import com.intellij.facet.impl.FacetUtil
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.workspace.jps.entities.FacetEntity
import com.intellij.platform.workspace.jps.entities.FacetEntityTypeId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.facet.AndroidFacetConfiguration
import org.jetbrains.android.facet.AndroidFacetType
import org.jetbrains.bsp.protocol.AndroidTargetType
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaModule

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

    return addFacetEntity(facet, parentModuleEntity)
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
  ): FacetEntity {
    val facetConfigurationXml = FacetUtil.saveFacetConfiguration(facet)?.let { JDOMUtil.write(it) }
    val entity = FacetEntity(
      name = "Android",
      moduleId = parentModuleEntity.symbolicId,
      typeId = FacetEntityTypeId(AndroidFacetType.TYPE_ID),
      entitySource = parentModuleEntity.entitySource,
    ) {
      this.configurationXmlTag = facetConfigurationXml
    }

    val updatedParentModuleEntity =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.modifyModuleEntity(parentModuleEntity) {
        this.facets += entity
      }
    return updatedParentModuleEntity.facets.last()
  }
}
