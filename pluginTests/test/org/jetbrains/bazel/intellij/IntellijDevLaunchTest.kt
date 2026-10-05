package org.jetbrains.bazel.intellij

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.label.Label
import org.junit.jupiter.api.Test
import java.nio.file.Path

private const val RUNFILES = "/cache/execroot/_main/bazel-out/darwin_arm64-fastbuild/bin/build/idea.runfiles"

private val LAUNCH_JSON = """
  {
    "argfile": "$RUNFILES/_main/idea.jvm.args",
    "env": {
      "JAVA_RUNFILES": "$RUNFILES",
      "RUNFILES_DIR": "$RUNFILES"
    },
    "java": "/cache/execroot/_main/external/remotejbr25/bin/java",
    "runfilesDirectory": "$RUNFILES",
    "version": 2,
    "workingDirectory": "$RUNFILES/_main"
  }
""".trimIndent()

private val BAZEL_BIN = Path.of("/work/idea/bazel-bin")

private val REPO_MAPPING = mapOf("" to "", "community" to "community+")

internal class IntellijDevLaunchTest {
  @Test
  fun `parses the launch file of the rule`() {
    IntellijDevLaunch.parse(LAUNCH_JSON) shouldBe IntellijDevLaunch(
      java = "/cache/execroot/_main/external/remotejbr25/bin/java",
      argfile = "$RUNFILES/_main/idea.jvm.args",
      runfilesDirectory = RUNFILES,
      workingDirectory = "$RUNFILES/_main",
      environment = mapOf("JAVA_RUNFILES" to RUNFILES, "RUNFILES_DIR" to RUNFILES),
    )
  }

  @Test
  fun `refuses another version`() {
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunch.parse(LAUNCH_JSON.replace("\"version\": 2", "\"version\": 1"))
    }.message shouldBe "the version is not 2"
  }

  @Test
  fun `refuses a launch file without the argument file`() {
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunch.parse(LAUNCH_JSON.replace("\"argfile\"", "\"argFile\""))
    }.message shouldContain "`argfile` is missing"
  }

  @Test
  fun `refuses an environment value that is not a string`() {
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunch.parse(LAUNCH_JSON.replace("\"RUNFILES_DIR\": \"$RUNFILES\"", "\"RUNFILES_DIR\": 1"))
    }.message shouldContain "`env.RUNFILES_DIR` is not a string"
  }

  @Test
  fun `puts java first, the VM options before the argument file and the extra program arguments after it`() {
    val launch = IntellijDevLaunch.parse(LAUNCH_JSON)
    val vmOptions = listOf(jdwpVmOption(5005), "-javaagent:/ide/lib/debugger-agent.jar")
    intellijDevLaunchCommand(launch, vmOptions, listOf("--extra")) shouldContainExactly listOf(
      "/cache/execroot/_main/external/remotejbr25/bin/java",
      "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005",
      "-javaagent:/ide/lib/debugger-agent.jar",
      "@$RUNFILES/_main/idea.jvm.args",
      "--extra",
    )
  }

  @Test
  fun `a run puts nothing around the argument file, which holds the program arguments of the row`() {
    val launch = IntellijDevLaunch.parse(LAUNCH_JSON)
    intellijDevLaunchCommand(launch, emptyList(), emptyList()) shouldContainExactly listOf(
      "/cache/execroot/_main/external/remotejbr25/bin/java",
      "@$RUNFILES/_main/idea.jvm.args",
    )
  }

  @Test
  fun `finds the launch file of a main repository row in its package`() {
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("//build:idea"), REPO_MAPPING) shouldBe BAZEL_BIN.resolve("build/idea.launch.json")
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("@//build:idea"), REPO_MAPPING) shouldBe BAZEL_BIN.resolve("build/idea.launch.json")
  }

  @Test
  fun `finds the launch file of an external repository row below its canonical repository name`() {
    val expected = BAZEL_BIN.resolve("external/community+/build/idea_community.launch.json")
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("@@community+//build:idea_community"), REPO_MAPPING) shouldBe expected
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("@community//build:idea_community"), REPO_MAPPING) shouldBe expected
  }

  @Test
  fun `finds no launch file for a repository that the mapping does not know`() {
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("@unknown//build:idea"), REPO_MAPPING).shouldBeNull()
  }

  @Test
  fun `the environment is the one of bazel run`() {
    val launch = IntellijDevLaunch.parse(LAUNCH_JSON)
    val environment = intellijDevLaunchEnvironment(
      launch = launch,
      parentEnvironment = mapOf("PATH" to "/usr/bin", "RUNFILES_MANIFEST_FILE" to "/stale/MANIFEST", "JAVA_RUNFILES" to "/stale"),
      runConfigurationEnvironment = mapOf("CWM_NO_TIMEOUTS" to "1", "RUNFILES_DIR" to "/user"),
      workspaceRoot = "/work/idea",
    )
    environment shouldContainExactly mapOf(
      "PATH" to "/usr/bin",
      "BUILD_WORKING_DIRECTORY" to "/work/idea",
      "BUILD_WORKSPACE_DIRECTORY" to "/work/idea",
      "JAVA_RUNFILES" to RUNFILES,
      "RUNFILES_DIR" to "/user",
      "CWM_NO_TIMEOUTS" to "1",
    )
  }
}
