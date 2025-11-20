package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.ThreadingAssertions

@Service(Service.Level.PROJECT)
class SyncService(
  private val project: Project
) {
  suspend fun sync(scope: SyncScope) {
    ThreadingAssertions.assertBackgroundThread()
  }
}
