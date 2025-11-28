package org.jetbrains.bazel.bazelrunner

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.orLatestSupported
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

class BazelReleaseTest {
  @Test
  fun `should handle old bazel`() {
    // given & when
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("release 4.0.0")

    // then
    release?.major shouldBe 4
  }

  @Test
  fun `should handle new bazel`() {
    // given & when
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("release 6.0.0")

    // then
    release?.major shouldBe 6
  }

  @Test
  fun `should handle new bazel unofficial`() {
    // given & when
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("release 6.0.0-pre20230102")

    // then
    release?.major shouldBe 6
  }

  @Test
  fun `should handle new bazel multi-digit version`() {
    // given & when
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("release 16.0.0")

    // then
    release?.major shouldBe 16
  }

  @Test
  fun `should fall back to last supported version in case of error`() {
    // given & when
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("debug test")
        .orLatestSupported()

    // then
    release.major shouldBe 6
  }

  @Test
  fun `should correctly parse bazelversion`() {
    // given & when
    val path = copyBazelVersionToTmp()
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromBazelVersionFile(path.parent)

    // then
    release?.major shouldBe 6
  }

  private fun copyBazelVersionToTmp(): Path {
    val tempDir = createTempDirectory("workspace").createDirectories()
    val tempFile = tempDir.resolve(".bazelversion")
    tempFile.writeText("6.3.0")
    return tempFile
  }
}
