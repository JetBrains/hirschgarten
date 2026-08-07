package org.jetbrains.bazel.golang

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.golang.sync.BazelGoSdk
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil
import org.jetbrains.bazel.startup.utils.BazelProjectActivity

// 262-only hack for https://youtrack.jetbrains.com/issue/BAZEL-3411 to avoid breaking API compatibility
// 263 and newer get a proper fix on the Go plugin side
internal class BazelGoStartupActivity : BazelProjectActivity() {
  override suspend fun executeForBazelProject(project: Project) {
    if (!BazelFeatureFlags.isGoSupportEnabled) return
    val goWorkspaceModule = GoWorkspaceModuleUtil.findModule(project) ?: return
    val goSdkService = GoSdkService.getInstance(project)
    val originalSdk = goSdkService.getSdk(goWorkspaceModule)
    if (originalSdk === GoSdk.NULL) return
    edtWriteAction { goSdkService.setSdk(BazelGoSdk(originalSdk)) }
  }
}
