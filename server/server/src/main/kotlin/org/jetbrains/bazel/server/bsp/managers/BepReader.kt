package org.jetbrains.bazel.server.bsp.managers

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.server.bep.BepServer
import org.jetbrains.bazel.server.bsp.utils.DelimitedMessageReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.deleteExisting

class BepReader(private val bepServer: BepServer) {
  val eventFile =
    Files.createTempFile("bazel-bep-output", null).toFile().also {
      it.setFilePermissions()
    }
  val serverPid = CompletableFuture<Long>()

  private val bazelBuildFinished: AtomicBoolean = AtomicBoolean(false)
  private val logger: Logger = LoggerFactory.getLogger(BepReader::class.java)

  private fun File.setFilePermissions() {
    setReadable(true, true)
    setWritable(true, true)
  }

  suspend fun start() =
    withContext(Dispatchers.Default) {
      logger.info("Start listening to BEP events")
      val inputStream =
        withContext(Dispatchers.IO) {
          eventFile.inputStream().buffered()
        }
      try {
        readBepEvents(inputStream)
        logger.info("BEP events listening finished")
      } finally {
        try {
          inputStream.close()
        } finally {
          eventFile.toPath().deleteExisting()
        }
      }
    }

  private suspend fun readBepEvents(inputStream: BufferedInputStream) {
    val reader =
      DelimitedMessageReader(
        inputStream,
        BuildEventStreamProtos.BuildEvent.parser(),
      )
    var event: BuildEventStreamProtos.BuildEvent? = null

    suspend fun readEvent(): BuildEventStreamProtos.BuildEvent? {
      event = reader.nextMessage()
      return event
    }

    // It's important not to use the short-circuited ||, so that the events are parsed
    // every time the loop condition is checked
    while (!bazelBuildFinished.get() or (readEvent() != null)) {
      val event = event
      if (event != null) {
        bepServer.handleBuildEventStreamProtosEvent(event)
        setServerPid(event)
      } else {
        delay(50)
      }
    }
  }

  private fun setServerPid(event: BuildEventStreamProtos.BuildEvent) {
    if (event.hasStarted() && !serverPid.isDone) {
      serverPid.complete(event.started.serverPid)
    }
  }

  fun finishBuild() {
    bazelBuildFinished.set(true)
  }
}
