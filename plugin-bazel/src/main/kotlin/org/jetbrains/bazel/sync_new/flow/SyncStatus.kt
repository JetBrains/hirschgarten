package org.jetbrains.bazel.sync_new.flow

sealed interface SyncStatus {
  object Success : SyncStatus
  object PartialFailure : SyncStatus
  object Failure : SyncStatus
}
