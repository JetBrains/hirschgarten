package org.jetbrains.bazel.sync_new.index

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class SyncIndexService(
  private val project: Project
) {
}
