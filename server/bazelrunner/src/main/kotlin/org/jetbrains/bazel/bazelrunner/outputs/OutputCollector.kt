package org.jetbrains.bazel.bazelrunner.outputs

import java.io.ByteArrayOutputStream

class OutputCollector {
  private val raw = ByteArrayOutputStream()

  fun append(byte: Int) {
    raw.write(byte)
  }

  fun append(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
    raw.write(bytes, offset, length)
  }

  fun raw(): ByteArray = raw.toByteArray()
  fun lines(): List<String> = raw().toString(Charsets.UTF_8).lines()
}
