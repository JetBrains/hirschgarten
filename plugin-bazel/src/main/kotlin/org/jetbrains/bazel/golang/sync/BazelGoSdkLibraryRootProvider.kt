package org.jetbrains.bazel.golang.sync

import com.goide.GoIcons
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.roots.SyntheticLibrary.ExcludeFileCondition
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import java.util.function.BooleanSupplier
import javax.swing.Icon

/**
 * Based on [com.goide.project.GoSdkLibraryRootProvider]
 *
 * This is a workaround for the case that there is no module registered in the project
 */
class BazelGoSdkLibraryRootProvider : AdditionalLibraryRootsProvider() {
  override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
    val result = mutableSetOf<SyntheticLibrary>()
    val sdk = GoSdkService.getInstance(project).getSdk(null)
    if (sdk.isValid) {
      result.add(GoSdkLibrary(sdk))
    }
    return result
  }

  override fun getRootsToWatch(project: Project): Collection<VirtualFile> {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      return emptySet()
    }
    val result = mutableSetOf<VirtualFile>()
    val sdk = GoSdkService.getInstance(project).getSdk(null)
    if (sdk.isValid) {
      ContainerUtil.addIfNotNull(result, sdk.sdkRoot)
    }
    return result
  }

  private class GoSdkLibrary(sdk: GoSdk) :
    SyntheticLibrary(
      "GoSdkLib::" + sdk.homeUrl,
      ExcludeFileCondition {
        _: Boolean,
        name: String?,
        _: BooleanSupplier?,
        _: BooleanSupplier?,
        _: BooleanSupplier?,
        ->
        "testdata".contentEquals(
          name,
        )
      },
    ),
    ItemPresentation {
    private val myRoots: Collection<VirtualFile> = sdk.rootsToAttach

    private val myVersion = StringUtil.notNullize(sdk.version)

    override fun getSourceRoots(): Collection<VirtualFile> = myRoots

    override fun getPresentableText(): String = "Go SDK $myVersion"

    override fun getIcon(unused: Boolean): Icon = GoIcons.ICON

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || javaClass != other.javaClass) return false

      val library = other as GoSdkLibrary

      if (myRoots != library.myRoots) return false
      return myVersion == library.myVersion
    }

    override fun hashCode(): Int {
      var result = myRoots.hashCode()
      result = 31 * result + myVersion.hashCode()
      return result
    }
  }
}
