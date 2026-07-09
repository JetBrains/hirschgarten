package org.jetbrains.bazel.sync.workspace.snapshot

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
fun IntellijIdeInfo.TargetKey.toWorkspaceTargetKey(): WorkspaceTargetKey = WorkspaceTargetKey(
  label = Label.parse(this.label),
  configuration = WorkspaceConfigurationId.of(this.configuration),
  aspectIds = WorkspaceAspectIds.of(this.aspectIdsList),
)

@get:ApiStatus.Internal
val BuildEventStreamProtos.BuildEventId.workspaceTargetKey: WorkspaceTargetKey?
  get() {
    if (!this.hasTargetCompleted()) {
      return null
    }
    return WorkspaceTargetKey(
      label = Label.parseOrNull(targetCompleted.label) ?: return null,
      configuration = WorkspaceConfigurationId.of(targetCompleted.configuration.id),
      aspectIds = WorkspaceAspectIds.of(listOf(targetCompleted.aspect)),
    )
  }

@get:ApiStatus.Internal
val WorkspaceAspectIds.labelIds: List<Label>
  get() = ids.mapNotNull { Label.parseOrNull(it) }

@get:ApiStatus.Internal
val WorkspaceConfiguration.isExecConfig: Boolean
  get() = summary.mnemonic.endsWith("-exec") || summary.mnemonic == "exec"
