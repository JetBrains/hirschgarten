package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import org.jetbrains.plugins.bsp.server.tasks.ScalaSdk

public interface ScalaSdkGetterExtension {
  public fun addScalaSdk(
    scalaSdk: ScalaSdk,
    modelsProvider: IdeModifiableModelsProvider,
  )
}

private val ep =
  ExtensionPointName.create<ScalaSdkGetterExtension>(
    "com.intellij.scalaSdkGetterExtension",
  )

public fun scalaSdkGetterExtension(): ScalaSdkGetterExtension? =
  ep.extensionList.firstOrNull()

public fun scalaSdkGetterExtensionExists(): Boolean =
  ep.extensionList.isNotEmpty()
