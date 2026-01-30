package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import kotlinx.coroutines.delay
import org.jetbrains.bazel.server.bsp.utils.DelimitedMessageReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.deleteExisting
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.milliseconds

class BepReader(val bepServer: BepServer) {
  val eventFile: Path =
    Files.createTempFile("bazel-bep-output", null).toAbsolutePath().also {
      it.setFilePermissions()
    }
  val serverPid = AtomicLong(0)

  private val bazelBuildFinished = AtomicBoolean(false)

  suspend fun start() {
    logger.info("Start listening to BEP events")
    try {
      eventFile.inputStream().buffered().use { inputStream ->
        readBepEvents(inputStream)
      }
      logger.info("BEP events listening finished")
    }
    finally {
      eventFile.deleteExisting()
    }
  }

  private suspend fun readBepEvents(inputStream: BufferedInputStream) {
    val reader =
        DelimitedMessageReader(
            inputStream,
            BuildEventStreamProtos.BuildEvent.parser(),
        )

    while(true) {
      val event: BuildEventStreamProtos.BuildEvent? = reader.nextMessage()
      if (event == null) {
        if (bazelBuildFinished.get())
          break

        delay(PollInterval)
        continue
      }

      bepServer.handleBuildEventStreamProtosEvent(event)
      setServerPid(event)
    }
  }

  private fun setServerPid(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasStarted()) {
      serverPid.compareAndSet(0, event.started.serverPid)
    }
  }

  fun finishBuild() {
    bazelBuildFinished.set(true)
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(BepReader::class.java)
    private val PollInterval = 10.milliseconds

    private fun Path.setFilePermissions() {
      with(toFile()) {
        setReadable(true, true)
        setWritable(true, true)
      }
    }
  }
}
