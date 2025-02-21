package org.jetbrains.bazel.sync.status

import com.intellij.util.messages.Topic

interface SyncStatusListener {
  fun syncStarted()

  fun syncFinished(canceled: Boolean)

  fun allTasksCancelled() {}

  companion object {
    val TOPIC: Topic<SyncStatusListener> = Topic(SyncStatusListener::class.java)
  }
}
