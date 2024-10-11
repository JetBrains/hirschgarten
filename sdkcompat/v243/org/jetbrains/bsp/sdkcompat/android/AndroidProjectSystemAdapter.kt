package org.jetbrains.bsp.sdkcompat.android

import com.android.tools.idea.projectsystem.AndroidProjectSystem
import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet

// v243: getAndroidFacetsWithPackageName dropped the project parameter
abstract class AndroidProjectSystemAdapter : AndroidProjectSystem {
  abstract fun getAndroidFacetsWithPackageNameCompat(project: Project, packageName: String): Collection<AndroidFacet>

  override fun getAndroidFacetsWithPackageName(packageName: String): Collection<AndroidFacet> =
    getAndroidFacetsWithPackageNameCompat(project, packageName)
}
