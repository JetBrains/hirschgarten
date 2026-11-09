@file:ApiStatus.Internal

package org.jetbrains.bazel.intellij

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.label.toPath
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.concurrent.thread

/** The rule kind of a dev target that starts the native launcher of a composed dev distribution. */
internal const val INTELLIJ_DEV_JAVA_LAUNCHER_KIND: String = "intellij_dev_java_launcher"

/** The file name suffix of the launch file that `intellij_dev_java_launcher` writes next to the executable. */
internal const val INTELLIJ_DEV_LAUNCH_FILE_SUFFIX: String = ".launch.json"

/** The version of the launch file that this reader reads. */
private const val LAUNCH_FILE_VERSION = 4

/** The prefix of the failure line that the materializer prints on stderr. */
private const val MATERIALIZER_ERROR_PREFIX = "ERROR: "

/**
 * The variables that `bazel run` removes from the environment of the executable.
 * The launch states its own `JAVA_RUNFILES` and `RUNFILES_DIR`.
 */
private val BAZEL_RUN_REMOVED_VARIABLES = listOf("JAVA_RUNFILES", "RUNFILES_DIR", "RUNFILES_MANIFEST_FILE", "RUNFILES_MANIFEST_ONLY", "TEST_SRCDIR")

/**
 * The content of `<name>.launch.json` of version 4, which `intellij_dev_java_launcher` in `community/build/intellij_dev.bzl` writes.
 * Each path is absolute.
 * The home of the target is a generation of hard links outside the Bazel output base, which [materializer] makes.
 * So Bazel can build the target again while its IDE runs.
 *
 * Monorepo DevKit has its own reader of the same file, `DevLaunch` in `plugins/monorepo-devkit/bazel`, because the two
 * plugins have separate versions. Change both readers together.
 *
 * @property materializer the command that makes the home and prints it, see [materializeIntellijDevHome].
 * @property programArguments the program arguments of the target.
 * @property workingDirectory the directory where `bazel run` starts the executable.
 * @property environment the environment of `RunEnvironmentInfo`. It does not name the vmoptions file, because the materializer gives it.
 */
@ApiStatus.Internal
data class IntellijDevLaunch(
  val materializer: List<String>,
  val programArguments: List<String>,
  val workingDirectory: String,
  val environment: Map<String, String>,
) {
  companion object {
    /**
     * Reads the launch file [text].
     * It throws [IllegalArgumentException] when the text is not a launch file of version 4.
     * A target that a checkout before version 4 built has version 3, so the message names the version.
     */
    fun parse(text: String): IntellijDevLaunch {
      val root = JsonParser.parseString(text)
      require(root.isJsonObject) { "the root is not an object" }
      val launch = root.asJsonObject
      val version = launch.get("version")
      require(version != null && version.isJsonPrimitive && version.asJsonPrimitive.isNumber && version.asDouble == LAUNCH_FILE_VERSION.toDouble()) {
        "the version is ${version ?: "missing"}, not $LAUNCH_FILE_VERSION, so the checkout predates launch file version $LAUNCH_FILE_VERSION. " +
        "Update the checkout and build the target again"
      }
      return IntellijDevLaunch(
        materializer = launch.stringArray("materializer").also { require(it.isNotEmpty()) { "`materializer` is empty" } },
        programArguments = launch.stringArray("programArguments"),
        workingDirectory = launch.string("workingDirectory"),
        environment = launch.environment(),
      )
    }
  }
}

/**
 * The home that the materializer of an [IntellijDevLaunch] makes. Each path is absolute.
 *
 * @property home the directory of the home.
 * @property launcher the production native launcher in [home].
 * @property vmOptionsFile the vmoptions file of the home. It holds the JVM flags of the target, one per line.
 * @property vmOptionsVariable the variable `<ENV>_VM_OPTIONS` that names [vmOptionsFile] for the launcher.
 */
@ApiStatus.Internal
data class IntellijDevHome(
  val home: String,
  val launcher: String,
  val vmOptionsFile: String,
  val vmOptionsVariable: String,
) {
  companion object {
    /** Reads the answer [text] of the materializer. It throws [IllegalArgumentException] when the text is not such an answer. */
    fun parse(text: String): IntellijDevHome {
      val root = try {
        JsonParser.parseString(text)
      }
      catch (e: RuntimeException) {
        throw IllegalArgumentException("the text is not JSON: ${e.message}", e)
      }
      require(root.isJsonObject) { "the root is not an object" }
      val answer = root.asJsonObject
      return IntellijDevHome(
        home = answer.string("home"),
        launcher = answer.string("launcher"),
        vmOptionsFile = answer.string("vmOptionsFile"),
        vmOptionsVariable = answer.string("vmOptionsVariable"),
      )
    }
  }
}

/**
 * Runs the materializer [command] of an [IntellijDevLaunch] with [environment] and returns the home that it prints.
 * The materializer reads `INTELLIJ_DEV_HOME_ROOT` of [environment] when it is set.
 * It throws [IOException] when the materializer cannot start, fails, or prints no home.
 * A failure has the message of the `ERROR:` line of the materializer, so the user sees why the home is missing.
 * The call blocks until the materializer exits. An interrupt of the thread stops the materializer.
 */
@ApiStatus.Internal
fun materializeIntellijDevHome(command: List<String>, environment: Map<String, String>): IntellijDevHome {
  val builder = ProcessBuilder(command)
  builder.environment().clear()
  builder.environment().putAll(environment)
  val process = try {
    builder.start()
  }
  catch (e: IOException) {
    throw IOException("cannot start the materializer ${command.first()}: ${e.message}", e)
  }
  try {
    process.outputStream.close()
    var errors = ""
    // The materializer writes to both streams. A second reader keeps a full stderr pipe from blocking it.
    val errorReader = thread(name = "dev home materializer stderr") {
      errors = try {
        process.errorStream.readAllBytes().decodeToString()
      }
      catch (_: IOException) {
        ""
      }
    }
    val output = process.inputStream.readAllBytes().decodeToString()
    val exitCode = process.waitFor()
    errorReader.join()
    return intellijDevHomeOf(exitCode, output, errors)
  }
  finally {
    process.destroyForcibly()
  }
}

/**
 * The home that the materializer printed in [output] when it exits with [exitCode] 0.
 * Otherwise it throws [IOException] with the text of the `ERROR:` line in [errors], or with the exit code when there is no such line.
 */
@ApiStatus.Internal
fun intellijDevHomeOf(exitCode: Int, output: String, errors: String): IntellijDevHome {
  if (exitCode != 0) {
    val message = errors.lines().lastOrNull { it.startsWith(MATERIALIZER_ERROR_PREFIX) }?.removePrefix(MATERIALIZER_ERROR_PREFIX)?.trim()
                  ?: errors.trim().ifEmpty { "the materializer exited with code $exitCode" }
    throw IOException(message)
  }
  try {
    return IntellijDevHome.parse(output)
  }
  catch (e: IllegalArgumentException) {
    throw IOException("the materializer printed no home: ${e.message}", e)
  }
}

/** The JDWP agent option. The JVM waits for a debugger on [port] of the local host. */
@ApiStatus.Internal
fun jdwpVmOption(port: Int): String = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port"

/** The command line of [launch]: the launcher of [home], the program arguments of the target, then [extraProgramArguments]. */
@ApiStatus.Internal
fun intellijDevLaunchCommand(launch: IntellijDevLaunch, home: IntellijDevHome, extraProgramArguments: List<String>): List<String> =
  buildList {
    add(home.launcher)
    addAll(launch.programArguments)
    addAll(extraProgramArguments)
  }

/**
 * The launch file of [target] below [bazelBin].
 * A target of the main repository has it in `bazel-bin/<package>`.
 * A target of an external repository has it in `bazel-bin/external/<canonical repository name>/<package>`.
 * [apparentRepoNameToCanonicalName] gives the canonical name of an apparent repository name.
 * The result is `null` when [target] is not a resolved label, or when the mapping does not know its repository.
 */
@ApiStatus.Internal
fun intellijDevLaunchFile(bazelBin: Path, target: Label, apparentRepoNameToCanonicalName: Map<String, String>): Path? {
  val label = when (target) {
    is ResolvedLabel -> target
    is RelativeLabel, is SyntheticLabel -> return null
  }
  val canonicalRepoName = when (val repo = label.repo) {
    is Main -> ""
    is Canonical -> repo.repoName
    is Apparent -> apparentRepoNameToCanonicalName[repo.repoName] ?: return null
  }
  val repositoryDirectory = if (canonicalRepoName.isEmpty()) bazelBin else bazelBin.resolve("external").resolve(canonicalRepoName)
  return repositoryDirectory.resolve(label.packagePath.toPath()).resolve(label.targetName + INTELLIJ_DEV_LAUNCH_FILE_SUFFIX)
}

/**
 * The environment of [launch], as `bazel run` gives it to the executable.
 * [runConfigurationEnvironment] overrides the environment of the launch and the variable of [home] that names its vmoptions file.
 * They override [parentEnvironment].
 * With [vmOptionsCopy], the variable of [home] names the copy, so the extra VM options always apply.
 */
@ApiStatus.Internal
fun intellijDevLaunchEnvironment(
  launch: IntellijDevLaunch,
  home: IntellijDevHome,
  parentEnvironment: Map<String, String>,
  runConfigurationEnvironment: Map<String, String>,
  workspaceRoot: String,
  vmOptionsCopy: Path?,
): Map<String, String> {
  val result = LinkedHashMap(parentEnvironment)
  for (name in BAZEL_RUN_REMOVED_VARIABLES) {
    result.remove(name)
  }
  result["BUILD_WORKING_DIRECTORY"] = workspaceRoot
  result["BUILD_WORKSPACE_DIRECTORY"] = workspaceRoot
  result.putAll(launch.environment)
  result[home.vmOptionsVariable] = home.vmOptionsFile
  result.putAll(runConfigurationEnvironment)
  if (vmOptionsCopy != null) {
    result[home.vmOptionsVariable] = vmOptionsCopy.toString()
  }
  return result
}

/**
 * Writes the vmoptions file of [home] and then [extraVmOptions], one per line, into the copy of the target of [launch] in [directory].
 *
 * The launcher reads the JVM options only from files and from the environment, not from its command line.
 * So the debug options reach the JVM through this copy, and the variable of [home] names it.
 * The copy acts as the user vmoptions file of a release, where a developer adds the JDWP option to debug the release.
 * Each target has one copy, and each start with extra options writes it again. The copy stays after the process ends.
 * A restart of the IDE starts a child launcher, which inherits the variable. So the restart gets the same JVM options.
 *
 * The name of the copy is the variable of [home] without `_VM_OPTIONS`, in lower case, and a hash of the materializer command.
 * Each generation has another vmoptions file, but the command names the home spec of the target in its checkout.
 * So each target has one copy, and two checkouts with a target of the same name do not share a copy.
 * The function replaces the copy in one move, so a restart that reads it never sees half a file.
 * The launcher trims each line and skips an empty line and a `#` line, so such an option fails here.
 */
@ApiStatus.Internal
fun writeIntellijDevVmOptionsCopy(launch: IntellijDevLaunch, home: IntellijDevHome, extraVmOptions: List<String>, directory: Path): Path {
  for (option in extraVmOptions) {
    require(option.isNotEmpty() && option == option.trim() && !option.startsWith("#") && '\n' !in option && '\r' !in option) {
      "the VM option $option cannot be one line of a vmoptions file"
    }
  }
  val text = buildString {
    append(Files.readString(Path.of(home.vmOptionsFile)))
    if (isNotEmpty() && !endsWith('\n')) {
      append('\n')
    }
    for (option in extraVmOptions) {
      append(option).append('\n')
    }
  }
  Files.createDirectories(directory)
  val name = home.vmOptionsVariable.removeSuffix("_VM_OPTIONS").lowercase() + "-" +
             Integer.toHexString(launch.materializer.joinToString("\u0000").hashCode())
  val copy = directory.resolve("$name.vmoptions")
  val temporary = Files.createTempFile(directory, "$name-", ".tmp")
  try {
    Files.writeString(temporary, text)
    Files.move(temporary, copy, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
  }
  catch (e: IOException) {
    Files.deleteIfExists(temporary)
    throw e
  }
  return copy
}

private fun JsonObject.member(name: String): JsonElement = requireNotNull(get(name)) { "`$name` is missing" }

private fun JsonObject.string(name: String): String = member(name).stringValue(name)

private fun JsonObject.stringArray(name: String): List<String> {
  val element = member(name)
  require(element.isJsonArray) { "`$name` is not an array" }
  return element.asJsonArray.mapIndexed { index, value -> value.stringValue("$name[$index]") }
}

private fun JsonObject.environment(): Map<String, String> {
  val element = member("env")
  require(element.isJsonObject) { "`env` is not an object" }
  return element.asJsonObject.entrySet().associate { (name, value) -> name to value.stringValue("env.$name") }
}

private fun JsonElement.stringValue(name: String): String {
  require(isJsonPrimitive && asJsonPrimitive.isString) { "`$name` is not a string" }
  return asString
}
