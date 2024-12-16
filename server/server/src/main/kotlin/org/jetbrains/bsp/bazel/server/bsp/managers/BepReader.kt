package org.jetbrains.bsp.bazel.server.bsp.managers

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.bsp.bazel.server.bep.BepServer
import org.jetbrains.bsp.bazel.server.bsp.utils.DelimitedMessageReader
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class BepReader(private val bepServer: BepServer) {
  val eventFile =
    Files.createTempFile("bazel-bep-output", null).toFile().also {
      it.setFilePermissions()
      it.deleteOnExit()
    }
  val serverPid = CompletableFuture<Long>()

  private val bazelBuildFinished: AtomicBoolean = AtomicBoolean(false)
  private val logger: Logger = LogManager.getLogger(BepReader::class.java)

  private fun File.setFilePermissions() {
    setReadable(true, true)
    setWritable(true, true)
  }

  suspend fun start() =
    withContext(Dispatchers.Default) {
      logger.info("Start listening to BEP events")
      val reader =
        DelimitedMessageReader(
          FileInputStream(eventFile),
          BuildEventStreamProtos.BuildEvent.parser(),
        )
      var event: BuildEventStreamProtos.BuildEvent? = null
      do {
        event = reader.nextMessage()
        if (event != null) {
          bepServer.handleBuildEventStreamProtosEvent(event)
          setServerPid(event)
        }
      } while (!bazelBuildFinished.get() || event != null)
      logger.info("BEP events listening finished")
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
