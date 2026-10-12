package org.jetbrains.bazel.workspace.model.test.framework

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.persistence.InMemoryWorkspaceTargetMap
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndexBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMapBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.target.TargetStorage
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.OutputLocationResolver
import org.jetbrains.bsp.protocol.OutputRoot
import java.nio.file.Path

val TEST_OUTPUT_ROOT: OutputRoot = OutputRoot.of(listOf("k8-fastbuild", "bin"))

val testOutputResolver: OutputLocationResolver = DefaultOutputLocationResolver.createHardlinkResolving(testBazelInfo())

fun testLocation(path: Path): OutputLocation = OutputLocation.Host(path.toString())

fun generatedTestLocation(relativePath: String): OutputLocation = OutputLocation.Output(TEST_OUTPUT_ROOT, relativePath)

fun resolveTestLocation(location: OutputLocation): Path? = testOutputResolver.resolve(location)

fun testLocations(paths: List<Path>): OutputLocationCollection = OutputLocationCollectionBuilder.ofLocations(paths.map(::testLocation))

fun createTestBuildTarget(
  id: Label = Label.parse("//target"),
  dependencies: List<DependencyLabel> = emptyList(),
  kind: TargetKind = TargetKind(
    kind = "java_library",
    ruleType = RuleType.LIBRARY,
    languageClasses = setOf(JavaLanguageClass.JAVA),
  ),
  sources: List<Path> = emptyList(),
  generatedSources: List<OutputLocation> = emptyList(),
  resources: List<Path> = emptyList(),
  data: List<BuildTargetData> = emptyList(),
  isTestOnly: Boolean = false,
): TestBuildTarget =
  TestBuildTarget(
    key = WorkspaceTargetKey(label = id),
    dependencies = dependencies,
    kind = kind,
    sources = OutputLocationCollectionBuilder.ofLocations(sources.map(::testLocation) + generatedSources),
    resources = testLocations(resources),
    data = data,
    isTestOnly = isTestOnly,
  )

fun Project.setTargets(targets: List<BuildTarget>, bazelInfo: BazelInfo = BazelInfo.DEFAULT, repoMapping: RepoMapping = RepoMappingDisabled) {
  val project = this
  val graph = WorkspaceTargetGraphBuilder.build(
    rootTargets = targets.map { it.key }.toSet(),
    targets = targets,
  )
  val snapshot = WorkspaceSnapshot(
    targets = InMemoryWorkspaceTargetMap(targets.associateBy { it.key }),
    workspaceName = null,
    configurations = mapOf(),
    targetGraph = graph,
    fileToTarget = File2TargetMapBuilder(bazelInfo, repoMapping).build(targets = targets),
    executableTargets = ExecutableTargetsIndexBuilder.build(targetGraph = graph, importDepth = 0, targets = targets),
    syncConfigs = listOf(
      CommonWorkspaceSyncConfig(
        projectRootDir = Path.of(project.basePath!!),
        projectName = project.name,
        importDepth = 0,
        dotIdeaPath = null,
      ),
    ),
    repoMapping = repoMapping,
    bazelInfo = bazelInfo,
    metadata = WorkspaceSnapshotMetadata(version = 1),
  )
  runBlocking {
    project.service<WorkspaceSnapshotService>().update { snapshot to Unit }
    project.service<TargetStorage>().views.first { it.snapshot === snapshot }
  }
}
