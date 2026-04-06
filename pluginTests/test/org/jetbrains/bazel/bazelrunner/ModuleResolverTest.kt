package org.jetbrains.bazel.bazelrunner

import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.outputs.OutputCollector
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.sync.workspace.projectTree.BazelRunnerSpyStubbingHelper
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.TaskGroupId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import java.nio.file.Path

class ModuleResolverTest {
  fun makeOutputCollector(lines: String): OutputCollector =
    OutputCollector().also {
      it.append(lines.toByteArray(Charsets.UTF_8))
    }

  val moduleOutputParser = ModuleOutputParser()
  private val tempDir = Path.of(System.getProperty("java.io.tmpdir"))
  private val taskId = TaskGroupId.EMPTY.task("module-resolver-test")
  private val workspaceContext =
    WorkspaceContext(
      targets = emptyList(),
      directories = emptyList(),
      buildFlags = emptyList(),
      syncFlags = emptyList(),
      debugFlags = emptyList(),
      bazelBinary = null,
      allowManualTargetsSync = false,
      importDepth = 0,
      enabledRules = emptyList(),
      ideJavaHomeOverride = null,
      shardSync = false,
      targetShardSize = 0,
      shardingApproach = null,
      importRunConfigurations = emptyList(),
      gazelleTarget = null,
      indexAllFilesInDirectories = false,
      pythonCodeGeneratorRuleNames = emptyList(),
      importIjars = false,
      deriveInstrumentationFilterFromTargets = false,
      indexAdditionalFilesInDirectories = emptyList(),
      preferClassJarsOverSourcelessJars = false,
    )

  private fun makeResult(stdout: String = "", stderr: String = "", exitCode: Int = 0): BazelProcessResult =
    BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(stderr), exitCode)

  private fun bazelInfo(workspaceEnabled: Boolean = true): BazelInfo =
    BazelInfo(
      execRoot = tempDir,
      outputBase = tempDir,
      workspaceRoot = tempDir,
      bazelBin = tempDir,
      release = BazelRelease(7, 6),
      isBzlModEnabled = true,
      isWorkspaceEnabled = workspaceEnabled,
      externalAutoloads = emptyList(),
    )

  private fun createResolver(vararg processResults: BazelProcessResult): ModuleResolver {
    val runner = spy(BazelRunner(null, tempDir))
    val process = mock(BazelProcess::class.java)

    runBlocking {
      `when`(process.waitAndGetResult()).thenReturn(*processResults)
    }

    BazelRunnerSpyStubbingHelper.stubRunBazelCommand(runner, process)
    return ModuleResolver(runner, workspaceContext, taskId)
  }

  private fun localRepositoryStanza(repoArgument: String, repoName: String, path: String): String =
    """
      ## $repoArgument:
      # <builtin>
      local_repository(
        name = "$repoName",
        path = "$path",
      )
    """.trimIndent()

  private fun httpArchiveStanza(repoArgument: String, repoName: String, url: String): String =
    """
      ## $repoArgument:
      # <builtin>
      http_archive(
        name = "$repoName",
        urls = ["$url"],
      )
    """.trimIndent()

  @Test
  fun `should throw on failed show repo invocation`() {
    val stderr =
      "ERROR: In repo argument lll: Module lll does not exist in the dependency graph." +
      "(Note that unused modules cannot be used here). Type 'bazel help mod' for syntax and help."
    val result = BazelProcessResult(makeOutputCollector(""), makeOutputCollector(stderr), 1)

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

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), 0)

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

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), 0)

    val parsed =
      moduleOutputParser.parseShowRepoResults(result, false).get("rules_jvm_external@6.5") ?: fail("No entry produced for rules_jvm_external")

    parsed.shouldBeInstanceOf<ShowRepoResult.HttpArchiveRepository>()
    (parsed as ShowRepoResult.HttpArchiveRepository).urls shouldBe listOf("https://github.com/bazel-contrib/rules_jvm_external/releases/download/6.5/rules_jvm_external-6.5.tar.gz")
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

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), 0)

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

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), 0)

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

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), 0)

    val parsed = moduleOutputParser.parseShowRepoResults(result, true )

    parsed shouldBe mapOf(
      "bundled@_" to ShowRepoResult.LocalRepository("bundled+", "subproject"),
      "rules_kotlin@2.2.2" to ShowRepoResult.HttpArchiveRepository("rules_kotlin+", listOf("https://github.com/bazelbuild/rules_kotlin/releases/download/v2.2.2/rules_kotlin-v2.2.2.tar.gz"))
    )

  }

  @Test
  fun `should correctly extract bad repo from error message`() {
    val stderr =
      "ERROR: In repo argument maven: Module maven does not exist in the dependency graph." +
      "(Note that unused modules cannot be used here). Type 'bazel help mod' for syntax and help."
    val result = BazelProcessResult(makeOutputCollector(""), makeOutputCollector(stderr), 2)

    val badRepo = ModuleResolver.extractBadRepoFromError(result)
    badRepo shouldBe "maven"
  }

  @Test
  fun `should return null when error message has no repo argument`() {
    val stderr = "ERROR: some unrelated bazel error"
    val result = BazelProcessResult(makeOutputCollector(""), makeOutputCollector(stderr), 1)

    val badRepo = ModuleResolver.extractBadRepoFromError(result)
    badRepo shouldBe null
  }

  @Test
  fun `should extract repo with @@ prefix from error`() {
    val stderr =
      "ERROR: In repo argument @@some_workspace_repo: Module @@some_workspace_repo does not exist in the dependency graph." +
      "(Note that unused modules cannot be used here). Type 'bazel help mod' for syntax and help."
    val result = BazelProcessResult(makeOutputCollector(""), makeOutputCollector(stderr), 2)

    val badRepo = ModuleResolver.extractBadRepoFromError(result)
    badRepo shouldBe "@@some_workspace_repo"
  }

  @Test
  fun `should ignore non retryable repo argument errors`() {
    val stderr =
      "ERROR: In repo argument @rules_python+: invalid argument '@rules_python+': invalid user-provided repo name 'rules_python+'"
    val result = BazelProcessResult(makeOutputCollector(""), makeOutputCollector(stderr), 2)

    val badRepo = ModuleResolver.extractBadRepoFromError(result)
    badRepo shouldBe null
  }

  @Test
  fun `resolveModules should keep successful repos while peeling retryable failures`() {
    val batchFailure =
      makeResult(
        stderr = "ERROR: In repo argument @@bad_repo: no such repo. Type 'bazel help mod' for syntax and help.",
        exitCode = 2,
      )
    val batchSuccess =
      makeResult(
        stdout =
          listOf(
            httpArchiveStanza("@@good_http", "good_http+", "https://example.com/good_http.tar.gz"),
            localRepositoryStanza("@@good_local", "good_local+", "good/local"),
          ).joinToString("\n"),
      )
    val resolver = createResolver(batchFailure, batchSuccess)

    val resolved = runBlocking {
      resolver.resolveModules(listOf("@@good_local", "@@bad_repo", "@@good_http"), bazelInfo())
    }

    resolved shouldBe mapOf(
      "@@good_http" to ShowRepoResult.HttpArchiveRepository("good_http+", listOf("https://example.com/good_http.tar.gz")),
      "@@good_local" to ShowRepoResult.LocalRepository("good_local+", "good/local"),
      "@@bad_repo" to null,
    )
  }

  @Test
  fun `resolveModules should fall back to individual lookups when batch failure is not retryable`() {
    val batchFailure = makeResult(stderr = "ERROR: some unrelated bazel error", exitCode = 2)
    val firstSingleSuccess =
      makeResult(
        stdout = httpArchiveStanza("@@good_http", "good_http+", "https://example.com/good_http.tar.gz"),
      )
    val secondSingleSuccess =
      makeResult(
        stdout = localRepositoryStanza("@@good_local", "good_local+", "good/local"),
      )
    val resolver = createResolver(batchFailure, firstSingleSuccess, secondSingleSuccess)

    val resolved = runBlocking {
      resolver.resolveModules(listOf("@@good_local", "@@good_http"), bazelInfo())
    }

    resolved shouldBe mapOf(
      "@@good_http" to ShowRepoResult.HttpArchiveRepository("good_http+", listOf("https://example.com/good_http.tar.gz")),
      "@@good_local" to ShowRepoResult.LocalRepository("good_local+", "good/local"),
    )
  }

  @Test
  fun `resolveModules should peel multiple retryable failures before succeeding`() {
    val firstBatchFailure =
      makeResult(
        stderr = "ERROR: In repo argument @@bad_a: no such repo. Type 'bazel help mod' for syntax and help.",
        exitCode = 2,
      )
    val secondBatchFailure =
      makeResult(
        stderr =
          "ERROR: In repo argument @@bad_b: Module @@bad_b does not exist in the dependency graph. " +
            "(Note that unused modules cannot be used here). Type 'bazel help mod' for syntax and help.",
        exitCode = 2,
      )
    val finalSuccess =
      makeResult(
        stdout = localRepositoryStanza("@@good_local", "good_local+", "good/local"),
      )
    val resolver = createResolver(firstBatchFailure, secondBatchFailure, finalSuccess)

    val resolved = runBlocking {
      resolver.resolveModules(listOf("@@good_local", "@@bad_b", "@@bad_a"), bazelInfo())
    }

    resolved shouldBe mapOf(
      "@@good_local" to ShowRepoResult.LocalRepository("good_local+", "good/local"),
      "@@bad_a" to null,
      "@@bad_b" to null,
    )
  }

  @Test
  fun `resolveModules should return null for single retryable failure`() {
    val singleFailure =
      makeResult(
        stderr = "ERROR: In repo argument @@bad_repo: no such repo. Type 'bazel help mod' for syntax and help.",
        exitCode = 2,
      )
    val resolver = createResolver(singleFailure)

    val resolved = runBlocking {
      resolver.resolveModules(listOf("@@bad_repo"), bazelInfo())
    }

    resolved shouldBe mapOf("@@bad_repo" to null)
  }

  @Test
  fun `json parser should accept empty lines in both line endings supported by NDJSON`() {
    val stdout = """
      {"canonicalName":"bundled+","repoRuleName":"local_repository","moduleKey":"bundled@_","attribute":[{"name":"path","type":"STRING","stringValue":"subproject"}]}
    """.trimIndent() + "\n\r\n\n"

    val result = BazelProcessResult(makeOutputCollector(stdout), makeOutputCollector(""), 0)

    val parsed = moduleOutputParser.parseShowRepoResults(result, true )

    parsed shouldBe mapOf(
      "bundled@_" to ShowRepoResult.LocalRepository("bundled+", "subproject"),
    )
  }
}
