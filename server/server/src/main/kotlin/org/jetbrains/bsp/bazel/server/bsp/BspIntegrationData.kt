package org.jetbrains.bsp.bazel.server.bsp

import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.concurrent.ExecutorService

data class BspIntegrationData(
  val stdout: OutputStream,
  val stdin: InputStream,
  val executor: ExecutorService,
  val traceWriter: PrintWriter?,
)
