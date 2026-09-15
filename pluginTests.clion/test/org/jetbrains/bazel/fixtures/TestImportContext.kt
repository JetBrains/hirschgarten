package org.jetbrains.bazel.fixtures

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.IdeVirtualFileUrlManagerImpl
import org.jetbrains.bazel.clion.workspace.CcImportContext
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bsp.protocol.OutputLocationParser
import org.jetbrains.bsp.protocol.OutputLocationResolver
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
  // Just resolve everything against execroot in unit tests
  val testBazelInfo = testBazelInfo(workspaceRoot = execroot, outputBase = execroot, execRoot = execroot)

  override val outputParser: OutputLocationParser
    get() = OutputLocationParser(BazelPathsResolver(testBazelInfo), BazelOutFileHardLinks.NONE)

  override val outputResolver: OutputLocationResolver
    get() = DefaultOutputLocationResolver(testBazelInfo, BazelOutFileHardLinks.NONE)

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
