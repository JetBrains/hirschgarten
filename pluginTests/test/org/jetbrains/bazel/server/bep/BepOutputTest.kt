package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEventId
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.File
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.NamedSetOfFiles
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.OutputGroup
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.TargetComplete
import com.intellij.openapi.command.impl.DummyProject
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManagerResult
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationSummary
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.testProjectBazelInfo
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskStartParams
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.relativeToOrNull

@BazelTestApplication
internal class BepOutputTest {
  private class EmptyBuildTaskEventsHandler : BazelTaskEventsHandler {
    override fun onBuildLogMessage(p0: LogMessageParams) {}
    override fun onBuildPublishDiagnostics(p0: PublishDiagnosticsParams) {}
    override fun onBuildTaskStart(p0: TaskStartParams) {}
    override fun onBuildTaskFinish(p0: TaskFinishParams) {}
    override fun onPublishCoverageReport(report: CoverageReport) {}
    override fun onCachedTestLog(testLog: CachedTestLog) {}
  }

  private val bazelInfo = testProjectBazelInfo(workspaceRoot = Path("workspaceRoot"))

  private fun newBepSever(): BepServer {
    return BepServer(
      project = DummyProject.getInstance(),
      taskEventsHandler = EmptyBuildTaskEventsHandler(),
      diagnosticsService = DiagnosticsService(bazelInfo.workspaceRoot),
      parentId = TaskGroupId("originId").task(""),
      bazelPathsResolver = BazelPathsResolver(bazelInfo),
    )
  }

  private fun namedSet(name: String, files: List<String>, children: List<String>): BuildEvent =
    BuildEvent.newBuilder()
      .setId(BuildEventId.newBuilder().setNamedSet(BuildEventId.NamedSetOfFilesId.newBuilder().setId(name).build()).build())
      .setNamedSetOfFiles(
        NamedSetOfFiles.newBuilder()
          .addAllFiles(files.map { File.newBuilder().setName(it).build() })
          .addAllFileSets(children.map { BuildEventId.NamedSetOfFilesId.newBuilder().setId(it).build() }),
      ).build()

  private fun targetCompleted(label: String, outputGroupSets: Map<String, List<String>>): BuildEvent {
    val payloadBuilder = TargetComplete.newBuilder().setSuccess(true)
    outputGroupSets.forEach { (name, fileSets) ->
      payloadBuilder.addOutputGroup(
        OutputGroup.newBuilder()
          .setName(name)
          .addAllFileSets(fileSets.map { BuildEventId.NamedSetOfFilesId.newBuilder().setId(it).build() })
          .build(),
      )
    }
    return BuildEvent.newBuilder()
      .setId(BuildEventId.newBuilder().setTargetCompleted(BuildEventId.TargetCompletedId.newBuilder().setLabel(label).build()).build())
      .setCompleted(payloadBuilder.build())
      .build()
  }

  @Test
  fun testBasicOutputGroups() {
    val server = newBepSever()
    server.handleBuildEventStreamProtosEvent(namedSet("0", listOf("foo.intellij-info.txt"), listOf()))
    server.handleBuildEventStreamProtosEvent(namedSet("1", listOf("bar.intellij-info.txt"), listOf("0")))
    server.handleBuildEventStreamProtosEvent(targetCompleted("//:foobar", mapOf("intellij-info" to listOf("1"), "foo" to listOf("0"))))
    val bepOutput = server.getOutputForTesting()
    bepOutput.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet() shouldBe setOf("foo.intellij-info.txt", "bar.intellij-info.txt")

    // Renaming should not affect output group
    val renamed = bepOutput.renameNamedSets(2)
    renamed.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet() shouldBe setOf("foo.intellij-info.txt", "bar.intellij-info.txt")
  }

  @Test
  fun testCombineShards() {
    val firstRunServer = newBepSever()
    firstRunServer.handleBuildEventStreamProtosEvent(namedSet("0", listOf("firstfoo.intellij-info.txt"), listOf()))
    firstRunServer.handleBuildEventStreamProtosEvent(namedSet("1", listOf("firstbar.intellij-info.txt"), listOf()))
    firstRunServer.handleBuildEventStreamProtosEvent(
      targetCompleted(
        "//:first",
        mapOf("intellij-info" to listOf("0"), "bar" to listOf("1")),
      ),
    )
    val firstResult = firstRunServer.getOutputForTesting().renameNamedSets(1)
    firstResult.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet() shouldBe setOf("firstfoo.intellij-info.txt")

    val secondRunServer = newBepSever()
    secondRunServer.handleBuildEventStreamProtosEvent(namedSet("0", listOf("secondbar.intellij-info.txt"), listOf()))
    secondRunServer.handleBuildEventStreamProtosEvent(namedSet("1", listOf("secondfoo.intellij-info.txt"), listOf()))
    secondRunServer.handleBuildEventStreamProtosEvent(
      targetCompleted(
        "//:second",
        mapOf("intellij-info" to listOf("1"), "bar" to listOf("0")),
      ),
    )
    val secondResult = secondRunServer.getOutputForTesting().renameNamedSets(2)
    secondResult.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet() shouldBe setOf("secondfoo.intellij-info.txt")

    val combined = firstResult.merge(secondResult)
    combined.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet() shouldBe setOf(
      "firstfoo.intellij-info.txt",
      "secondfoo.intellij-info.txt",
    )
  }

  @Test
  fun testNestedOutputs() {
    val server = newBepSever()
    server.handleBuildEventStreamProtosEvent(namedSet("0", listOf("common_a.intellij-info.txt"), listOf()))
    server.handleBuildEventStreamProtosEvent(namedSet("1", listOf("common_b.intellij-info.txt"), listOf()))
    server.handleBuildEventStreamProtosEvent(namedSet("2", listOf(), listOf("0", "1")))
    server.handleBuildEventStreamProtosEvent(namedSet("3", listOf("foo.intellij-info.txt"), listOf()))
    server.handleBuildEventStreamProtosEvent(namedSet("4", listOf("foobar.intellij-info.txt"), listOf("3")))
    server.handleBuildEventStreamProtosEvent(namedSet("5", listOf("baz.intellij-info.txt"), listOf()))
    server.handleBuildEventStreamProtosEvent(
      targetCompleted(
        "//:foobarbaz",
        mapOf("foobar" to listOf("4", "2"), "intellij-info" to listOf("5", "2")),
      ),
    )
    val bepOutput = server.getOutputForTesting()
    bepOutput.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet() shouldBe setOf(
      "baz.intellij-info.txt",
      "common_b.intellij-info.txt",
      "common_a.intellij-info.txt",
    )

    // Renaming should not affect output group
    val renamed = bepOutput.renameNamedSets(42)
    renamed.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet() shouldBe setOf(
      "baz.intellij-info.txt",
      "common_b.intellij-info.txt",
      "common_a.intellij-info.txt",
    )
  }

  @Test
  fun testNestedOutputsCombined() {
    val firstServer = newBepSever()
    firstServer.handleBuildEventStreamProtosEvent(namedSet("0", listOf("common_a.intellij-info.txt"), listOf()))
    firstServer.handleBuildEventStreamProtosEvent(namedSet("1", listOf("common_b.intellij-info.txt"), listOf()))
    firstServer.handleBuildEventStreamProtosEvent(namedSet("2", listOf(), listOf("0", "1")))
    firstServer.handleBuildEventStreamProtosEvent(namedSet("3", listOf("build_only.intellij-info.txt"), listOf("2")))
    firstServer.handleBuildEventStreamProtosEvent(
      targetCompleted(
        "//:common",
        mapOf("sync" to listOf("2"), "intellij-info" to listOf("3")),
      ),
    )
    val afterFirstRun = firstServer.getOutputForTesting().renameNamedSets(1)
    val firstBuild = afterFirstRun.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet()
    firstBuild shouldBe setOf("common_a.intellij-info.txt", "common_b.intellij-info.txt", "build_only.intellij-info.txt")

    val secondServer = newBepSever()
    secondServer.handleBuildEventStreamProtosEvent(namedSet("0", listOf("build_2.intellij-info.txt"), listOf()))
    secondServer.handleBuildEventStreamProtosEvent(namedSet("1", listOf("sync_2.intellij-info.txt"), listOf()))
    secondServer.handleBuildEventStreamProtosEvent(targetCompleted("//:two", mapOf("sync" to listOf("1"), "intellij-info" to listOf("0"))))
    val afterSecondRun = afterFirstRun.merge(secondServer.getOutputForTesting().renameNamedSets(2))
    val secondBuild = afterSecondRun.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet()
    secondBuild shouldBe firstBuild + setOf("build_2.intellij-info.txt")

    val thirdServer = newBepSever()
    thirdServer.handleBuildEventStreamProtosEvent(namedSet("0", listOf("build_and_sync_3.intellij-info.txt"), listOf()))
    thirdServer.handleBuildEventStreamProtosEvent(namedSet("1", listOf("more_build_and_sync_3.intellij-info.txt"), listOf("0")))
    thirdServer.handleBuildEventStreamProtosEvent(namedSet("2", listOf("even_more_build_and_sync_3.intellij-info.txt"), listOf("1")))
    thirdServer.handleBuildEventStreamProtosEvent(namedSet("3", listOf("build_3.intellij-info.txt"), listOf("2")))
    thirdServer.handleBuildEventStreamProtosEvent(namedSet("4", listOf("sync_3.intellij-info.txt"), listOf("2")))
    thirdServer.handleBuildEventStreamProtosEvent(targetCompleted("//:three", mapOf("sync" to listOf("4"), "intellij-info" to listOf("3"))))
    val afterThirdRun = afterSecondRun.merge(thirdServer.getOutputForTesting().renameNamedSets(3))
    val thirdBuild = afterThirdRun.filesByInfoOutputGroup()
      .map { it.relativeToOrNull(bazelInfo.execRoot)?.toString() }.toSet()
    thirdBuild shouldBe secondBuild + setOf(
      "build_3.intellij-info.txt",
      "build_and_sync_3.intellij-info.txt",
      "more_build_and_sync_3.intellij-info.txt",
      "even_more_build_and_sync_3.intellij-info.txt",
    )
  }

  @Test
  fun testMergeAllPreservesDuplicateKeys() {
    val configurationId = WorkspaceConfigurationId.of("1234567")
    val firstConfiguration = WorkspaceConfiguration(
      configurationId,
      WorkspaceConfigurationSummary("first", "first", "", "", false),
      emptyList(),
    )
    val secondConfiguration = firstConfiguration.copy(summary = firstConfiguration.summary.copy(hash = "second"))
    val firstTarget = WorkspaceTargetKey(Label.parse("//:first"))
    val secondTarget = WorkspaceTargetKey(Label.parse("//:second"))
    val first = BepOutput(
      infoOutputGroupFileSets = setOf("shared"),
      textProtoFileSets = mapOf(
        "shared" to TextProtoDepSet(listOf(Path("first.txt")), listOf("first-child")),
        "first-child" to TextProtoDepSet(listOf(Path("first-child.txt")), emptyList()),
      ),
      rootTargets = setOf(firstTarget),
      options = listOf("--first"),
      configurations = mapOf(configurationId to firstConfiguration),
    )
    val second = BepOutput(
      infoOutputGroupFileSets = setOf("shared", "second-only"),
      textProtoFileSets = mapOf(
        "shared" to TextProtoDepSet(listOf(Path("second.txt")), listOf("second-child")),
        "second-child" to TextProtoDepSet(listOf(Path("second-child.txt")), emptyList()),
        "second-only" to TextProtoDepSet(listOf(Path("second-only.txt")), emptyList()),
      ),
      rootTargets = setOf(firstTarget, secondTarget),
      options = listOf("--second", "--last"),
      configurations = mapOf(configurationId to secondConfiguration),
      buildToolVersion = BazelRelease(8),
    )

    val merged = BepOutput.mergeAll(listOf(first, second, BepOutput(buildToolVersion = BazelRelease(9))))
    merged.filesByInfoOutputGroup().map { it.toString() }.toSet() shouldBe setOf(
      "first.txt", "first-child.txt", "second.txt", "second-child.txt", "second-only.txt",
    )
    merged.rootTargets() shouldBe setOf(firstTarget, secondTarget)
    merged.options shouldBe listOf("--first", "--second", "--last")
    merged.configurations[configurationId] shouldBe secondConfiguration
    merged.buildToolVersion shouldBe BazelRelease(8)
    first.merge(second).filesByInfoOutputGroup().map { it.toString() }.toSet() shouldBe
      merged.filesByInfoOutputGroup().map { it.toString() }.toSet()
  }

  @Test
  fun testMergeAllPreservesNestedSetsAcrossManyShards() {
    val shards = (1..128).map { shard ->
      BepOutput(
        infoOutputGroupFileSets = setOf("root"),
        textProtoFileSets = mapOf(
          "root" to TextProtoDepSet(listOf(Path("root-$shard.txt")), listOf("leaf")),
          "leaf" to TextProtoDepSet(listOf(Path("leaf-$shard.txt")), emptyList()),
        ),
        options = listOf("--shard=$shard"),
      ).renameNamedSets(shard)
    }

    val merged = BepOutput.mergeAll(shards)
    merged.filesByInfoOutputGroup().map { it.toString() }.toSet() shouldBe (1..128).flatMap { shard ->
      listOf("root-$shard.txt", "leaf-$shard.txt")
    }.toSet()
    merged.options shouldBe (1..128).map { "--shard=$it" }
  }

  @Test
  fun testMergeAllAcceptsEmptyInput() {
    val merged = BepOutput.mergeAll(emptyList())
    merged.rootTargets() shouldBe emptySet()
    merged.filesByInfoOutputGroup() shouldBe emptySet()
    merged.options shouldBe emptyList()
    merged.configurations shouldBe emptyMap()
    merged.buildToolVersion shouldBe BazelRelease.FALLBACK_VERSION
    BazelBspAspectsManagerResult.mergeAll(emptyList()).status shouldBe BazelStatus.SUCCESS
  }

  @Test
  fun testMergeAllPreservesStatusPriority() {
    val results = listOf(
      BazelBspAspectsManagerResult(BepOutput(options = listOf("first")), BazelStatus.BUILD_ERROR),
      BazelBspAspectsManagerResult(BepOutput(options = listOf("second")), BazelStatus.OOM_ERROR),
      BazelBspAspectsManagerResult(BepOutput(options = listOf("third")), BazelStatus.FATAL_ERROR),
    )

    val merged = BazelBspAspectsManagerResult.mergeAll(results)
    merged.status shouldBe BazelStatus.OOM_ERROR
    merged.bepOutput.options shouldBe listOf("first", "second", "third")
    results[0].merge(results[1]).merge(results[2]).status shouldBe merged.status
  }
}
