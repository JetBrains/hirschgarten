package org.jetbrains.bazel.bazelisk

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.writeText

class BazeliskChecksumTest {
  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `accepts a file with the expected checksum`() {
    val file = tempDir.resolve("bazelisk").apply { writeText("hello") }
    assertDoesNotThrow {
      checkFileSha256Checksum(file, HELLO_SHA256)
    }
    assertDoesNotThrow {
      checkFileSha256Checksum(file, HELLO_SHA256.uppercase())
    }
  }

  @Test
  fun `rejects a file with a different checksum`() {
    val file = tempDir.resolve("bazelisk").apply { writeText("evil") }
    assertThrows<IOException> {
      checkFileSha256Checksum(file, HELLO_SHA256)
    }
  }

  private companion object {
    const val HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
  }
}
