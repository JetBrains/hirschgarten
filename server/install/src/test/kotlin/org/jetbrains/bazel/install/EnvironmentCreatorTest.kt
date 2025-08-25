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
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class EnvironmentCreatorTest {
  @Nested
  @DisplayName("environmentCreator.create tests")
  inner class CopyAspectsTest {
    private lateinit var tempRoot: Path

    private val aspectFolders =
      listOf(
        "aspects/rules",
        "aspects/utils",
      )

    private val aspectFiles =
      listOf(
        "aspects/core.bzl.template",
        "aspects/utils/utils.bzl.template",
        "aspects/rules/java/java_info.bzl.template",
        "aspects/rules/jvm/jvm_info.bzl.template",
        "aspects/rules/kt/kt_info.bzl.template",
        "aspects/rules/python/python_info.bzl.template",
        "aspects/rules/scala/scala_info.bzl.template",
        "aspects/rules/cpp/cpp_info.bzl",
      )

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
      aspectFolders.forEach { dotBazelBsp.resolve(it).isDirectory() shouldBeEqual true }
      aspectFiles.forEach { dotBazelBsp.resolve(it).isRegularFile() shouldBeEqual true }
    }

    @Test
    fun `should not re-copy files if the content is the same when calling EnvironmentCreator#create`() {
      // when
      var dotBazelBsp = EnvironmentCreator(tempRoot).create()

      val lastModifiedTimes = aspectFiles.map { dotBazelBsp.resolve(it).getLastModifiedTime() }

      // try calling `create` again
      dotBazelBsp = EnvironmentCreator(tempRoot).create()

      // then
      lastModifiedTimes shouldContainExactly aspectFiles.map { dotBazelBsp.resolve(it).getLastModifiedTime() }
    }
  }
}
