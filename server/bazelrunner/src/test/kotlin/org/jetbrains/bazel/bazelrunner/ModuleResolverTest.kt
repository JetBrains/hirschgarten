package org.jetbrains.bazel.bazelrunner

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.commons.BazelStatus
import org.junit.jupiter.api.Test

class ModuleResolverTest {
  fun makeOutputCollector(lines: String): OutputCollector =
    OutputCollector().also {
      lines.lines().forEach(it::onNextLine)
    }

  val moduleOutputParser = ModuleOutputParser()

  @Test
  fun `should throw on failed show repo invocation`() {
    val stderr =
      "ERROR: In repo argument lll: Module lll does not exist in the dependency graph." +
        "(Note that unused modules cannot be used here). Type 'bazel help mod' for syntax and help."
    val result = BazelProcessResult(makeOutputCollector(""), makeOutputCollector(stderr), BazelStatus.BUILD_ERROR)

    val moduleOutputParser = ModuleOutputParser()

    shouldThrow<IllegalStateException> {
      moduleOutputParser.parseShowRepoResult(result)
    }.also {
      it.message shouldBe "Failed to resolve module from bazel info. Bazel Info output:\n'$stderr\n'"
    }
  }

  @Test
  fun `should correctly parse local_repository`() {
    val stdout =
      """
      ## @community:
      # <builtin>
      local_repository(
        name = "community~",
        path = "community",
      )
      # Rule community~ instantiated at (most recent call last):
      #   <builtin> in <toplevel>
      """.trimIndent()

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), BazelStatus.SUCCESS)

    val parsed = moduleOutputParser.parseShowRepoResult(result)
    parsed shouldBe ShowRepoResult.LocalRepository("community~", "community")
  }

  @Test
  fun `should correctly parse http_archive`() {
    val stdout =
      """
      ## rules_jvm_external@6.5:
      # <builtin>
      http_archive(
        name = "rules_jvm_external~",
        urls = ["https://github.com/bazel-contrib/rules_jvm_external/releases/download/6.5/rules_jvm_external-6.5.tar.gz"],
        integrity = "sha256-Ok1WNXhRz1sNrlOLPz4GEqT1iSXfs8rbLgxOh9UeYp4=",
        strip_prefix = "rules_jvm_external-6.5",
        remote_file_urls = {},
        remote_file_integrity = {},
        remote_patches = {},
        remote_patch_strip = 0,
      )
      # Rule rules_jvm_external~ instantiated at (most recent call last):
      #   <builtin> in <toplevel>
      # Rule http_archive defined at (most recent call last):
      #   /home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/39b3974c0c7bcab09c689dfd2d36f22b/external/bazel_tools/tools/build_defs/repo/http.bzl:387:31 in <toplevel>
      """.trimIndent()

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), BazelStatus.SUCCESS)

    val parsed = moduleOutputParser.parseShowRepoResult(result)

    parsed.shouldBeInstanceOf<ShowRepoResult.Unknown>()
    parsed.output shouldBe stdout + "\n"
    parsed.name shouldBe "rules_jvm_external~"
  }
}
