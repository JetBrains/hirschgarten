package org.jetbrains.bazel.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.writeText

class ExecutableInfoTest {
  @Test
  fun `parses a regular go_binary run script`() {
    val info = ExecutableInfo.fromBazelRunScript(BINARY_SCRIPT)
    info shouldBe ExecutableInfo(
      binary = Path("$EXEC_ROOT/$BIN/foo/foo_/foo"),
      workingDir = Path("$EXEC_ROOT/$BIN/foo/foo_/foo.runfiles/_main"),
      args = emptyList(),
      envVars = mapOf(
        "BUILD_EXECROOT" to EXEC_ROOT,
        "BUILD_ID" to "build-id",
        "BUILD_WORKING_DIRECTORY" to "/workspace",
        "BUILD_WORKSPACE_DIRECTORY" to "/workspace",
      ),
    )
  }

  @Test
  fun `parses a go_test run script and applies the test-setup environment`() {
    val info = ExecutableInfo.fromBazelRunScript(TEST_SCRIPT)
    val runfiles = Path("$EXEC_ROOT/$RUNFILES").toString()
    val tmpDir = Path("$EXEC_ROOT/_tmp/tmpdir")
    val runfilesRoot = Path("$EXEC_ROOT/$RUNFILES/_main")
    info shouldBe ExecutableInfo(
      binary = runfilesRoot.resolve("foo/foo_test_/foo_test"),
      workingDir = runfilesRoot,
      args = emptyList(),
      envVars = mapOf(
        "BAZEL_TEST" to "1",
        "BUILD_EXECROOT" to EXEC_ROOT,
        "BUILD_ID" to "build-id",
        "BUILD_WORKING_DIRECTORY" to "/workspace",
        "BUILD_WORKSPACE_DIRECTORY" to "/workspace",
        "GO_TEST_RUN_FROM_BAZEL" to "1",
        "GTEST_TMP_DIR" to tmpDir.toString(),
        "JAVA_RUNFILES" to runfiles,
        "PATH" to ".:/bin:/usr/bin:/usr/local/bin",
        "PYTHON_RUNFILES" to runfiles,
        "RUNFILES_DIR" to runfiles,
        "RUN_UNDER_RUNFILES" to "1",
        "TEST_BINARY" to "foo/foo_test_/foo_test",
        "TEST_SIZE" to "medium",
        "TEST_SRCDIR" to runfiles,
        "TEST_TARGET" to "//foo:foo_test",
        "TEST_TIMEOUT" to "300",
        "TEST_TMPDIR" to tmpDir.toString(),
        "TEST_UNUSED_RUNFILES_LOG_FILE" to Path("$EXEC_ROOT/$TESTLOGS/test.unused_runfiles_log").toString(),
        "TEST_WORKSPACE" to "_main",
        "TZ" to "UTC",
        "XML_OUTPUT_FILE" to Path("$EXEC_ROOT/$TESTLOGS/test.xml").toString(),
      ),
      directoriesToCreate = listOf(tmpDir, Path("$EXEC_ROOT/$TESTLOGS")),
    )
  }

  @Test
  fun `parses Windows C++ binary`() {
    val info = ExecutableInfo.fromBazelRunScript(WIN_BINARY_SCRIPT)
    info shouldBe ExecutableInfo(
      binary = Path("C:\\users\\user\\_bazel_user\\t3olqtfj\\execroot\\_main\\bazel-out\\x64_windows-dbg\\bin\\main\\echo0.exe"),
      workingDir = Path("$WIN_EXEC_ROOT/$WIN_BIN/main/echo0.exe.runfiles"),
      args = emptyList(),
      envVars = mapOf(
        "BUILD_EXECROOT" to WIN_EXEC_ROOT,
        "BUILD_WORKING_DIRECTORY" to "F:/ideaprojects/clion/execution",
        "BUILD_WORKSPACE_DIRECTORY" to "F:/ideaprojects/clion/execution",
      ),
    )
  }

  @Test
  fun `parses Windows C++ test with manifest-only runfiles`(@TempDir execRoot: Path) {
    val runfiles = execRoot.resolve(WIN_RUNFILES)
    val catchBinary = execRoot.resolve("$WIN_BIN/main/catch.exe")
    runfiles.createDirectories()
    runfiles.resolve("MANIFEST")
      .writeText("_main/main/other.exe ${execRoot.resolve("$WIN_BIN/main/other.exe")}\n_main/main/catch.exe $catchBinary\n")
    val script = WIN_TEST_SCRIPT.replace(WIN_EXEC_ROOT, execRoot.invariantSeparatorsPathString)

    val info = ExecutableInfo.fromBazelRunScript(script)

    val tmpDir = execRoot.resolve("_tmp/959b95b9d92618b2a99c4041f6f84373").toString()
    info.binary shouldBe catchBinary
    info.workingDir shouldBe runfiles.resolve("_main")
    info.args shouldBe emptyList()
    info.envVars["BAZEL_TEST"] shouldBe "1"
    info.envVars["TEST_SRCDIR"] shouldBe runfiles.toString()
    info.envVars["TEST_TMPDIR"] shouldBe tmpDir
    info.envVars["GTEST_TMP_DIR"] shouldBe tmpDir
    info.envVars["RUNFILES_MANIFEST_FILE"] shouldBe runfiles.resolve("MANIFEST").toString()
    info.envVars["PATH"] shouldBe ".;C:\\msys64\\usr\\bin;C:\\msys64\\bin;C:\\Windows;C:\\Windows\\System32;C:\\Windows\\System32\\WindowsPowerShell\\v1.0"
    info.envVars.keys shouldNotContain "TEST_UNDECLARED_OUTPUTS_MANIFEST"
    info.envVars.keys shouldNotContain "TEST_UNDECLARED_OUTPUTS_ANNOTATIONS"
  }

  @Test
  fun `parses program arguments from a go_binary run script and unquotes them`() {
    val script =
      """
      #!/bin/bash
      cd $EXEC_ROOT && \
        exec env \
        $BIN/foo/foo_/foo --verbose 'name with spaces' 'it'\''s' "$@"
      """.trimIndent()

    val info = ExecutableInfo.fromBazelRunScript(script)

    info shouldBe ExecutableInfo(
      binary = Path("$BIN/foo/foo_/foo"),
      workingDir = Path(EXEC_ROOT),
      args = listOf("--verbose", "name with spaces", "it's"),
      envVars = emptyMap(),
    )
  }

  @Test
  fun `keeps a backslash before the closing single quote and an empty argument`() {
    val script =
      """
      #!/bin/bash
      cd $EXEC_ROOT && \
        exec env \
        $BIN/foo/foo_/foo 'C:\tmp\' '' next "$@"
      """.trimIndent()

    val info = ExecutableInfo.fromBazelRunScript(script)

    info.args shouldBe listOf("C:\\tmp\\", "", "next")
  }

  @Test
  fun `keeps a backslash before the closing double quote on Windows`() {
    val script =
      """
      @echo off
        cd /d C:/execroot/_main
        SET BUILD_WORKSPACE_DIRECTORY=C:/workspace
        C:\execroot\_main\bin\program.exe "C:\path with spaces\\" next %*
      """.trimIndent()

    val info = ExecutableInfo.fromBazelRunScript(script)

    info.args shouldBe listOf("C:\\path with spaces\\", "next")
  }

  @Test
  fun `ignores a newline after the last line`() {
    ExecutableInfo.fromBazelRunScript("$TEST_SCRIPT\n") shouldBe ExecutableInfo.fromBazelRunScript(TEST_SCRIPT)
    ExecutableInfo.fromBazelRunScript("$WIN_BINARY_SCRIPT\n") shouldBe ExecutableInfo.fromBazelRunScript(WIN_BINARY_SCRIPT)
  }

  @Test
  fun `parses program arguments from a go_test run script and unquotes them`() {
    val script =
      """
      #!/bin/bash
      cd /execroot/_main && \
        exec env \
          TEST_SRCDIR=foo.runfiles \
          TEST_WORKSPACE=_main \
          TEST_TARGET=//foo:foo_test \
        wrapper foo/foo_test_/foo_test -test.run 'name with spaces' "$@"
      """.trimIndent()

    val info = ExecutableInfo.fromBazelRunScript(script)

    val runfilesRoot = Path("/execroot/_main/foo.runfiles/_main")
    info shouldBe ExecutableInfo(
      binary = runfilesRoot.resolve("foo/foo_test_/foo_test"),
      workingDir = runfilesRoot,
      args = listOf("-test.run", "name with spaces"),
      envVars = mapOf(
        "BAZEL_TEST" to "1",
        "TEST_SRCDIR" to Path("/execroot/_main/foo.runfiles").toString(),
        "TEST_TARGET" to "//foo:foo_test",
        "TEST_WORKSPACE" to "_main",
      ),
    )
  }

  @Test
  fun `throws when the execution root cd line is missing`() {
    val script =
      """
      #!/bin/bash
        exec env \
        /execroot/_main/bin/foo "$@"
      """.trimIndent()

    shouldThrow<UnexpectedScriptContentException> { ExecutableInfo.fromBazelRunScript(script) }
  }

  @Test
  fun `throws when a regular binary has fewer than two arguments`() {
    val script =
      """
      #!/bin/bash
      cd /execroot/_main && \
        exec env \
        "$@"
      """.trimIndent()

    shouldThrow<UnexpectedScriptContentException> { ExecutableInfo.fromBazelRunScript(script) }
  }

  @Test
  fun `throws when a test binary has fewer than three arguments`() {
    val script =
      """
      #!/bin/bash
      cd /execroot/_main && \
        exec env \
          TEST_SRCDIR=foo.runfiles \
        wrapper "$@"
      """.trimIndent()

    shouldThrow<UnexpectedScriptContentException> { ExecutableInfo.fromBazelRunScript(script) }
  }

  @Test
  fun `throws when a test binary script has no workspace name`() {
    val script =
      """
      #!/bin/bash
      cd /execroot/_main && \
        exec env \
          TEST_SRCDIR=foo.runfiles \
        wrapper bin/foo "$@"
      """.trimIndent()

    shouldThrow<UnexpectedScriptContentException> { ExecutableInfo.fromBazelRunScript(script) }
  }

  @Test
  fun `throws when the runfiles manifest is missing`() {
    val script =
      """
      #!/bin/bash
      cd /execroot/_main && \
        exec env \
          TEST_SRCDIR=foo.runfiles \
          TEST_WORKSPACE=_main \
          RUNFILES_MANIFEST_ONLY=1 \
        wrapper bin/foo "$@"
      """.trimIndent()

    shouldThrow<UnexpectedScriptContentException> { ExecutableInfo.fromBazelRunScript(script) }
  }

  @Test
  fun `finds an external test in the runfiles manifest`(@TempDir execRoot: Path) {
    val runfiles = execRoot.resolve("foo.runfiles")
    val testBinary = execRoot.resolve("external/other/foo/foo_test")
    runfiles.createDirectories()
    runfiles.resolve("MANIFEST").writeText("other/foo/foo_test $testBinary\n")
    val script =
      """
      #!/bin/bash
      cd ${execRoot.invariantSeparatorsPathString} && \
        exec env \
          TEST_SRCDIR=foo.runfiles \
          TEST_WORKSPACE=_main \
          RUNFILES_MANIFEST_ONLY=1 \
        wrapper ../other/foo/foo_test "$@"
      """.trimIndent()

    val info = ExecutableInfo.fromBazelRunScript(script)

    info.binary shouldBe testBinary
  }

  companion object {

    private const val EXEC_ROOT = "/cache/execroot/_main"
    private const val BIN = "bazel-out/darwin-fastbuild/bin"
    private const val RUNFILES = "$BIN/foo/foo_test_/foo_test.runfiles"
    private const val TESTLOGS = "bazel-out/darwin-fastbuild/testlogs/foo/foo_test"

    private val BINARY_SCRIPT =
      """
      #!/bin/bash
      cd $EXEC_ROOT/$BIN/foo/foo_/foo.runfiles/_main && \
        exec env \
          -u JAVA_RUNFILES \
          -u RUNFILES_DIR \
          -u RUNFILES_MANIFEST_FILE \
          -u RUNFILES_MANIFEST_ONLY \
          -u TEST_SRCDIR \
          BUILD_EXECROOT=$EXEC_ROOT \
          BUILD_ID=build-id \
          BUILD_WORKING_DIRECTORY=/workspace \
          BUILD_WORKSPACE_DIRECTORY=/workspace \
        $EXEC_ROOT/$BIN/foo/foo_/foo "$@"
      """.trimIndent()

    private val TEST_SCRIPT =
      """
      #!/bin/bash
      cd $EXEC_ROOT && \
        exec env \
          -u JAVA_RUNFILES \
          -u RUNFILES_DIR \
          -u RUNFILES_MANIFEST_FILE \
          -u RUNFILES_MANIFEST_ONLY \
          -u TEST_SRCDIR \
          BUILD_EXECROOT=$EXEC_ROOT \
          BUILD_ID=build-id \
          BUILD_WORKING_DIRECTORY=/workspace \
          BUILD_WORKSPACE_DIRECTORY=/workspace \
          GO_TEST_RUN_FROM_BAZEL=1 \
          JAVA_RUNFILES=$RUNFILES \
          PATH=/bin:/usr/bin:/usr/local/bin \
          PYTHON_RUNFILES=$RUNFILES \
          RUNFILES_DIR=$RUNFILES \
          RUN_UNDER_RUNFILES=1 \
          TEST_BINARY=foo/foo_test_/foo_test \
          TEST_SIZE=medium \
          TEST_SRCDIR=$RUNFILES \
          TEST_TARGET=//foo:foo_test \
          TEST_TIMEOUT=300 \
          TEST_TMPDIR=_tmp/tmpdir \
          TEST_UNUSED_RUNFILES_LOG_FILE=$TESTLOGS/test.unused_runfiles_log \
          TEST_WORKSPACE=_main \
          TZ=UTC \
          XML_OUTPUT_FILE=$TESTLOGS/test.xml \
        external/bazel_tools/tools/test/test-setup.sh foo/foo_test_/foo_test "$@"
      """.trimIndent()

    private const val WIN_EXEC_ROOT = "C:/users/user/_bazel_user/t3olqtfj/execroot/_main"
    private const val WIN_BIN = "bazel-out/x64_windows-dbg/bin"
    private const val WIN_RUNFILES = "$WIN_BIN/main/catch.exe.runfiles"
    private const val WIN_TESTLOGS = "bazel-out/x64_windows-dbg/testlogs/main/catch"

    private val WIN_BINARY_SCRIPT =
      """
      @echo off
        cd /d $WIN_EXEC_ROOT/$WIN_BIN/main/echo0.exe.runfiles
        SET JAVA_RUNFILES=
        SET RUNFILES_DIR=
        SET RUNFILES_MANIFEST_FILE=
        SET RUNFILES_MANIFEST_ONLY=
        SET TEST_SRCDIR=
        SET BUILD_EXECROOT=$WIN_EXEC_ROOT
        //SET BUILD_ID=6c09bcae-4aad-4525-bf92-0a9cd93c2052
        SET BUILD_WORKING_DIRECTORY=F:/ideaprojects/clion/execution
        SET BUILD_WORKSPACE_DIRECTORY=F:/ideaprojects/clion/execution
        C:\users\user\_bazel_user\t3olqtfj\execroot\_main\bazel-out\x64_windows-dbg\bin\main\echo0.exe %*
      """.trimIndent()

    private val WIN_TEST_SCRIPT =
      """
      @echo off
        cd /d $WIN_EXEC_ROOT
        SET JAVA_RUNFILES=
        SET RUNFILES_DIR=
        SET RUNFILES_MANIFEST_FILE=
        SET RUNFILES_MANIFEST_ONLY=
        SET TEST_SRCDIR=
        SET BUILD_EXECROOT=$WIN_EXEC_ROOT
        SET BUILD_ID=294d8a68-0b8c-409b-882c-02004a48fe39
        SET BUILD_WORKING_DIRECTORY=F:/ideaprojects/clion/execution
        SET BUILD_WORKSPACE_DIRECTORY=F:/ideaprojects/clion/execution
        SET JAVA_RUNFILES=$WIN_RUNFILES
        SET PATH=C:\msys64\usr\bin;C:\msys64\bin;C:\Windows;C:\Windows\System32;C:\Windows\System32\WindowsPowerShell\v1.0
        SET PYTHON_RUNFILES=$WIN_RUNFILES
        SET RUNFILES_DIR=$WIN_RUNFILES
        SET RUNFILES_MANIFEST_ONLY=1
        SET RUN_UNDER_RUNFILES=1
        SET TEST_BINARY=main\catch.exe
        SET TEST_INFRASTRUCTURE_FAILURE_FILE=$WIN_TESTLOGS/test.infrastructure_failure
        SET TEST_LOGSPLITTER_OUTPUT_FILE=$WIN_TESTLOGS/test.raw_splitlogs/test.splitlogs
        SET TEST_PREMATURE_EXIT_FILE=$WIN_TESTLOGS/test.exited_prematurely
        SET TEST_SIZE=medium
        SET TEST_SRCDIR=$WIN_BIN/main/catch.exe.runfiles
        SET TEST_TARGET=//main:catch
        SET TEST_TIMEOUT=300
        SET TEST_TMPDIR=_tmp/959b95b9d92618b2a99c4041f6f84373
        SET TEST_UNDECLARED_OUTPUTS_ANNOTATIONS=$WIN_TESTLOGS/test.outputs_manifest/ANNOTATIONS
        SET TEST_UNDECLARED_OUTPUTS_ANNOTATIONS_DIR=$WIN_TESTLOGS/test.outputs_manifest
        SET TEST_UNDECLARED_OUTPUTS_DIR=$WIN_TESTLOGS/test.outputs
        SET TEST_UNDECLARED_OUTPUTS_MANIFEST=$WIN_TESTLOGS/test.outputs_manifest/MANIFEST
        SET TEST_UNUSED_RUNFILES_LOG_FILE=$WIN_TESTLOGS/test.unused_runfiles_log
        SET TEST_WARNINGS_OUTPUT_FILE=$WIN_TESTLOGS/test.warnings
        SET TEST_WORKSPACE=_main
        SET TZ=UTC
        SET XML_OUTPUT_FILE=$WIN_TESTLOGS/test.xml
        external\bazel_tools\tools\test\tw.exe main/catch.exe %*
      """.trimIndent()
  }
}
