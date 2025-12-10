package org.jetbrains.bazel.action.registered

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.workspaceModel.ide.toPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.ui.widgets.queryTab.registerBazelQueryToolWindow
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import kotlin.io.path.Path

@Suppress("ActionPresentationInstantiatedInCtor")
class AbuMagicAction :
  SuspendableAction(
    "Abu magic action",
    AllIcons.Providers.MongoDB,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    //project.connection.runWithServer { bspServer ->
    //  bspServer
    //    //.buildTargetInverseSources(InverseSourcesParams(TextDocumentIdentifier(Path("/BENCHMARK"))))
    //    .buildTargetInverseSources(InverseSourcesParams(TextDocumentIdentifier(Path("/Users/mkocot/Documents/work/_READONLY/ultimate_test/plugins/api-watcher/test/com/jetbrains/apiwatcher/tests/misc/KotlinK1RevisionUtilTest.kt"))))
    //}
  }
}
