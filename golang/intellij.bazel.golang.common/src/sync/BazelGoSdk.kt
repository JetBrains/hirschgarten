package org.jetbrains.bazel.golang.sync

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkImpl
import com.intellij.openapi.project.Project

// 262-only hack for https://youtrack.jetbrains.com/issue/BAZEL-3411 to avoid breaking API compatibility
// 263 and newer get a proper fix on the Go plugin side
internal class BazelGoSdk(original: GoSdk) : GoSdkImpl(original.homeUrl, original.version, original.versionFilePath) {
  // rules_go allows internal imports and doesn't do any additional checks
  override fun supportsInternalPackages(): Boolean = false
  override fun supportsSdkInternalPackages(): Boolean = false
  override fun debugString(project: Project?): String =
    "BazelGoSdk: " + super.debugString(project)
}
