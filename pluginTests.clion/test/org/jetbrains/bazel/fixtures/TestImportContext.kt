package org.jetbrains.bazel.fixtures

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.IdeVirtualFileUrlManagerImpl
import org.jetbrains.bazel.clion.workspace.CcImportContext
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.test.framework.testproject.outputParser
import org.jetbrains.bazel.test.framework.testproject.outputResolver
import org.jetbrains.bsp.protocol.OutputLocationParser
import org.jetbrains.bsp.protocol.OutputLocationResolver
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

internal suspend fun <T> withTestImportContext(
  snapshot: WorkspaceSnapshot,
  project: Project,
  body: suspend context(CcImportContext) () -> T,
): Pair<TestImportContext, T> {
  val ctx = TestImportContext(snapshot, project)
  return ctx to body(ctx)
}

internal class TestImportContext(
  override val snapshot: WorkspaceSnapshot,
  override val project: Project,
) : CcImportContext {
  override val execroot: Path
    get() = snapshot.bazelInfo.execRoot

  override val taskId: TaskId
    get() = TaskGroupId("test").task("task")

  override val outputParser: OutputLocationParser
    get() = snapshot.bazelInfo.outputParser()

  override val outputResolver: OutputLocationResolver
    get() = snapshot.bazelInfo.outputResolver()

  val events: MutableList<TestImportEvent> = mutableListOf()

  override val vfuManager: VirtualFileUrlManager by lazy {
    IdeVirtualFileUrlManagerImpl()
  }

  override fun reportEvent(severity: MessageEvent.Kind, message: String, description: String?) {
    events += TestImportEvent(severity, message, description)
  }
}

/** An event that an import step reported. */
internal data class TestImportEvent(val severity: MessageEvent.Kind, val message: String, val description: String?)
