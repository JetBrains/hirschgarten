package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.SaveAndSyncHandler
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.UnindexedFilesScannerExecutor
import com.intellij.openapi.util.NlsContexts
import kotlinx.coroutines.CoroutineScope

suspend fun suspendScanningAndIndexingThenExecute(
  @NlsContexts.ProgressText activityName: String,
  project: Project,
  activity: suspend CoroutineScope.() -> Unit,
) {
  UnindexedFilesScannerExecutor.getInstance(project).suspendScanningAndIndexingThenExecute(activityName) {
    serviceAsync<SaveAndSyncHandler>().disableAutoSave().use {
      activity()
    }
  }
}
