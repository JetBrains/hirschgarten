package org.jetbrains.bazel.assertions

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey

internal fun assertThat(actual: WorkspaceTargetKey?): WorkspaceTargetKeyAssert = WorkspaceTargetKeyAssert(actual)

internal class WorkspaceTargetKeyAssert(actual: WorkspaceTargetKey?) :
  AbstractObjectAssert<WorkspaceTargetKeyAssert, WorkspaceTargetKey>(actual, WorkspaceTargetKeyAssert::class.java) {

  fun hasLabel(label: String): WorkspaceTargetKeyAssert {
    isNotNull()
    assertThat(actual?.label).isEqualTo(Label.parse(label))
    return this
  }
}
