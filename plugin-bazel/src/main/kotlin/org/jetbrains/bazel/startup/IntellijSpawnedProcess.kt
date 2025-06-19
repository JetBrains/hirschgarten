package org.jetbrains.bazel.startup

import com.intellij.util.io.awaitExit
import org.jetbrains.bazel.bazelrunner.outputs.SpawnedProcess
import java.io.InputStream
import java.util.concurrent.TimeUnit

@JvmInline
value class IntellijSpawnedProcess(internal val process: Process) : SpawnedProcess {
  override val inputStream: InputStream get() = process.inputStream

  override val errorStream: InputStream get() = process.errorStream

  override val pid: Long
    get() = process.pid()

  override val isAlive: Boolean get() = process.isAlive

  override fun waitFor(timeout: Int, unit: TimeUnit): Boolean = process.waitFor(timeout.toLong(), unit)

  override fun waitFor(): Int = process.waitFor()

  override fun destroy(): Unit = process.destroy()

  override suspend fun awaitExit(): Int = process.awaitExit()
}
