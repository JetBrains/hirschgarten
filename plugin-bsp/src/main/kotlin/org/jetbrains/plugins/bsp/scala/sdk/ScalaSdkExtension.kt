package org.jetbrains.plugins.bsp.scala.sdk

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider

public interface ScalaSdkExtension {
  public fun addScalaSdk(
    scalaSdk: ScalaSdk,
    modelsProvider: IdeModifiableModelsProvider,
  )
}

private val ep =
  ExtensionPointName.create<ScalaSdkExtension>(
    "org.jetbrains.bsp.scalaSdkExtension",
  )

internal fun scalaSdkExtension(): ScalaSdkExtension? =
  ep.extensionList.firstOrNull()

internal fun scalaSdkExtensionExists(): Boolean =
  ep.extensionList.isNotEmpty()
