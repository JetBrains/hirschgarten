package org.jetbrains.plugins.bsp.android

import com.android.sdklib.AndroidVersion
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.model.Namespacing
import com.android.tools.idea.model.queryMinSdkAndTargetSdkFromManifestIndex
import com.android.tools.lint.detector.api.Desugaring
import org.jetbrains.android.facet.AndroidFacet

class BspAndroidModel(private val facet: AndroidFacet, private val androidMinSdkOverride: Int?) : AndroidModel {
  override fun getApplicationId(): String = AndroidModel.UNINITIALIZED_APPLICATION_ID

  override fun getAllApplicationIds(): Set<String> = emptySet()

  override fun overridesManifestPackage(): Boolean = false

  override fun isDebuggable(): Boolean = true

  override fun getMinSdkVersion(): AndroidVersion {
    androidMinSdkOverride?.let {
      return AndroidVersion(it)
    }
    return facet.queryMinSdkAndTargetSdkFromManifestIndex().minSdk
  }

  override fun getRuntimeMinSdkVersion(): AndroidVersion = getMinSdkVersion()

  override fun getTargetSdkVersion(): AndroidVersion = facet.queryMinSdkAndTargetSdkFromManifestIndex().targetSdk

  override fun getNamespacing(): Namespacing = Namespacing.DISABLED

  /**
   * Assume full desugaring to avoid false positive red code
   */
  override fun getDesugaring(): Set<Desugaring> = Desugaring.FULL
}
