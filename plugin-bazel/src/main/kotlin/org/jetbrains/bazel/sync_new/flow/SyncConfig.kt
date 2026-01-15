package org.jetbrains.bazel.sync_new.flow

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.languages.projectview.ProjectViewWorkspaceContextProvider
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.putNullable
import org.jetbrains.bazel.sync_new.storage.hash.putUnordered
import kotlin.io.path.absolutePathString

internal object SyncConfig {

  // TODO: add extension point
  fun hashWorkspaceConfig(project: Project): HashValue128 = hash {
    val workspaceContext = ProjectViewWorkspaceContextProvider.getInstance(project)
      .readWorkspaceContext()

    putByte(1) // mark
    putNullable(workspaceContext.bazelBinary) { putString(it.absolutePathString()) }
    putUnordered(workspaceContext.buildFlags) { putString(it) }
    putUnordered(workspaceContext.syncFlags) { putString(it) }
    putUnordered(workspaceContext.enabledRules) { putString(it) }
  }
}
