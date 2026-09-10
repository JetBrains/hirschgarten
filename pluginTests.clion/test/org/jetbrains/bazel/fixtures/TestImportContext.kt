package org.jetbrains.bazel.fixtures

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.IdeVirtualFileUrlManagerImpl
import org.jetbrains.bazel.clion.workspace.CcImportContext
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.toExecrootPath
import java.nio.file.Path

internal fun <T> withTestImportContext(
  snapshot: WorkspaceSnapshot,
  project: Project,
  execroot: Path = Path.of("/execroot"),
  body: context(CcImportContext) () -> T,
): Pair<TestImportContext, T> {
  val ctx = TestImportContext(snapshot, project, execroot)
  return ctx to body(ctx)
}

internal class TestImportContext(
  override val snapshot: WorkspaceSnapshot,
  override val project: Project,
  override val execroot: Path = Path.of("/execroot"),
) : CcImportContext {

  val events: MutableList<String> = mutableListOf()

  override val vfuManager: VirtualFileUrlManager by lazy {
    IdeVirtualFileUrlManagerImpl()
  }

  override fun reportEvent(severity: MessageEvent.Kind, message: String, description: String?) {
    events += "$severity: $message"
  }

  override fun resolve(location: OutputLocation): Path {
    return execroot.resolve(location.toExecrootPath())
  }
}
