package org.jetbrains.bazel.scala.sdk

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import org.jetbrains.annotations.ApiStatus

/**
 * this API will be sunset for the [org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity] later,
 * once Scala plugin is compatible with WSM
 */
@ApiStatus.Internal
interface ScalaSdkExtension {
  fun addScalaSdk(scalaSdk: ScalaSdk, modelsProvider: IdeModifiableModelsProvider)
}

private val ep = ExtensionPointName.create<ScalaSdkExtension>("org.jetbrains.bazel.scalaSdkExtension")

@ApiStatus.Internal
fun scalaSdkExtension(): ScalaSdkExtension? = ep.extensionList.firstOrNull()

@ApiStatus.Internal
fun scalaSdkExtensionExists(): Boolean = ep.extensionList.isNotEmpty()
