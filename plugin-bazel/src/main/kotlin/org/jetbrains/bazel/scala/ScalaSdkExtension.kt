package org.jetbrains.bazel.scala.sdk

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import org.jetbrains.bazel.annotations.PublicApi

/**
 * this API will be sunset for the [org.jetbrains.bazel.workspacemodel.entities.ScalaAddendumEntity] later,
 * once Scala plugin is compatible with WSM
 */
@PublicApi
interface ScalaSdkExtension {
  fun addScalaSdk(scalaSdk: ScalaSdk, modelsProvider: IdeModifiableModelsProvider)
}

private val ep = ExtensionPointName.create<ScalaSdkExtension>("org.jetbrains.bazel.scalaSdkExtension")

fun scalaSdkExtension(): ScalaSdkExtension? = ep.extensionList.firstOrNull()

fun scalaSdkExtensionExists(): Boolean = ep.extensionList.isNotEmpty()
