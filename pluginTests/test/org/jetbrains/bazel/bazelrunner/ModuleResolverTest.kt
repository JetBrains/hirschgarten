package org.jetbrains.bazel.bazelrunner

import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.commons.BazelStatus
import org.junit.jupiter.api.Test

class ModuleResolverTest {
  fun makeOutputCollector(lines: String): OutputCollector =
    OutputCollector().also {
      it.append(lines.toByteArray(Charsets.UTF_8))
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
      moduleOutputParser.parseShowRepoResults(result, false)
    }.also {
      it.message shouldBe "Failed to resolve module from bazel info. Bazel Info output:\n'$stderr'"
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

    val parsed = moduleOutputParser.parseShowRepoResults(result, false)
    parsed shouldBe mapOf("@community" to ShowRepoResult.LocalRepository("community~", "community"))
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

    val parsed =
      moduleOutputParser.parseShowRepoResults(result, false).get("rules_jvm_external@6.5") ?: fail("No entry produced for rules_jvm_external")

    parsed.shouldBeInstanceOf<ShowRepoResult.Unknown>()
    parsed.output shouldBe stdout + "\n"
    parsed.name shouldBe "rules_jvm_external~"
  }

  @Test
  fun `should correctly parse many repositories`() {
    val stdout = """
        ## community@_:
        # <builtin>
        local_repository(
          name = "community+",
          path = "community",
        )
        # Rule community+ instantiated at (most recent call last):
        #   <builtin> in <toplevel>
        ## rules_jvm@_:
        # <builtin>
        local_repository(
          name = "rules_jvm+",
          path = "community/build/jvm-rules",
        )
        # Rule rules_jvm+ instantiated at (most recent call last):
        #   <builtin> in <toplevel>
        ## lib@_:
        # <builtin>
        local_repository(
          name = "lib+",
          path = "community/lib",
        )
        # Rule lib+ instantiated at (most recent call last):
        #   <builtin> in <toplevel>
        ## ultimate_lib@_:
        # <builtin>
        local_repository(
          name = "ultimate_lib+",
          path = "lib",
        )
        # Rule ultimate_lib+ instantiated at (most recent call last):
        #   <builtin> in <toplevel>
        ## jps_to_bazel@_:
        # <builtin>
        local_repository(
          name = "jps_to_bazel+",
          path = "community/platform/build-scripts/bazel",
        )
        # Rule jps_to_bazel+ instantiated at (most recent call last):
        #   <builtin> in <toplevel>
    """.trimIndent()

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), BazelStatus.SUCCESS)

    val parsed = moduleOutputParser.parseShowRepoResults(result, false)
    parsed shouldBe mapOf(
      "community@_" to ShowRepoResult.LocalRepository("community+", "community"),
      "rules_jvm@_" to ShowRepoResult.LocalRepository("rules_jvm+", "community/build/jvm-rules"),
      "lib@_" to ShowRepoResult.LocalRepository("lib+", "community/lib"),
      "ultimate_lib@_" to ShowRepoResult.LocalRepository("ultimate_lib+", "lib"),
      "jps_to_bazel@_" to ShowRepoResult.LocalRepository("jps_to_bazel+", "community/platform/build-scripts/bazel"),
    )
  }

  @Test
  fun `should parse json correctly`() {
     val stdout = """
      {"canonicalName":"bundled+","repoRuleName":"local_repository","repoRuleBzlLabel":"@@bazel_tools//tools/build_defs/repo:local.bzl","moduleKey":"bundled@_","attribute":[{"name":"path","type":"STRING","stringValue":"subproject","explicitlySpecified":true,"nodep":false}]}
      {"canonicalName":"anotherbundled+","repoRuleName":"local_repository","repoRuleBzlLabel":"@@bazel_tools//tools/build_defs/repo:local.bzl","moduleKey":"anotherbundled@_","attribute":[{"name":"path","type":"STRING","stringValue":"the/other/subproject","explicitlySpecified":true,"nodep":false}]}
     """.trimIndent()

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), BazelStatus.SUCCESS)

    val parsed = moduleOutputParser.parseShowRepoResults(result, true )

    parsed shouldBe mapOf(
      "bundled@_" to ShowRepoResult.LocalRepository("bundled+", "subproject"),
      "anotherbundled@_" to ShowRepoResult.LocalRepository("anotherbundled+", "the/other/subproject"),
    )
  }

  @Test
  fun `json parser should handle non-local repositories correctly`() {
   val stdout = """
     {"canonicalName":"bundled+","repoRuleName":"local_repository","repoRuleBzlLabel":"@@bazel_tools//tools/build_defs/repo:local.bzl","moduleKey":"bundled@_","attribute":[{"name":"path","type":"STRING","stringValue":"subproject","explicitlySpecified":true,"nodep":false}]}
     {"canonicalName":"rules_kotlin+","repoRuleName":"http_archive","repoRuleBzlLabel":"@@bazel_tools//tools/build_defs/repo:http.bzl","moduleKey":"rules_kotlin@2.2.2","attribute":[{"name":"url","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false},{"name":"urls","type":"STRING_LIST","stringListValue":["https://github.com/bazelbuild/rules_kotlin/releases/download/v2.2.2/rules_kotlin-v2.2.2.tar.gz"],"explicitlySpecified":true,"nodep":false},{"name":"sha256","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false},{"name":"integrity","type":"STRING","stringValue":"sha256-QR2yavs0ksyDUbW1NJkxUir+LFTyZRttEncwoSVtD2A\u003d","explicitlySpecified":true,"nodep":false},{"name":"netrc","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false},{"name":"auth_patterns","type":"STRING_DICT","explicitlySpecified":false},{"name":"canonical_id","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false},{"name":"strip_prefix","type":"STRING","stringValue":"","explicitlySpecified":true,"nodep":false},{"name":"add_prefix","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false},{"name":"files","type":"LABEL_DICT_UNARY","explicitlySpecified":false,"nodep":false},{"name":"type","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false},{"name":"patches","type":"LABEL_LIST","explicitlySpecified":false,"nodep":false},{"name":"remote_file_urls","type":"STRING_LIST_DICT","explicitlySpecified":true},{"name":"remote_file_integrity","type":"STRING_DICT","explicitlySpecified":true},{"name":"remote_module_file_urls","type":"STRING_LIST","stringListValue":["https://bcr.bazel.build/modules/rules_kotlin/2.2.2/MODULE.bazel"],"explicitlySpecified":true,"nodep":false},{"name":"remote_module_file_integrity","type":"STRING","stringValue":"sha256-ANOcXg+njNhhk5RiZbuEnnh4wk5EJg+VJRCEKIUrMVw\u003d","explicitlySpecified":true,"nodep":false},{"name":"remote_patches","type":"STRING_DICT","stringDictValue":[{"key":"https://bcr.bazel.build/modules/rules_kotlin/2.2.2/patches/module_dot_bazel_version.patch","value":"sha256-THY5AnXd72H8scfX2na73BV+pShJYYTM9WkD+eC3Ad4\u003d"}],"explicitlySpecified":true},{"name":"remote_patch_strip","type":"INTEGER","intValue":1,"explicitlySpecified":true},{"name":"patch_tool","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false},{"name":"patch_args","type":"STRING_LIST","explicitlySpecified":false,"nodep":false},{"name":"patch_strip","type":"INTEGER","intValue":0,"explicitlySpecified":false},{"name":"patch_cmds","type":"STRING_LIST","explicitlySpecified":false,"nodep":false},{"name":"patch_cmds_win","type":"STRING_LIST","explicitlySpecified":false,"nodep":false},{"name":"build_file","type":"LABEL","explicitlySpecified":false,"nodep":false},{"name":"build_file_content","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false},{"name":"workspace_file","type":"LABEL","explicitlySpecified":false,"nodep":false},{"name":"workspace_file_content","type":"STRING","stringValue":"","explicitlySpecified":false,"nodep":false}]}
   """.trimIndent()

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), BazelStatus.SUCCESS)

    val parsed = moduleOutputParser.parseShowRepoResults(result, true )

    val rules_kotlin = parsed.get("rules_kotlin@2.2.2") ?: error("Entry for rules_kotlin@2.2.2 is missing")
    rules_kotlin.shouldBeInstanceOf<ShowRepoResult.Unknown>()

    parsed shouldBe mapOf(
      "bundled@_" to ShowRepoResult.LocalRepository("bundled+", "subproject"),
      "rules_kotlin@2.2.2" to rules_kotlin,
    )

  }

  @Test
  fun `json parser should accept empty lines in both line endings supported by NDJSON`() {
    val stdout = """
      {"canonicalName":"bundled+","repoRuleName":"local_repository","moduleKey":"bundled@_","attribute":[{"name":"path","type":"STRING","stringValue":"subproject"}]}
    """.trimIndent() + "\n\r\n\n"

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), BazelStatus.SUCCESS)

    val parsed = moduleOutputParser.parseShowRepoResults(result, true )

    parsed shouldBe mapOf(
      "bundled@_" to ShowRepoResult.LocalRepository("bundled+", "subproject"),
    )
  }
}
