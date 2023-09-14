package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.sdkFlavor
import org.jetbrains.plugins.bsp.server.tasks.PythonSdk
import java.net.URI
import kotlin.io.path.toPath

public interface PythonSdkGetterExtension {
  public fun getPythonSdk(
    pythonSdk: PythonSdk,
    jdkTable: ProjectJdkTable,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): Sdk

  public fun getSystemSdk(): PyDetectedSdk?

  public fun hasDetectedPythonSdk(): Boolean
}

private val ep =
  ExtensionPointName.create<PythonSdkGetterExtension>(
    "com.intellij.pythonSdkGetterExtension",
  )

public fun pythonSdkGetterExtension(): PythonSdkGetterExtension? =
  ep.extensionList.firstOrNull()

public fun pythonSdkGetterExtensionExists(): Boolean =
  ep.extensionList.isNotEmpty()

public class PythonSdkGetter : PythonSdkGetterExtension {
  private val defaultPythonSdk: PyDetectedSdk? = detectSystemWideSdks(null, emptyList()).firstOrNull { sdk ->
    val homePath = sdk.homePath
    homePath != null && sdk.sdkFlavor.getLanguageLevel(homePath).isPy3K
  }

  override fun getPythonSdk(
    pythonSdk: PythonSdk,
    jdkTable: ProjectJdkTable,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): Sdk {
    val allJdks = jdkTable.allJdks.toList()
    val additionalData = PythonSdkAdditionalData()
    val virtualFiles = pythonSdk.dependencies
      .flatMap { it.sources }
      .mapNotNull {
        URI.create(it)
          .toPath()
          .toVirtualFileUrl(virtualFileUrlManager)
          .virtualFile
      }
      .toSet()
    additionalData.setAddedPathsFromVirtualFiles(virtualFiles)

    return SdkConfigurationUtil.createSdk(
      allJdks,
      URI.create(pythonSdk.interpreterUri).toPath().toString(),
      PythonSdkType.getInstance(),
      additionalData,
      pythonSdk.name,
    )
  }

  override fun getSystemSdk(): PyDetectedSdk? = defaultPythonSdk

  override fun hasDetectedPythonSdk(): Boolean = defaultPythonSdk != null
}
