package org.jetbrains.bazel.intellij

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
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
      "IDEA_VM_OPTIONS": "/base/bin/build/idea.vmoptions",
      "JAVA_RUNFILES": "$RUNFILES",
      "RUNFILES_DIR": "$RUNFILES"
    },
    "launcher": "$RUNFILES/ide_home/Contents/MacOS/idea",
    "programArguments": [],
    "runfilesDirectory": "$RUNFILES",
    "version": 3,
    "vmOptionsFile": "/base/bin/build/idea.vmoptions",
    "vmOptionsVariable": "IDEA_VM_OPTIONS",
    "workingDirectory": "$RUNFILES/_main"
  }
""".trimIndent()

/** The launch file of version 4 of `//build:idea` on macOS, with the paths shortened. */
private val HOME_LAUNCH_JSON = """
  {
    "env": {
      "JAVA_RUNFILES": "$RUNFILES",
      "RUNFILES_DIR": "$RUNFILES"
    },
    "materializer": ["/base/bin/dev-home", "materialize", "--spec=/base/bin/build/idea.home.json", "--format=json"],
    "programArguments": ["serverMode"],
    "version": 4,
    "workingDirectory": "$RUNFILES/_main"
  }
""".trimIndent()

private val BAZEL_BIN = Path.of("/work/idea/bazel-bin")

private val REPO_MAPPING = mapOf("" to "", "community" to "community+")

internal class IntellijDevLaunchTest {
  @Test
  fun `parses the launch file of the rule`() {
    parseV3(LAUNCH_JSON) shouldBe IntellijDevLaunch(
      launcher = "$RUNFILES/ide_home/Contents/MacOS/idea",
      vmOptionsFile = "/base/bin/build/idea.vmoptions",
      vmOptionsVariable = "IDEA_VM_OPTIONS",
      launcherSource = null,
      programArguments = emptyList(),
      runfilesDirectory = RUNFILES,
      workingDirectory = "$RUNFILES/_main",
      environment = mapOf("IDEA_VM_OPTIONS" to "/base/bin/build/idea.vmoptions", "JAVA_RUNFILES" to RUNFILES, "RUNFILES_DIR" to RUNFILES),
    )
  }

  @Test
  fun `a Linux launch file states the launcher source`() {
    val text = LAUNCH_JSON.replace("\"version\": 3,", "\"version\": 3,\n\"launcherSource\": \"/base/bin/build/idea_home/bin/idea\",")
    parseV3(text).launcherSource shouldBe "/base/bin/build/idea_home/bin/idea"
  }

  @Test
  fun `refuses the launch file of an older checkout`() {
    shouldThrow<IllegalArgumentException> {
      parseV3("""{"version": 2, "java": "/jbr/bin/java", "argfile": "/a/idea.jvm.args"}""")
    }.message shouldStartWith "the version is 2, not 3 or 4"
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunchFile.parse("""{"version": 5}""")
    }.message shouldStartWith "the version is 5, not 3 or 4"
  }

  @Test
  fun `parses the launch file of version 4, which names the materializer`() {
    IntellijDevLaunchFile.parse(HOME_LAUNCH_JSON) shouldBe IntellijDevHomeLaunch(
      materializer = listOf("/base/bin/dev-home", "materialize", "--spec=/base/bin/build/idea.home.json", "--format=json"),
      programArguments = listOf("serverMode"),
      workingDirectory = "$RUNFILES/_main",
      environment = mapOf("JAVA_RUNFILES" to RUNFILES, "RUNFILES_DIR" to RUNFILES),
    )
  }

  @Test
  fun `refuses a launch file of version 4 without the materializer`() {
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunchFile.parse(HOME_LAUNCH_JSON.replace(Regex("\"materializer\": \\[[^\\]]*\\],"), ""))
    }.message shouldBe "`materializer` is missing"
    shouldThrow<IllegalArgumentException> {
      IntellijDevLaunchFile.parse(HOME_LAUNCH_JSON.replace(Regex("\"materializer\": \\[[^\\]]*\\]"), "\"materializer\": []"))
    }.message shouldBe "`materializer` is empty"
  }

  @Test
  fun `the answer of the materializer is the home`() {
    val answer = """{"home":"/root/g/1/idea","launcher":"/root/g/1/idea/bin/idea","vmOptionsFile":"/root/g/1/idea.vmoptions","vmOptionsVariable":"IDEA_VM_OPTIONS"}""" + "\n"
    intellijDevHomeOf(0, answer, "") shouldBe IntellijDevHome("/root/g/1/idea", "/root/g/1/idea/bin/idea", "/root/g/1/idea.vmoptions", "IDEA_VM_OPTIONS")
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
  @DisabledOnOs(OS.WINDOWS)
  fun `a launch of version 4 starts the launcher of the materialized home`(@TempDir temp: Path) {
    val root = temp.resolve("dev-home")
    val home = Files.createDirectories(root.resolve("g/1/idea"))
    val vmOptionsFile = Files.writeString(root.resolve("g/1/idea.vmoptions"), "-Didea.is.internal=true\n")
    val materializer = Files.writeString(temp.resolve("dev-home.sh"), """
      printf '%s\n' "${'$'}*" > "${'$'}INTELLIJ_DEV_HOME_ROOT/arguments"
      printf '{"home":"%s","launcher":"%s","vmOptionsFile":"%s","vmOptionsVariable":"IDEA_VM_OPTIONS"}\n' \
        "${'$'}INTELLIJ_DEV_HOME_ROOT/g/1/idea" "${'$'}INTELLIJ_DEV_HOME_ROOT/g/1/idea/bin/idea" "${'$'}INTELLIJ_DEV_HOME_ROOT/g/1/idea.vmoptions"
    """.trimIndent() + "\n")
    val launchFile = (IntellijDevLaunchFile.parse(HOME_LAUNCH_JSON) as IntellijDevHomeLaunch)
      .copy(materializer = listOf("/bin/sh", materializer.toString(), "materialize"))

    val launch = launchFile.launch(materializeIntellijDevHome(launchFile.materializer, mapOf("INTELLIJ_DEV_HOME_ROOT" to root.toString())))
    Files.readString(root.resolve("arguments")) shouldBe "materialize\n"
    launch.launcherSource.shouldBeNull()
    intellijDevLaunchCommand(launch, listOf("--extra")) shouldContainExactly listOf(home.resolve("bin/idea").toString(), "serverMode", "--extra")

    // Without extra options, the variable names the vmoptions file of the home, which the launch file does not name.
    val environment = intellijDevLaunchEnvironment(launch, mapOf("PATH" to "/usr/bin"), emptyMap(), "/work/idea", vmOptionsCopy = null)
    environment["IDEA_VM_OPTIONS"] shouldBe vmOptionsFile.toString()
    environment["RUNFILES_DIR"] shouldBe RUNFILES

    // The extra options go to a copy of the vmoptions file of the home.
    val copy = writeIntellijDevVmOptionsCopy(launch, listOf(jdwpVmOption(5005)), temp.resolve("copies"))
    Files.readString(copy) shouldBe "-Didea.is.internal=true\n${jdwpVmOption(5005)}\n"
    intellijDevLaunchEnvironment(launch, emptyMap(), emptyMap(), "/work/idea", copy)["IDEA_VM_OPTIONS"] shouldBe copy.toString()
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `a failed materializer gives its message`(@TempDir temp: Path) {
    val materializer = Files.writeString(temp.resolve("dev-home.sh"), "echo 'ERROR: cannot read /base/bin/build/idea.fingerprint' >&2\nexit 1\n")
    shouldThrow<IOException> {
      materializeIntellijDevHome(listOf("/bin/sh", materializer.toString()), emptyMap())
    }.message shouldBe "cannot read /base/bin/build/idea.fingerprint"
  }

  @Test
  fun `refuses a launch file with a missing or wrong field`() {
    shouldThrow<IllegalArgumentException> {
      parseV3(LAUNCH_JSON.replace("\"vmOptionsVariable\": \"IDEA_VM_OPTIONS\",", ""))
    }.message shouldContain "`vmOptionsVariable` is missing"
    shouldThrow<IllegalArgumentException> {
      parseV3(LAUNCH_JSON.replace("\"programArguments\": []", "\"programArguments\": \"a\""))
    }.message shouldContain "`programArguments` is not an array"
    shouldThrow<IllegalArgumentException> {
      parseV3(LAUNCH_JSON.replace("\"RUNFILES_DIR\": \"$RUNFILES\"", "\"RUNFILES_DIR\": 1"))
    }.message shouldContain "`env.RUNFILES_DIR` is not a string"
  }

  @Test
  fun `puts the launcher first, then the program arguments of the target, then the extra program arguments`() {
    val launch = parseV3(LAUNCH_JSON).copy(programArguments = listOf("serverMode"))
    intellijDevLaunchCommand(launch, listOf("--extra")) shouldContainExactly listOf(
      "$RUNFILES/ide_home/Contents/MacOS/idea",
      "serverMode",
      "--extra",
    )
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
  fun `the environment is the one of bazel run`() {
    val launch = parseV3(LAUNCH_JSON)
    val environment = intellijDevLaunchEnvironment(
      launch = launch,
      parentEnvironment = mapOf("PATH" to "/usr/bin", "RUNFILES_MANIFEST_FILE" to "/stale/MANIFEST", "JAVA_RUNFILES" to "/stale"),
      runConfigurationEnvironment = mapOf("CWM_NO_TIMEOUTS" to "1", "RUNFILES_DIR" to "/user"),
      workspaceRoot = "/work/idea",
      vmOptionsCopy = null,
    )
    environment shouldContainExactly mapOf(
      "PATH" to "/usr/bin",
      "BUILD_WORKING_DIRECTORY" to "/work/idea",
      "BUILD_WORKSPACE_DIRECTORY" to "/work/idea",
      "IDEA_VM_OPTIONS" to "/base/bin/build/idea.vmoptions",
      "JAVA_RUNFILES" to RUNFILES,
      "RUNFILES_DIR" to "/user",
      "CWM_NO_TIMEOUTS" to "1",
    )
  }

  @Test
  fun `the vmoptions copy wins over the variable of the run configuration`() {
    val launch = parseV3(LAUNCH_JSON)
    val environment = intellijDevLaunchEnvironment(
      launch = launch,
      parentEnvironment = emptyMap(),
      runConfigurationEnvironment = mapOf("IDEA_VM_OPTIONS" to "/mine.vmoptions"),
      workspaceRoot = "/work/idea",
      vmOptionsCopy = Path.of("/system/bazel-dev-launch/idea-1.vmoptions"),
    )
    environment["IDEA_VM_OPTIONS"] shouldBe Path.of("/system/bazel-dev-launch/idea-1.vmoptions").toString()
  }

  @Test
  fun `the extra VM options go to a copy of the vmoptions file`(@TempDir temp: Path) {
    val vmOptionsFile = Files.writeString(temp.resolve("idea.vmoptions"), "-Dawt.toolkit.name=auto\n-Didea.is.internal=true")
    val launch = parseV3(LAUNCH_JSON).copy(vmOptionsFile = vmOptionsFile.toString())
    val agent = "-javaagent:/my caches/debugger-agent.jar=file:/my caches/capture.props"
    val directory = temp.resolve("copies")

    val copy = writeIntellijDevVmOptionsCopy(launch, listOf(jdwpVmOption(5005), agent), directory)
    copy.parent shouldBe directory
    copy.fileName.toString() shouldStartWith "idea-"
    Files.readString(copy) shouldBe "-Dawt.toolkit.name=auto\n-Didea.is.internal=true\n${jdwpVmOption(5005)}\n$agent\n"
    // The file of the target stays as it is.
    Files.readString(vmOptionsFile) shouldBe "-Dawt.toolkit.name=auto\n-Didea.is.internal=true"

    // The next start writes the same copy again, and the directory holds no temporary file.
    writeIntellijDevVmOptionsCopy(launch, listOf(jdwpVmOption(6006)), directory) shouldBe copy
    Files.readString(copy) shouldBe "-Dawt.toolkit.name=auto\n-Didea.is.internal=true\n${jdwpVmOption(6006)}\n"
    Files.list(directory).use { files -> files.map { it.fileName.toString() }.toList() } shouldContainExactly listOf(copy.fileName.toString())

    // The target of the same name in another checkout gets its own copy.
    val otherFile = Files.writeString(Files.createDirectories(temp.resolve("other")).resolve("idea.vmoptions"), "-Dother=true\n")
    writeIntellijDevVmOptionsCopy(launch.copy(vmOptionsFile = otherFile.toString()), listOf(jdwpVmOption(5005)), directory) shouldNotBe copy
  }

  @Test
  fun `refuses an extra VM option that is not one line`(@TempDir temp: Path) {
    val vmOptionsFile = Files.writeString(temp.resolve("idea.vmoptions"), "")
    val launch = parseV3(LAUNCH_JSON).copy(vmOptionsFile = vmOptionsFile.toString())
    for (option in listOf("-Da=1\n-Db=2", " -Da=1", "# -Da=1", "")) {
      shouldThrow<IllegalArgumentException> {
        writeIntellijDevVmOptionsCopy(launch, listOf(option), temp.resolve("copies"))
      }
    }
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `the launcher link becomes a copy of the launcher source`(@TempDir temp: Path) {
    val source = Files.writeString(temp.resolve("idea-source"), "launcher bytes")
    val launcher = Files.createSymbolicLink(temp.resolve("idea"), source)
    val launch = parseV3(LAUNCH_JSON).copy(launcher = launcher.toString(), launcherSource = source.toString())

    ensureIntellijDevLauncherFile(launch)
    Files.isSymbolicLink(launcher) shouldBe false
    Files.readString(launcher) shouldBe "launcher bytes"
    Files.isExecutable(launcher) shouldBe true

    // A copy with the same bytes stays.
    val modified = Files.getLastModifiedTime(launcher)
    ensureIntellijDevLauncherFile(launch)
    Files.getLastModifiedTime(launcher) shouldBe modified

    // A new launcher build replaces the copy.
    Files.writeString(source, "new launcher bytes")
    ensureIntellijDevLauncherFile(launch)
    Files.readString(launcher) shouldBe "new launcher bytes"
    Files.list(temp).use { files -> files.map { it.fileName.toString() }.sorted().toList() } shouldContainExactly listOf("idea", "idea-source")
  }

  @Test
  fun `a launch without the launcher source keeps the launcher`(@TempDir temp: Path) {
    val launch = parseV3(LAUNCH_JSON).copy(launcher = temp.resolve("missing").toString())
    ensureIntellijDevLauncherFile(launch)
    Files.exists(temp.resolve("missing")) shouldBe false
  }
}

private fun parseV3(text: String): IntellijDevLaunch = IntellijDevLaunchFile.parse(text) as IntellijDevLaunch
