package org.jetbrains.bazel.fastbuild

import com.intellij.util.messages.Topic

interface FastBuildStatusListener {
  fun fastBuildStarted()

  fun fastBuildFinished(fastBuildStatus: FastBuildStatus)

  fun fastBuildTargetStarted()

  fun fastBuildTargetFinished(fastBuildTargetStatus: FastBuildTargetStatus)

  companion object {
    val TOPIC: Topic<FastBuildStatusListener> = Topic(FastBuildStatusListener::class.java)
  }
}
