package org.jetbrains.bazel.android

import com.android.sdklib.AndroidVersion
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.model.Namespacing
import com.android.tools.idea.model.queryMinSdkAndTargetSdkFromManifestIndex
import com.android.tools.lint.detector.api.Desugaring
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import org.jetbrains.android.facet.AndroidFacet

private const val MIN_SDK_VERSION = "minSdkVersion"
private const val TARGET_SDK_VERSION = "targetSdkVersion"

class BazelAndroidModel(private val facet: AndroidFacet, private val manifestOverrides: Map<String, String>) : AndroidModel {
  private val project: Project = facet.module.project

  override fun getApplicationId(): String = AndroidModel.UNINITIALIZED_APPLICATION_ID

  override fun getAllApplicationIds(): Set<String> = emptySet()

  override fun overridesManifestPackage(): Boolean = false

  override fun isDebuggable(): Boolean = true

  override fun getMinSdkVersion(): AndroidVersion =
    manifestOverrides[MIN_SDK_VERSION]?.let {
      return AndroidVersion(it)
    } ?: runReadActionInSmartMode {
      facet.queryMinSdkAndTargetSdkFromManifestIndex().minSdk
    }

  override fun getRuntimeMinSdkVersion(): AndroidVersion = getMinSdkVersion()

  override fun getTargetSdkVersion(): AndroidVersion =
    manifestOverrides[TARGET_SDK_VERSION]?.let {
      return AndroidVersion(it)
    } ?: runReadActionInSmartMode {
      facet.queryMinSdkAndTargetSdkFromManifestIndex().targetSdk
    }

  override fun getNamespacing(): Namespacing = Namespacing.DISABLED

  /**
   * Assume full desugaring to avoid false positive red code
   */
  override fun getDesugaring(): Set<Desugaring> = Desugaring.FULL

  private fun <T> runReadActionInSmartMode(block: () -> T): T =
    DumbService.getInstance(project).runReadActionInSmartMode(
      Computable {
        block()
      },
    )
}
