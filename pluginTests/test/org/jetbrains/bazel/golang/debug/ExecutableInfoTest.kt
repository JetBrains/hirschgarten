package org.jetbrains.bazel.golang.debug

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ExecutableInfoTest {
  @Test
  fun `parses a regular go_binary run script`() {
    val info = ExecutableInfo.fromBazelRunScript(BINARY_SCRIPT)
    info shouldBe ExecutableInfo(
      binary = Path("$EXEC_ROOT/$BIN/foo/foo_/foo"),
      workingDir = Path("$EXEC_ROOT/$BIN/foo/foo_/foo.runfiles/_main"),
      args = emptyList(),
      envVars = emptyMap(),
    )
  }

  @Test
  fun `parses a go_test run script and resolves paths against the runfiles root`() {
    val info = ExecutableInfo.fromBazelRunScript(TEST_SCRIPT)

    val runfilesRoot = Path("$EXEC_ROOT/$RUNFILES/_main")
    info shouldBe ExecutableInfo(
      binary = runfilesRoot.resolve("foo/foo_test_/foo_test"),
      workingDir = runfilesRoot.resolve("foo"),
      args = emptyList(),
      envVars = mapOf(
        "RUNFILES_DIR" to Path("$EXEC_ROOT/$RUNFILES").toString(),
        "RUNFILES_LOG_FILE" to Path("$EXEC_ROOT/$TESTLOGS/test.unused_runfiles_log").toString(),
        "TEST_SRCDIR" to Path("$EXEC_ROOT/$RUNFILES").toString(),
      ),
    )
  }

  @Test
  fun `parses program arguments from a go_binary run script and unquotes them`() {
    val script =
      """
      #!/bin/bash
      cd $EXEC_ROOT && \
        exec env \
        $BIN/foo/foo_/foo --verbose 'name with spaces' "$@"
      """.trimIndent()

    val info = ExecutableInfo.fromBazelRunScript(script)

    info shouldBe ExecutableInfo(
      binary = Path("$BIN/foo/foo_/foo"),
      workingDir = Path(EXEC_ROOT),
      args = listOf("--verbose", "name with spaces"),
      envVars = emptyMap(),
    )
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
      workingDir = runfilesRoot.resolve("foo"),
      args = listOf("-test.run", "name with spaces"),
      envVars = mapOf("TEST_SRCDIR" to Path("/execroot/_main/foo.runfiles").toString()),
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
  fun `throws when the runfiles are manifest-only`() {
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

    shouldThrow<RunfileManifestOnlyException> { ExecutableInfo.fromBazelRunScript(script) }
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
  }
}
