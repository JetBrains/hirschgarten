package org.jetbrains.bazel.bazelrunner

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.bazel.commons.orFallbackVersion
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
        .fromReleaseString("release 7.5.0")

    // then
    release?.major shouldBe 7
    release?.minor shouldBe 5
  }

  @Test
  fun `should handle new bazel unofficial`() {
    // given & when
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("release 7.6.0-pre20230102")

    // then
    release?.major shouldBe 7
    release?.minor shouldBe 6
  }

  @Test
  fun `should handle new bazel multi-digit version`() {
    // given & when
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("release 16.23.0")

    // then
    release?.major shouldBe 16
    release?.minor shouldBe 23
  }

  @Test
  fun `should fall back to last supported version in case of error`() {
    // given & when
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("debug test")
        .orFallbackVersion()

    // then
    release.major shouldBe 7
  }

  @Test
  fun `should correctly parse bazelversion`() {
    // given & when
    val path = copyBazelVersionToTmp()
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromBazelVersionFile(path.parent)

    // then
    release?.major shouldBe 8
    release?.minor shouldBe 3
  }

  private fun copyBazelVersionToTmp(): Path {
    val tempDir = createTempDirectory("workspace").createDirectories()
    val tempFile = tempDir.resolve(".bazelversion")
    tempFile.writeText("8.3.0")
    return tempFile
  }

  @Test
  fun `fallback version not deprecated`() {
    val fallbackForUnkownRelease =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("debug test")
        .orFallbackVersion()

    fallbackForUnkownRelease.deprecated() shouldBe null
  }

  @Test
  fun `very old version deprecated`() {

    val outdatedRelease =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("release 4.0.0")

    outdatedRelease!!.deprecated() shouldNotBe null
  }

 @Test
 fun `future release not deprecated`() {
   val futureRelease =
     org.jetbrains.bazel.commons.BazelRelease
       .fromReleaseString("release 99.0.0")

   futureRelease!!.deprecated() shouldBe null
 }

  @Test
  fun `Release 741 is deprecated`() {
    val release =
      org.jetbrains.bazel.commons.BazelRelease
        .fromReleaseString("release 7.4.1")

    release!!.deprecated() shouldNotBe null
  }
}
