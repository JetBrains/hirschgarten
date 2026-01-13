package org.jetbrains.bazel.sync_new.flow

sealed interface SyncStatus {
  object Success : SyncStatus
  object PartialFailure : SyncStatus
  object Failure : SyncStatus

  companion object {
    fun reduce(lhs: SyncStatus, rhs: SyncStatus): SyncStatus {
      if (lhs is PartialFailure || rhs is PartialFailure) {
        return PartialFailure
      }
      if (lhs is Failure || rhs is Failure) {
        return Failure
      }
      return lhs
    }
  }
}
