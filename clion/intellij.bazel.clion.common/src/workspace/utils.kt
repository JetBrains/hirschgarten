package org.jetbrains.bazel.clion.workspace

import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey

fun WorkspaceTargetKey.presentable(): String {
  return label.toString() + configuration.shortChecksum?.let { " ($it)" }.orEmpty()
}
