package org.jetbrains.bazel.workspace.importer

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.KotlinBuildTarget

@ApiStatus.Internal
object KotlinFacetBuilder {
  fun write(
    kotlinBuildTarget: KotlinBuildTarget?,
    isTestModule: Boolean,
    associates: Set<String>,
    parentModuleEntity: ModuleEntity,
    storage: MutableEntityStorage,
  ) {
    val ep = KotlinFacetEntityUpdater.ep.extensionList.firstOrNull() ?: return
    val kotlinOptions = kotlinBuildTarget?.let {
      KotlinOptions(
        languageVersion = it.languageVersion,
        apiVersion = it.apiVersion,
        moduleName = it.moduleName,
        kotlincOptions = it.kotlincOptions,
      )
    }
    ep.addEntity(
      diff = storage,
      parentModuleEntity = parentModuleEntity,
      kotlinOptions = kotlinOptions,
      isTestModule = isTestModule,
      associates = associates,
    )
  }
}
