package org.jetbrains.plugins.bsp.android

import com.android.sdklib.IAndroidTarget
import com.android.tools.idea.sdk.AndroidSdks
import com.android.tools.sdk.AndroidSdkData
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk

public interface AndroidSdkGetterExtension {
  public fun getAndroidSdk(androidSdk: AndroidSdk): Sdk?
}

private val ep =
  ExtensionPointName.create<AndroidSdkGetterExtension>(
    "org.jetbrains.bsp.androidSdkGetterExtension",
  )

public fun androidSdkGetterExtension(): AndroidSdkGetterExtension? =
  ep.extensionList.firstOrNull()

public fun androidSdkGetterExtensionExists(): Boolean =
  ep.extensionList.isNotEmpty()

public class AndroidSdkGetter : AndroidSdkGetterExtension {
  override fun getAndroidSdk(androidSdk: AndroidSdk): Sdk? {
    // AndroidSdks will alter the Sdk name by adding (1), (2) and so on if an Sdk with the same name exists already,
    // so we can't create the Sdk with the requested name in that case.
    val sdkTable = ProjectJdkTable.getInstance()
    if (sdkTable.allJdks.any { sdk -> sdk.name == androidSdk.name }) {
      return null
    }

    // The android.jar is located at <sdkPath>/platforms/android-<versionNumber>/android.jar, so we go 3 levels up
    val androidJar = androidSdk.androidJar
    val sdkPath = androidJar.parent?.parent?.parent ?: return null
    val sdkData = AndroidSdkData.getSdkData(sdkPath.toFile()) ?: return null

    val target = sdkData.targets.firstOrNull { androidTarget ->
      androidTarget.getPath(IAndroidTarget.ANDROID_JAR) == androidJar
    } ?: return null

    return AndroidSdks.getInstance().create(target, sdkPath.toFile(), androidSdk.name, true)
  }
}
