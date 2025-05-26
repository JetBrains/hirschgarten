package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.UnindexedFilesScannerExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

suspend fun suspendScanningAndIndexingThenExecute(
  activityName: String,
  project: Project,
  activity: suspend () -> Unit,
) {
  coroutineScope {
    // Use Dispatchers.IO to wait for the blocking call
    withContext(Dispatchers.IO) {
      UnindexedFilesScannerExecutor.getInstance(project).suspendScanningAndIndexingThenRun(activityName) {
        runBlocking(coroutineContext) {
          activity()
        }
      }
    }
  }
}
