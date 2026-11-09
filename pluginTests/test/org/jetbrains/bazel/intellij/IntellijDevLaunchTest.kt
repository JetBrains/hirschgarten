package org.jetbrains.bazel.intellij

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.jetbrains.bazel.label.Label
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

private const val RUNFILES = "/base/bin/build/idea.runfiles"

/** The launch file of `//build:idea` on macOS, with the paths shortened. */
private val LAUNCH_JSON = """
  {
    "env": {
      "JAVA_RUNFILES": "$RUNFILES",
      "RUNFILES_DIR": "$RUNFILES"
    },
    "materializer": ["/base/bin/dev-home", "materialize", "--spec=/base/bin/build/idea.home.json", "--format=json"],
    "programArguments": [],
    "version": 4,
    "workingDirectory": "$RUNFILES/_main"
  }
""".trimIndent()

/** The answer of the materializer of [LAUNCH_JSON] for the generation `0a1b`. */
private val HOME = IntellijDevHome(
  home = "/cache/dev-home/g/0a1b",
  launcher = "/cache/dev-home/g/0a1b/Contents/MacOS/idea",
  vmOptionsFile = "/cache/dev-home/g/0a1b/vmoptions-1a2b",
  vmOptionsVariable = "IDEA_VM_OPTIONS",
)

private val BAZEL_BIN = Path.of("/work/idea/bazel-bin")

private val REPO_MAPPING = mapOf("" to "", "community" to "community+")

internal class IntellijDevLaunchTest {
  @Test
  fun `parses the launch file of the rule`() {
    IntellijDevLaunch.parse(LAUNCH_JSON) shouldBe IntellijDevLaunch(
      materializer = listOf("/base/bin/dev-home", "materialize", "--spec=/base/bin/build/idea.home.json", "--format=json"),
      programArguments = emptyList(),
      workingDirectory = "$RUNFILES/_main",
      environment = mapOf("JAVA_RUNFILES" to RUNFILES, "RUNFILES_DIR" to RUNFILES),
    )
  }

  @Test
  fun `refuses the launch file of a checkout before version 4`() {
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunch.parse("""{"version": 3, "launcher": "$RUNFILES/ide_home/Contents/MacOS/idea"}""")
    }.message shouldBe "the version is 3, not 4, so the checkout predates launch file version 4. Update the checkout and build the target again"
    shouldThrow<IllegalArgumentException> { IntellijDevLaunch.parse("{}") }.message shouldStartWith "the version is missing, not 4"
  }

  @Test
  fun `refuses a launch file with a missing or wrong field`() {
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunch.parse(LAUNCH_JSON.replace(Regex("\"materializer\": \\[[^\\]]*\\],"), ""))
    }.message shouldBe "`materializer` is missing"
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunch.parse(LAUNCH_JSON.replace(Regex("\"materializer\": \\[[^\\]]*\\]"), "\"materializer\": []"))
    }.message shouldBe "`materializer` is empty"
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunch.parse(LAUNCH_JSON.replace("\"programArguments\": []", "\"programArguments\": \"a\""))
    }.message shouldBe "`programArguments` is not an array"
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunch.parse(LAUNCH_JSON.replace("\"RUNFILES_DIR\": \"$RUNFILES\"", "\"RUNFILES_DIR\": 1"))
    }.message shouldBe "`env.RUNFILES_DIR` is not a string"
  }

  @Test
  fun `the answer of the materializer is the home`() {
    val answer = """{"home":"${HOME.home}","launcher":"${HOME.launcher}","vmOptionsFile":"${HOME.vmOptionsFile}","vmOptionsVariable":"IDEA_VM_OPTIONS"}""" + "\n"
    intellijDevHomeOf(0, answer, "") shouldBe HOME
    shouldThrow<IOException> {
      intellijDevHomeOf(0, """{"home":"/h","launcher":"/h/l","vmOptionsFile":"/h.vmoptions"}""", "")
    }.message shouldBe "the materializer printed no home: `vmOptionsVariable` is missing"
  }

  @Test
  fun `a failure of the materializer has its error line`() {
    shouldThrow<IOException> {
      intellijDevHomeOf(1, "", "ERROR: cannot read /base/bin/build/idea.fingerprint: No such file or directory\n")
    }.message shouldBe "cannot read /base/bin/build/idea.fingerprint: No such file or directory"
    shouldThrow<IOException> { intellijDevHomeOf(101, "", "") }.message shouldBe "the materializer exited with code 101"
  }

  @Test
  fun `puts the launcher of the home first, then the program arguments of the target, then the extra program arguments`() {
    val launch = IntellijDevLaunch.parse(LAUNCH_JSON).copy(programArguments = listOf("serverMode"))
    intellijDevLaunchCommand(launch, HOME, listOf("--extra")) shouldContainExactly listOf(HOME.launcher, "serverMode", "--extra")
  }

  @Test
  fun `finds the launch file of a main repository target in its package`() {
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("//build:idea"), REPO_MAPPING) shouldBe BAZEL_BIN.resolve("build/idea.launch.json")
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("@//build:idea"), REPO_MAPPING) shouldBe BAZEL_BIN.resolve("build/idea.launch.json")
  }

  @Test
  fun `finds the launch file of an external repository target below its canonical repository name`() {
    val expected = BAZEL_BIN.resolve("external/community+/build/idea_community.launch.json")
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("@@community+//build:idea_community"), REPO_MAPPING) shouldBe expected
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("@community//build:idea_community"), REPO_MAPPING) shouldBe expected
  }

  @Test
  fun `finds no launch file for a repository that the mapping does not know`() {
    intellijDevLaunchFile(BAZEL_BIN, Label.parse("@unknown//build:idea"), REPO_MAPPING).shouldBeNull()
  }

  @Test
  fun `the environment is the one of bazel run, and the variable of the home names its vmoptions file`() {
    val environment = intellijDevLaunchEnvironment(
      launch = IntellijDevLaunch.parse(LAUNCH_JSON),
      home = HOME,
      parentEnvironment = mapOf("PATH" to "/usr/bin", "RUNFILES_MANIFEST_FILE" to "/stale/MANIFEST", "JAVA_RUNFILES" to "/stale"),
      runConfigurationEnvironment = mapOf("CWM_NO_TIMEOUTS" to "1", "RUNFILES_DIR" to "/user"),
      workspaceRoot = "/work/idea",
      vmOptionsCopy = null,
    )
    environment shouldContainExactly mapOf(
      "PATH" to "/usr/bin",
      "BUILD_WORKING_DIRECTORY" to "/work/idea",
      "BUILD_WORKSPACE_DIRECTORY" to "/work/idea",
      "JAVA_RUNFILES" to RUNFILES,
      "RUNFILES_DIR" to "/user",
      "IDEA_VM_OPTIONS" to HOME.vmOptionsFile,
      "CWM_NO_TIMEOUTS" to "1",
    )
  }

  @Test
  fun `the vmoptions copy wins over the variable of the run configuration`() {
    val environment = intellijDevLaunchEnvironment(
      launch = IntellijDevLaunch.parse(LAUNCH_JSON),
      home = HOME,
      parentEnvironment = emptyMap(),
      runConfigurationEnvironment = mapOf("IDEA_VM_OPTIONS" to "/mine.vmoptions"),
      workspaceRoot = "/work/idea",
      vmOptionsCopy = Path.of("/system/bazel-dev-launch/idea-1.vmoptions"),
    )
    environment["IDEA_VM_OPTIONS"] shouldBe Path.of("/system/bazel-dev-launch/idea-1.vmoptions").toString()
  }

  @Test
  fun `the extra VM options go to a copy of the vmoptions file of the home`(@TempDir temp: Path) {
    val launch = IntellijDevLaunch.parse(LAUNCH_JSON)
    val home = homeWithVmOptions(temp.resolve("g1"), "-Dawt.toolkit.name=auto\n-Didea.is.internal=true")
    val agent = "-javaagent:/my caches/debugger-agent.jar=file:/my caches/capture.props"
    val directory = temp.resolve("copies")

    val copy = writeIntellijDevVmOptionsCopy(launch, home, listOf(jdwpVmOption(5005), agent), directory)
    copy.parent shouldBe directory
    copy.fileName.toString() shouldStartWith "idea-"
    Files.readString(copy) shouldBe "-Dawt.toolkit.name=auto\n-Didea.is.internal=true\n${jdwpVmOption(5005)}\n$agent\n"
    // The file of the home stays as it is.
    Files.readString(Path.of(home.vmOptionsFile)) shouldBe "-Dawt.toolkit.name=auto\n-Didea.is.internal=true"

    // The next start from a new generation writes the same copy again, and the directory holds no temporary file.
    val next = homeWithVmOptions(temp.resolve("g2"), "-Didea.is.internal=true\n")
    writeIntellijDevVmOptionsCopy(launch, next, listOf(jdwpVmOption(6006)), directory) shouldBe copy
    Files.readString(copy) shouldBe "-Didea.is.internal=true\n${jdwpVmOption(6006)}\n"
    Files.list(directory).use { files -> files.map { it.fileName.toString() }.toList() } shouldContainExactly listOf(copy.fileName.toString())

    // The target of the same name in another checkout gets its own copy.
    val other = launch.copy(materializer = listOf("/other/bin/dev-home", "materialize", "--spec=/other/bin/build/idea.home.json"))
    writeIntellijDevVmOptionsCopy(other, next, listOf(jdwpVmOption(5005)), directory) shouldNotBe copy
  }

  @Test
  fun `refuses an extra VM option that is not one line`(@TempDir temp: Path) {
    val home = homeWithVmOptions(temp, "")
    for (option in listOf("-Da=1\n-Db=2", " -Da=1", "# -Da=1", "")) {
      shouldThrow<IllegalArgumentException> {
        writeIntellijDevVmOptionsCopy(IntellijDevLaunch.parse(LAUNCH_JSON), home, listOf(option), temp.resolve("copies"))
      }
    }
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `the materializer gets the environment and gives the home`(@TempDir temp: Path) {
    val root = Files.createDirectories(temp.resolve("dev-home"))
    val materializer = Files.writeString(temp.resolve("dev-home.sh"), """
      printf '%s\n' "${'$'}*" > "${'$'}INTELLIJ_DEV_HOME_ROOT/arguments"
      printf '{"home":"%s","launcher":"%s","vmOptionsFile":"%s","vmOptionsVariable":"IDEA_VM_OPTIONS"}\n' \
        "${'$'}INTELLIJ_DEV_HOME_ROOT/g/1" "${'$'}INTELLIJ_DEV_HOME_ROOT/g/1/bin/idea" "${'$'}INTELLIJ_DEV_HOME_ROOT/g/1/vmoptions-1"
    """.trimIndent() + "\n")

    materializeIntellijDevHome(listOf("/bin/sh", materializer.toString(), "materialize"), mapOf("INTELLIJ_DEV_HOME_ROOT" to root.toString())) shouldBe
      IntellijDevHome("$root/g/1", "$root/g/1/bin/idea", "$root/g/1/vmoptions-1", "IDEA_VM_OPTIONS")
    Files.readString(root.resolve("arguments")) shouldBe "materialize\n"
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `a failed materializer gives its message`(@TempDir temp: Path) {
    val materializer = Files.writeString(temp.resolve("dev-home.sh"), "echo 'ERROR: cannot read /base/bin/build/idea.fingerprint' >&2\nexit 1\n")
    shouldThrow<IOException> {
      materializeIntellijDevHome(listOf("/bin/sh", materializer.toString()), emptyMap())
    }.message shouldBe "cannot read /base/bin/build/idea.fingerprint"
  }
}

/** The home of a generation in [directory] with a vmoptions file of [text]. */
private fun homeWithVmOptions(directory: Path, text: String): IntellijDevHome {
  val vmOptionsFile = Files.writeString(Files.createDirectories(directory).resolve("vmoptions-1a2b"), text)
  return HOME.copy(home = directory.toString(), vmOptionsFile = vmOptionsFile.toString())
}
