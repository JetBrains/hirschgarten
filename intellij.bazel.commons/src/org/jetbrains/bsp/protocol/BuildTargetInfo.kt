package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey

@ApiStatus.Internal
data class BuildTargetInfo(
  val key: WorkspaceTargetKey,
  val kind: TargetKind,
  val generatorName: String?,
  val isTestOnly: Boolean,
  val tags: List<String>,
  val workspaceName: String,
) {
  val id: Label
    get() = key.label
}

@ApiStatus.Internal
fun BuildTargetInfo(label: Label, kind: TargetKind, tags: List<String> = listOf()): BuildTargetInfo =
  BuildTargetInfo(
    key = WorkspaceTargetKey(label = label),
    kind = kind,
    generatorName = null,
    isTestOnly = false,
    tags = tags,
    workspaceName = "_main",
  )

/**
 * From Bazel doc (https://bazel.build/reference/be/common-definitions)
 * manual tag will exclude the target from expansion of target pattern wildcards (..., :*, :all, etc.) and test_suite rules which do not
 * list the test explicitly when computing the set of top-level targets to build/run for the build, test, and coverage commands
 */
@get:ApiStatus.Internal
val BuildTargetInfo.isManual: Boolean
  get() = BuildTargetTag.MANUAL in tags
