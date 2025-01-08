package org.jetbrains.bsp.bazel.commons

import java.time.Duration

class Stopwatch private constructor() {
  private val start: Long = now()

  fun stop(): Duration = Duration.ofMillis(now() - start)

  private fun now(): Long = System.currentTimeMillis()

  companion object {
    fun start(): Stopwatch = Stopwatch()
  }
}
