package org.jetbrains.bazel.ui.widgets.tool.window.all.targets

import com.intellij.build.BuildContentManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.client.ClientProjectSession
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rdserver.core.RemoteClientSessionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService

/**
 * We want the BSP tool window and the Build tool window to be visible when we open a project with remote dev.
 * However, calling `ToolWindow#show` before we have a client session will cause the call to be dropped
 * [here](https://code.jetbrains.team/p/ij/repositories/ultimate/files/4e3299ed354f40ac148ffca1c01c24d66df8c04a/remote-dev/cwm-host/src/toolWindow/BackendServerToolWindowManager.kt?tab=source&line=291&lines-count=1).
 * Hence the usage of a `RemoteClientSessionListener` to wait before the connection is established.
 * Another problem with showing a tool window too early is that it can be hidden by `ToolWindowManagerImpl#setLayout`, see
 * [this comment](https://code.jetbrains.team/p/ij/repositories/ultimate/files/66f6824f124da59d24e64111fcb085125305942b/remote-dev/cwm-guest/src/com/jetbrains/thinclient/toolWindow/generic/FrontendToolWindowHost.kt?tab=source&line=89&lines-count=2).
 * That's why we try to show the tool windows several times so that at least one call will be **after** `ToolWindowManagerImpl#setLayout`.
 */
class BazelRemoteClientSessionListener : RemoteClientSessionListener {
  override fun projectSessionInitialized(lifetime: Lifetime, session: ClientProjectSession) {
    val project = session.project
    BazelCoroutineService.getInstance(project).start {
      val delays = listOf<Long>(0, 100, 200, 500, 1000, 2000, 4000)
      for (delay in delays) {
        delay(delay)
        showToolWindows(project)
      }
    }
  }

  private suspend fun BazelRemoteClientSessionListener.showToolWindows(project: Project) {
    if (!project.isBazelProject) return
    showBspToolWindow(project)
    showBuildToolWindow(project)
  }

  private suspend fun showBuildToolWindow(project: Project) =
    withContext(Dispatchers.EDT) {
      BuildContentManager.getInstance(project).getOrCreateToolWindow().show()
    }
}
