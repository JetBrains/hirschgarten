package org.jetbrains.bazel.bazelrunner.outputs

import org.jetbrains.annotations.ApiStatus
import java.io.ByteArrayOutputStream

@ApiStatus.Internal
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
