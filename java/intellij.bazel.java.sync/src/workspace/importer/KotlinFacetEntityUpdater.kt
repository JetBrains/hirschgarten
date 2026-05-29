package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class KotlinOptions(
  val languageVersion: String?,
  val apiVersion: String?,
  val moduleName: String?,
  val kotlincOptions: List<String>,
)

// TODO: should be removed, kotlin needs a separate sync hook: https://youtrack.jetbrains.com/issue/BAZEL-1885
// RC: EP interface moved out of `JavaModuleUpdater` and reshaped to take `KotlinOptions` + `ModuleEntity`
// instead of the old `JavaModule` wrapper
@ApiStatus.Internal
interface KotlinFacetEntityUpdater {
  fun addEntity(
    diff: MutableEntityStorage,
    parentModuleEntity: ModuleEntity,
    kotlinOptions: KotlinOptions?,
    isTestModule: Boolean,
    associates: Set<String>,
  )

  companion object {
    val ep: ExtensionPointName<KotlinFacetEntityUpdater> =
      ExtensionPointName.create("org.jetbrains.bazel.kotlinFacetEntityUpdater")
  }
}
