package org.jetbrains.bazel.test.framework.testproject

import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection

class TestTargetBuilder internal constructor(private val label: Label, private val kind: TargetKind) {
  private val sources = mutableListOf<OutputLocation>()
  private val resources = mutableListOf<OutputLocation>()
  private val dependencies = mutableListOf<DependencyLabel>()
  private val data = mutableListOf<BuildTargetData>()

  fun srcs(vararg paths: String, generated: Boolean = false) {
    sources += locations(paths, generated)
  }

  fun resources(vararg paths: String, generated: Boolean = false) {
    resources += locations(paths, generated)
  }

  fun deps(vararg labels: String) {
    dependencies += labels.map { DependencyLabel(targetKey = WorkspaceTargetKey(label = Label.parse(it))) }
  }

  fun data(vararg items: BuildTargetData) {
    for (item in items) {
      val index = data.indexOfFirst { it::class == item::class }
      if (index < 0) {
        data += item
      }
      else {
        data[index] = item
      }
    }
  }

  internal fun build(): TestBuildTarget =
    TestBuildTarget(
      key = WorkspaceTargetKey(label = label),
      dependencies = dependencies.toList(),
      kind = kind,
      sources = OutputLocationCollectionBuilder.ofLocations(sources),
      resources = OutputLocationCollectionBuilder.ofLocations(resources),
      data = data.toList(),
    )
}

private fun locations(paths: Array<out String>, generated: Boolean): List<OutputLocation> =
  paths.map { if (generated) OutputLocation.Output(TEST_OUTPUT_ROOT, it) else OutputLocation.Workspace(it) }

fun buildTarget(label: Label, kind: TargetKind, declare: TestTargetBuilder.() -> Unit = {}): TestBuildTarget =
  TestTargetBuilder(label, kind).apply(declare).build()

fun buildTarget(label: String, kind: TargetKind, declare: TestTargetBuilder.() -> Unit = {}): TestBuildTarget =
  buildTarget(Label.parse(label), kind, declare)

fun workspaceLocations(vararg relativePaths: String): OutputLocationCollection =
  OutputLocationCollectionBuilder.ofLocations(relativePaths.map(OutputLocation::Workspace))
