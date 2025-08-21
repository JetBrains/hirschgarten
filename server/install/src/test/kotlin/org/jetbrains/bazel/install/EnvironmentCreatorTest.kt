package org.jetbrains.bazel.install

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory

class EnvironmentCreatorTest {
  @Nested
  @DisplayName("environmentCreator.create tests")
  inner class CopyAspectsTest {
    private lateinit var tempRoot: Path

    @BeforeEach
    fun beforeEach() {
      tempRoot = createTempDirectory("test-workspace-root")
      tempRoot.toFile().deleteOnExit()
    }

    @Test
    fun `should copy aspects from resources to dot bazelbsp directory`() {
      // when
      val dotBazelBsp = EnvironmentCreator(tempRoot).create()

      // then
      dotBazelBsp shouldNotBe null
      dotBazelBsp.resolve("aspects/core.bzl.template").exists() shouldBeEqual true
      dotBazelBsp.resolve("aspects/rules").isDirectory() shouldBeEqual true
      dotBazelBsp.resolve("aspects/utils").isDirectory() shouldBeEqual true
      dotBazelBsp.resolve("aspects/utils/utils.bzl.template").exists() shouldBeEqual true
      dotBazelBsp.resolve("aspects/rules/java/java_info.bzl.template").exists() shouldBeEqual true
      dotBazelBsp.resolve("aspects/rules/jvm/jvm_info.bzl.template").exists() shouldBeEqual true
      dotBazelBsp.resolve("aspects/rules/kt/kt_info.bzl.template").exists() shouldBeEqual true
      dotBazelBsp.resolve("aspects/rules/python/python_info.bzl.template").exists() shouldBeEqual true
      dotBazelBsp.resolve("aspects/rules/scala/scala_info.bzl.template").exists() shouldBeEqual true
      dotBazelBsp.resolve("aspects/rules/cpp/cpp_info.bzl").exists() shouldBeEqual true
    }

    @Test
    fun `should not re-copy files if the content is the same when calling EnvironmentCreator#create`() {
      // when
      var dotBazelBsp = EnvironmentCreator(tempRoot).create()

      val filesToTest =
        listOf(
          ".gitignore",
          "aspects/core.bzl.template",
          "aspects/utils/utils.bzl.template",
          "aspects/rules/java/java_info.bzl.template",
          "aspects/rules/jvm/jvm_info.bzl.template",
          "aspects/rules/kt/kt_info.bzl.template",
        )
      val lastModifiedTimes = filesToTest.map { dotBazelBsp.resolve(it).getLastModifiedTime() }

      // try calling `create` again
      dotBazelBsp = EnvironmentCreator(tempRoot).create()

      // then
      lastModifiedTimes shouldContainExactly filesToTest.map { dotBazelBsp.resolve(it).getLastModifiedTime() }
    }
  }
}
