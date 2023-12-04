package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import org.jetbrains.plugins.bsp.server.tasks.ScalaSdk

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

public fun scalaSdkExtension(): ScalaSdkExtension? =
  ep.extensionList.firstOrNull()

public fun scalaSdkExtensionExists(): Boolean =
  ep.extensionList.isNotEmpty()
