package org.jetbrains.bazel.test.framework.testproject

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.sync.workspace.persistence.InMemoryWorkspaceTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndexBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMapBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphBuilder
import org.jetbrains.bsp.protocol.BuildTarget
import kotlin.io.path.name

class TestSnapshotBuilder internal constructor() {
  private val targets = mutableListOf<BuildTarget>()
  private var roots: MutableList<BuildTarget>? = null
  private var importDepth = -1
  private val repositories = TestRepositoriesBuilder()
  private var release: BazelRelease? = null

  fun targets(vararg targets: BuildTarget) {
    this.targets += targets
  }

  fun targets(targets: Iterable<BuildTarget>) {
    this.targets += targets
  }

  fun roots(vararg targets: BuildTarget) {
    val roots = roots ?: mutableListOf<BuildTarget>().also { roots = it }
    roots += targets
  }

  fun importDepth(depth: Int) {
    importDepth = depth
  }

  fun repositories(declare: TestRepositoriesBuilder.() -> Unit) {
    repositories.declare()
  }

  fun release(release: BazelRelease) {
    this.release = release
  }

  internal fun bazelInfoFrom(current: BazelInfo): BazelInfo = release?.let { current.copy(release = it) } ?: current

  internal fun repoMappingFrom(bazelInfo: BazelInfo): BzlmodRepoMapping = repositories.build(bazelInfo)

  internal fun build(
    bazelInfo: BazelInfo,
    repoMapping: BzlmodRepoMapping,
    languageSyncConfigs: List<WorkspaceSyncConfig>,
  ): WorkspaceSnapshot {
    val root = bazelInfo.workspaceRoot
    val commonSyncConfig = CommonWorkspaceSyncConfig(
      projectRootDir = root,
      dotIdeaPath = root.resolve(Project.DIRECTORY_STORE_FOLDER),
      projectName = root.name,
      importDepth = importDepth,
    )
    val targetsByKey = targets.associateBy { it.key }
    val rootKeys = (roots ?: targets).mapTo(HashSet()) { it.key }
    val targetGraph = WorkspaceTargetGraphBuilder.build(rootKeys, targetsByKey.values)
    return WorkspaceSnapshot(
      targets = InMemoryWorkspaceTargetMap(targetsByKey),
      workspaceName = "_main",
      configurations = emptyMap(),
      targetGraph = targetGraph,
      fileToTarget = File2TargetMapBuilder(bazelInfo, repoMapping).build(targetsByKey.values),
      executableTargets = ExecutableTargetsIndexBuilder.build(
        targetGraph = targetGraph,
        importDepth = importDepth,
        targets = targetsByKey.values,
      ),
      syncConfigs = listOf(commonSyncConfig) + languageSyncConfigs,
      repoMapping = repoMapping,
      bazelInfo = bazelInfo,
      metadata = WorkspaceSnapshotMetadata(version = 1),
    )
  }
}

fun testWorkspaceSnapshot(bazelInfo: BazelInfo, declare: TestSnapshotBuilder.() -> Unit = {}): WorkspaceSnapshot {
  val builder = TestSnapshotBuilder().apply(declare)
  val snapshotBazelInfo = builder.bazelInfoFrom(bazelInfo)
  return builder.build(snapshotBazelInfo, builder.repoMappingFrom(snapshotBazelInfo), languageSyncConfigs = emptyList())
}
