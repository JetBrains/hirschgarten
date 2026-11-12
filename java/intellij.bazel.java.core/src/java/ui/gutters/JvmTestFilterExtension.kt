package org.jetbrains.bazel.java.ui.gutters

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTargetInfo

@ApiStatus.Internal
interface JvmTestFilterExtension {
  fun isApplicable(project: Project, target: BuildTargetInfo): Boolean

  fun getTestFilter(className: String, method: PsiMethod?): String

  companion object {
    val ep: ExtensionPointName<JvmTestFilterExtension> = ExtensionPointName("org.jetbrains.bazel.jvmTestFilterExtension")

    fun getInstance(project: Project, target: BuildTargetInfo): JvmTestFilterExtension =
      checkNotNull(ep.findFirstSafe { it.isApplicable(project, target) })
  }
}

internal class DefaultJvmTestFilterExtension : JvmTestFilterExtension {
  override fun isApplicable(project: Project, target: BuildTargetInfo): Boolean = true

  override fun getTestFilter(className: String, method: PsiMethod?): String = getTestFilter(className, method?.name)
}
