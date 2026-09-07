package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.bazel.server.bsp.utils.awaitDelimitedMessageBytes
import java.io.BufferedInputStream
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class BepReader(val bepServer: BepServer, val eventFile: Path) {
  val serverPid = AtomicLong(0)

  private val bazelBuildFinished = AtomicBoolean(false)

  suspend fun start() {
    logger.info("Start listening to BEP events")
    eventFile.setFilePermissions()
    eventFile.inputStream().buffered().use { inputStream ->
      readBepEvents(inputStream)
    }
    logger.info("BEP events listening finished")
  }

  private suspend fun readBepEvents(inputStream: BufferedInputStream) {
    val parser = BuildEventStreamProtos.BuildEvent.parser()
    while (true) {
      val eventBytes = inputStream.awaitDelimitedMessageBytes(Timeout, PollInterval) { bazelBuildFinished.get() } ?: break
      val event = parser.parsePartialFrom(eventBytes)
      bepServer.handleBuildEventStreamProtosEvent(event)
      setServerPid(event)
    }
  }

  private fun setServerPid(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasStarted()) {
      serverPid.compareAndSet(0, event.started.serverPid)
    }
  }

  fun bazelBuildFinished() {
    bazelBuildFinished.set(true)
  }

  companion object {
    private val logger = logger<BepReader>()
    private val PollInterval = 10.milliseconds
    private val Timeout = 30.seconds

    private fun Path.setFilePermissions() {
      with(toFile()) {
        setReadable(true, true)
        setWritable(true, true)
      }
    }
  }
}
