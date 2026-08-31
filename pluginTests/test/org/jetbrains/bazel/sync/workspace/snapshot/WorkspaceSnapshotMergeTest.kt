package org.jetbrains.bazel.sync.workspace.snapshot

import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.junit.jupiter.api.Test
import java.nio.file.Path

@BazelTestApplication
class WorkspaceSnapshotMergeTest {
  val project by projectFixture()

  @Test
  fun `merge keeps the root targets of both snapshots`(): Unit = runBlocking {
    val previous = fullSync(
      resolved(
        roots = listOf("//base:base"),
        targets = listOf(target("//base:base")),
      ),
    )

    val merged = partialSync(
      previous,
      resolved(
        roots = listOf("//extra:extra"),
        targets = listOf(target("//extra:extra")),
      ),
    )

    merged.rootLabels() shouldContainExactlyInAnyOrder listOf("@//base:base", "@//extra:extra")
    merged.importedLabels() shouldContainExactlyInAnyOrder listOf("@//base:base", "@//extra:extra")
  }

  @Test
  fun `merge imports a dependency that only the new snapshot brings in`(): Unit = runBlocking {
    val previous = fullSync(
      resolved(
        roots = listOf("//base:base"),
        targets = listOf(target("//base:base")),
      ),
    )

    val merged = partialSync(
      previous,
      resolved(
        roots = listOf("//extra:extra"),
        targets = listOf(target("//extra:extra", deps = listOf("//lib:lib")), target("//lib:lib")),
      ),
    )

    merged.importedLabels() shouldContainExactlyInAnyOrder listOf("@//base:base", "@//extra:extra", "@//lib:lib")
  }

  @Test
  fun `merge takes the target of the new snapshot when both snapshots hold the key`(): Unit = runBlocking {
    val previous = fullSync(
      resolved(
        roots = listOf("//base:base"),
        targets = listOf(target("//base:base")),
      ),
    )

    val merged = partialSync(
      previous,
      resolved(
        roots = listOf("//base:base"),
        targets = listOf(target("//base:base", deps = listOf("//helper:helper")), target("//helper:helper")),
      ),
    )

    merged.dependencyLabelsOf("//base:base") shouldContainExactlyInAnyOrder listOf("@//helper:helper")
    merged.importedLabels() shouldContainExactlyInAnyOrder listOf("@//base:base", "@//helper:helper")
  }

  @Test
  fun `merge keeps a target that only the previous snapshot holds`(): Unit = runBlocking {
    val previous = fullSync(
      resolved(
        roots = listOf("//base:base", "//stale:stale"),
        targets = listOf(target("//base:base"), target("//stale:stale")),
      ),
    )

    val merged = partialSync(
      previous,
      resolved(
        roots = listOf("//extra:extra"),
        targets = listOf(target("//extra:extra")),
      ),
    )

    merged.importedLabels() shouldContainExactlyInAnyOrder listOf("@//base:base", "@//stale:stale", "@//extra:extra")
  }

  @Test
  fun `merge unions the bzlmod repo mapping of both snapshots`(): Unit = runBlocking {
    val previous = fullSync(
      resolved(
        roots = listOf("//base:base"),
        targets = listOf(target("//base:base")),
        repoMapping = bzlmod("dep_repo"),
      ),
    )

    val merged = partialSync(
      previous,
      resolved(
        roots = listOf("//extra:extra"),
        targets = listOf(target("//extra:extra")),
        repoMapping = bzlmod("ext_repo"),
      ),
    )

    val repoMapping = merged.repoMapping as BzlmodRepoMapping
    repoMapping.apparentRepoNameToCanonicalName.keys shouldContainExactlyInAnyOrder listOf("dep_repo", "ext_repo")
    repoMapping.canonicalRepoNameToLocalPath.keys shouldContainExactlyInAnyOrder listOf("dep_repo+", "ext_repo+")
    repoMapping.nonLocalCanonicalRepoNames shouldContainExactlyInAnyOrder listOf("dep_repo+ext", "ext_repo+ext")
  }

  @Test
  fun `merge keeps the repo mapping disabled when both snapshots disable it`(): Unit = runBlocking {
    val previous = fullSync(
      resolved(roots = listOf("//base:base"), targets = listOf(target("//base:base"))),
    )

    val merged = partialSync(
      previous,
      resolved(roots = listOf("//extra:extra"), targets = listOf(target("//extra:extra"))),
    )

    merged.repoMapping shouldBe RepoMappingDisabled
  }

  @Test
  fun `merge takes the workspace name of the new snapshot when there is no previous sync`(): Unit = runBlocking {
    val merged = partialSync(
      WorkspaceSnapshot.EMPTY,
      resolved(
        roots = listOf("//extra:extra"),
        targets = listOf(target("//extra:extra")),
        workspaceName = "_main",
      ),
    )

    merged.workspaceName shouldBe "_main"
    merged.importedLabels() shouldContainExactlyInAnyOrder listOf("@//extra:extra")
  }

  private suspend fun fullSync(resolved: BazelResolvedWorkspace): WorkspaceSnapshot =
    WorkspaceSnapshotBuilder.build(
      project = project,
      projectView = ProjectView.EMPTY,
      repoMapping = resolved.repoMapping,
      resolved = resolved,
    )

  private suspend fun partialSync(previous: WorkspaceSnapshot, resolved: BazelResolvedWorkspace): WorkspaceSnapshot {
    val incomplete = WorkspaceSnapshotBuilder.buildIncomplete(resolved = resolved)
    return WorkspaceSnapshotBuilder.merge(
      project = project,
      projectView = ProjectView.EMPTY,
      snapshots = listOf(previous.toIncompleteSnapshot(), incomplete),
    )
  }

  private fun resolved(
    roots: List<String>,
    targets: List<BuildTarget>,
    repoMapping: RepoMapping = RepoMappingDisabled,
    workspaceName: String = "_main",
  ): BazelResolvedWorkspace =
    BazelResolvedWorkspace(
      workspaceName = workspaceName,
      repoMapping = repoMapping,
      rootTargets = roots.map { WorkspaceTargetKey(label = Label.parse(it)) }.toSet(),
      targets = targets,
      configurations = emptyMap(),
    )

  private fun target(label: String, deps: List<String> = emptyList()): TestBuildTarget =
    TestBuildTarget(
      key = WorkspaceTargetKey(label = Label.parse(label)),
      dependencies = deps.map {
        DependencyLabel(
          targetKey = WorkspaceTargetKey(label = Label.parse(it)),
          kind = DependencyLabelKind.COMPILE,
        )
      },
    )

  private fun bzlmod(repoName: String): BzlmodRepoMapping =
    BzlmodRepoMapping(
      canonicalRepoNameToLocalPath = mapOf("$repoName+" to Path.of("/workspace/$repoName")),
      apparentRepoNameToCanonicalName = mapOf(repoName to "$repoName+"),
      canonicalRepoNameToPath = mapOf("$repoName+" to Path.of("/external/$repoName")),
      nonLocalCanonicalRepoNames = setOf("${repoName}+ext"),
    )

  private fun WorkspaceSnapshot.rootLabels(): List<String> =
    targetGraph.rootTargets.map { it.label.toString() }.toList()

  private fun WorkspaceSnapshot.importedLabels(): List<String> =
    targetGraph.findAllTargetsAtDepth(maxDepth = -1, useRelaxedDependencyExpansion = true)
      .map { it.label.toString() }
      .toList()

  private fun WorkspaceSnapshot.dependencyLabelsOf(label: String): List<String> =
    targets.let { map ->
      val key = targetGraph.findTargetByKey(WorkspaceTargetKey(label = Label.parse(label)))
                ?: error("target not found: $label")
      val target = map.findTargetByKey(key, TargetLoadOptions.ALL) ?: error("target not loaded: $label")
      target.dependencies.map { it.targetKey.label.toString() }
    }
}
