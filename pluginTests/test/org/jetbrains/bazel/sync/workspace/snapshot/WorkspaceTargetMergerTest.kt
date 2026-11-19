package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.copy
import org.junit.jupiter.api.Test

class WorkspaceTargetMergerTest {
  @Test
  fun `merging aspects preserves all missing generated sources`() {
    val base = javaLibrary("//app:main")
    val shared = OutputLocation.Output(TEST_OUTPUT_ROOT, "Shared.java")
    val first = OutputLocation.Output(TEST_OUTPUT_ROOT, "First.java")
    val second = OutputLocation.Output(TEST_OUTPUT_ROOT, "Second.java")

    fun target(aspect: String, vararg sources: OutputLocation): BuildTarget = base.copy(
      info = base.info.copy(key = base.key.copy(aspectIds = WorkspaceAspectIds.of(listOf(aspect)))),
      data = listOf(MissingGeneratedSources(OutputLocationCollectionBuilder.ofLocations(sources.toList()))),
    )

    val merged = WorkspaceTargetMerger(emptyMap()).mergeByTargetKey(
      listOf(target("first", shared, first), target("second", shared, second)),
    ).single()

    merged.findBuildData<MissingGeneratedSources>()?.sources?.getOutputLocations()?.toList() shouldContainExactlyInAnyOrder
      listOf(shared, first, second)
  }
}
