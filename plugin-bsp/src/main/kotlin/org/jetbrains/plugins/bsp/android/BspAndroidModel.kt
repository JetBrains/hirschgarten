package org.jetbrains.plugins.bsp.android

import com.android.sdklib.AndroidVersion
import com.android.tools.idea.model.AndroidModel
import com.android.tools.idea.model.Namespacing
import com.android.tools.idea.projectsystem.NamedIdeaSourceProvider
import com.android.tools.lint.detector.api.Desugaring

public class BspAndroidModel(public val sourceProvider: NamedIdeaSourceProvider) : AndroidModel {
  override fun getApplicationId(): String = AndroidModel.UNINITIALIZED_APPLICATION_ID

  override fun getAllApplicationIds(): Set<String> = setOf(applicationId)

  override fun overridesManifestPackage(): Boolean = false

  override fun isDebuggable(): Boolean = true

  override fun getMinSdkVersion(): AndroidVersion = AndroidVersion(1)

  override fun getRuntimeMinSdkVersion(): AndroidVersion = AndroidVersion(1)

  override fun getTargetSdkVersion(): AndroidVersion? = null

  override fun getNamespacing(): Namespacing = Namespacing.DISABLED

  override fun getDesugaring(): Set<Desugaring> = Desugaring.DEFAULT
}
