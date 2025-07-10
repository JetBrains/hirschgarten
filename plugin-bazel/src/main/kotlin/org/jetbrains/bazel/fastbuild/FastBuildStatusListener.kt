package org.jetbrains.bazel.fastbuild

import com.intellij.util.messages.Topic

interface FastBuildStatusListener {
  fun fastBuildStarted(status: FastBuildStatus)

  fun fastBuildFinished(status: FastBuildStatus)

  companion object {
    val TOPIC: Topic<FastBuildStatusListener> = Topic(FastBuildStatusListener::class.java)
  }
}
