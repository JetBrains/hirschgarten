package org.jetbrains.bazel.test.framework

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.kotest.common.runBlocking
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot

fun Project.updateWorkspaceSnapshot(updater: (snapshot: WorkspaceSnapshot) -> WorkspaceSnapshot) =
  runBlocking { service<WorkspaceSnapshotService>().update { snapshot -> updater(snapshot) to Unit } }

fun Project.updateBazelInfo(updater: (bazelInfo: BazelInfo) -> BazelInfo) =
  updateWorkspaceSnapshot { snapshot -> snapshot.copy(bazelInfo = updater(snapshot.bazelInfo)) }
