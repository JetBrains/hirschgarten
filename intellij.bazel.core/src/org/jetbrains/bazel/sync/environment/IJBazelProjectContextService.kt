package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.stateStore
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.toVersionString
import org.jetbrains.bazel.utils.findVirtualFile
import java.nio.file.Path

@com.intellij.openapi.components.State(
  name = "BazelProjectContextService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
internal class IJBazelProjectContextService(private val project: Project)
  : BazelProjectContextService, PersistentStateComponent<IJBazelProjectContextService.State> {

  override var bazelBinPath: Path? = null
  override var bazelExecPath: Path? = null
  override var workspaceName: String? = null
  override var bazelRelease: BazelRelease? = null

  override val isBazelProject: Boolean
    get() = project.stateStore.storeDescriptor is BazelProjectStoreDescriptor

  override val projectRootDir: VirtualFile?
    get() {
      val bazelProjectStoreDescriptor = project.stateStore.storeDescriptor as? BazelProjectStoreDescriptor
      return bazelProjectStoreDescriptor?.historicalProjectBasePath?.findVirtualFile()
             // Fallback for running a Bazel run configuration in a non-Bazel project
             ?: project.guessProjectDir()
    }

  override val avoidExternalSystem: Boolean = false

  override fun getState(): State = State(
    bazelBin = bazelBinPath?.toString(),
    execRoot = bazelExecPath?.toString(),
    workspaceName = workspaceName,
    bazelReleaseVersion = bazelRelease?.toVersionString(),
  )

  override fun loadState(state: State) {
    bazelBinPath = state.bazelBin?.toNioPathOrNull()
    bazelExecPath = state.execRoot?.toNioPathOrNull()
    workspaceName = state.workspaceName
    bazelRelease = state.bazelReleaseVersion?.let(BazelRelease::fromVersionString)
  }

  data class State(
    var bazelBin: String? = null,
    var execRoot: String? = null,
    var workspaceName: String? = null,
    var bazelReleaseVersion: String? = null,
  )
}
