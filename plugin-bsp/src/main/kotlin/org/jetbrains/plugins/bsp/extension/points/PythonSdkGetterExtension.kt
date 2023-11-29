package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.guessedLanguageLevel
import org.jetbrains.plugins.bsp.server.tasks.PythonSdk
import java.net.URI
import kotlin.io.path.toPath

public interface PythonSdkGetterExtension {
  public fun getPythonSdk(
    pythonSdk: PythonSdk,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): Sdk

  public fun getSystemSdk(): PyDetectedSdk?

  public fun hasDetectedPythonSdk(): Boolean
}

private val ep =
  ExtensionPointName.create<PythonSdkGetterExtension>(
    "org.jetbrains.bsp.pythonSdkGetterExtension",
  )

public fun pythonSdkGetterExtension(): PythonSdkGetterExtension? =
  ep.extensionList.firstOrNull()

public fun pythonSdkGetterExtensionExists(): Boolean =
  ep.extensionList.isNotEmpty()

public class PythonSdkGetter : PythonSdkGetterExtension {
  private var defaultPythonSdk: PyDetectedSdk? = null

  override fun getPythonSdk(
    pythonSdk: PythonSdk,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): Sdk {
    val sdk = ProjectJdkImpl(
      pythonSdk.name,
      PythonSdkType.getInstance(),
    )
    sdk.homePath = URI(pythonSdk.interpreterUri).toPath().toString()
    sdk.versionString // needs to be invoked in order to fetch the version and cache it
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
    sdk.sdkAdditionalData = additionalData

    return sdk
  }

  override fun getSystemSdk(): PyDetectedSdk? =
    defaultPythonSdk ?: detectSystemWideSdks(null, emptyList()).firstOrNull {
      it.homePath != null && it.guessedLanguageLevel?.isPy3K == true
    }?.also { defaultPythonSdk = it }

  override fun hasDetectedPythonSdk(): Boolean = defaultPythonSdk != null
}
