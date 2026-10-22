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
import java.nio.file.attribute.PosixFilePermissions

/** The rule kind of a dev target that starts the native launcher of a composed dev distribution. */
internal const val INTELLIJ_DEV_JAVA_LAUNCHER_KIND: String = "intellij_dev_java_launcher"

/** The file name suffix of the launch file that `intellij_dev_java_launcher` writes next to the executable. */
internal const val INTELLIJ_DEV_LAUNCH_FILE_SUFFIX: String = ".launch.json"

private const val LAUNCH_FILE_VERSION = 3

/**
 * The variables that `bazel run` removes from the environment of the executable.
 * The launch states its own `JAVA_RUNFILES` and `RUNFILES_DIR`.
 */
private val BAZEL_RUN_REMOVED_VARIABLES = listOf("JAVA_RUNFILES", "RUNFILES_DIR", "RUNFILES_MANIFEST_FILE", "RUNFILES_MANIFEST_ONLY", "TEST_SRCDIR")

/**
 * The content of `<name>.launch.json`, which `intellij_dev_java_launcher` in `community/build/intellij_dev.bzl` writes.
 * Each path is absolute.
 *
 * Monorepo DevKit has its own reader of the same file, `DevLaunch` in `plugins/monorepo-devkit/bazel`, because the two
 * plugins have separate versions. Change both readers together.
 *
 * @property launcher the production native launcher in the home of the target.
 * @property vmOptionsFile the user vmoptions file of the target. It holds the JVM flags of the target, one per line.
 * @property vmOptionsVariable the variable `<ENV>_VM_OPTIONS` that names [vmOptionsFile] for the launcher.
 * @property launcherSource the Bazel output of the launcher, on Linux only. [launcher] must be a real file with its bytes,
 *   because the launcher resolves the links of its own path (IJPL-258306).
 * @property programArguments the program arguments of the target.
 * @property workingDirectory the directory where `bazel run` starts the executable.
 * @property environment the environment of `RunEnvironmentInfo`. It names [vmOptionsFile] in [vmOptionsVariable].
 */
@ApiStatus.Internal
data class IntellijDevLaunch(
  val launcher: String,
  val vmOptionsFile: String,
  val vmOptionsVariable: String,
  val launcherSource: String?,
  val programArguments: List<String>,
  val runfilesDirectory: String,
  val workingDirectory: String,
  val environment: Map<String, String>,
) {
  companion object {
    /**
     * Reads the launch file [text].
     * It throws [IllegalArgumentException] when the text is not a launch file of version [LAUNCH_FILE_VERSION].
     * A target that an older checkout built has version 2, so the message names the version.
     */
    fun parse(text: String): IntellijDevLaunch {
      val root = JsonParser.parseString(text)
      require(root.isJsonObject) { "the root is not an object" }
      val launch = root.asJsonObject
      val version = launch.get("version")
      require(version != null && version.isJsonPrimitive && version.asJsonPrimitive.isNumber && version.asDouble == LAUNCH_FILE_VERSION.toDouble()) {
        "the version is ${version ?: "missing"}, not $LAUNCH_FILE_VERSION. Update the checkout and build the target again"
      }
      return IntellijDevLaunch(
        launcher = launch.string("launcher"),
        vmOptionsFile = launch.string("vmOptionsFile"),
        vmOptionsVariable = launch.string("vmOptionsVariable"),
        launcherSource = launch.get("launcherSource")?.let { launch.string("launcherSource") },
        programArguments = launch.member("programArguments").let { element ->
          require(element.isJsonArray) { "`programArguments` is not an array" }
          element.asJsonArray.mapIndexed { index, value -> value.stringValue("programArguments[$index]") }
        },
        runfilesDirectory = launch.string("runfilesDirectory"),
        workingDirectory = launch.string("workingDirectory"),
        environment = launch.member("env").let { element ->
          require(element.isJsonObject) { "`env` is not an object" }
          element.asJsonObject.entrySet().associate { (name, value) -> name to value.stringValue("env.$name") }
        },
      )
    }
  }
}

/** The JDWP agent option. The JVM waits for a debugger on [port] of the local host. */
@ApiStatus.Internal
fun jdwpVmOption(port: Int): String = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port"

/** The command line of [launch]: the launcher, the program arguments of the target, then [extraProgramArguments]. */
@ApiStatus.Internal
fun intellijDevLaunchCommand(launch: IntellijDevLaunch, extraProgramArguments: List<String>): List<String> =
  buildList {
    add(launch.launcher)
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
 * [runConfigurationEnvironment] overrides the environment of the launch, which overrides [parentEnvironment].
 * With [vmOptionsCopy], the variable of the launch names the copy, so the extra VM options always apply.
 */
@ApiStatus.Internal
fun intellijDevLaunchEnvironment(
  launch: IntellijDevLaunch,
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
  result.putAll(runConfigurationEnvironment)
  if (vmOptionsCopy != null) {
    result[launch.vmOptionsVariable] = vmOptionsCopy.toString()
  }
  return result
}

/**
 * Writes the vmoptions file of [launch] and then [extraVmOptions], one per line, into the copy of the target in [directory].
 *
 * The launcher reads the JVM options only from files and from the environment, not from its command line.
 * So the debug options reach the JVM through this copy, and the variable of the launch names it.
 * The copy acts as the user vmoptions file of a release, where a developer adds the JDWP option to debug the release.
 * Each target has one copy, and each start with extra options writes it again. The copy stays after the process ends.
 * A restart of the IDE starts a child launcher, which inherits the variable. So the restart gets the same JVM options.
 *
 * The name of the copy is the name of the vmoptions file and a hash of its path.
 * So two checkouts with a target of the same name do not share a copy.
 * The function replaces the copy in one move, so a restart that reads it never sees half a file.
 * The launcher trims each line and skips an empty line and a `#` line, so such an option fails here.
 */
@ApiStatus.Internal
fun writeIntellijDevVmOptionsCopy(launch: IntellijDevLaunch, extraVmOptions: List<String>, directory: Path): Path {
  for (option in extraVmOptions) {
    require(option.isNotEmpty() && option == option.trim() && !option.startsWith("#") && '\n' !in option && '\r' !in option) {
      "the VM option $option cannot be one line of a vmoptions file"
    }
  }
  val text = buildString {
    append(Files.readString(Path.of(launch.vmOptionsFile)))
    if (isNotEmpty() && !endsWith('\n')) {
      append('\n')
    }
    for (option in extraVmOptions) {
      append(option).append('\n')
    }
  }
  Files.createDirectories(directory)
  val name = Path.of(launch.vmOptionsFile).fileName.toString().removeSuffix(".vmoptions")
  val copy = directory.resolve("$name-${Integer.toHexString(launch.vmOptionsFile.hashCode())}.vmoptions")
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

/**
 * Makes the launcher of [launch] a real file with the bytes of `launcherSource`, as the Linux stub of the target does.
 * The launcher reads its path from `/proc/self/exe`, which resolves every link. So from a link it finds no home.
 * Bazel restores the link when the runfiles manifest changes. Then this function copies the file again.
 * A launch without `launcherSource`, on macOS and Windows, needs no copy. IJPL-258306 removes this step.
 */
@ApiStatus.Internal
fun ensureIntellijDevLauncherFile(launch: IntellijDevLaunch) {
  val source = Path.of(launch.launcherSource ?: return)
  val launcher = Path.of(launch.launcher)
  if (!Files.isSymbolicLink(launcher) && Files.isRegularFile(launcher) && Files.mismatch(source, launcher) == -1L) {
    return
  }
  val temporary = launcher.resolveSibling("${launcher.fileName}.tmp.${ProcessHandle.current().pid()}")
  try {
    Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING)
    Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rwxr-xr-x"))
    Files.move(temporary, launcher, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
  }
  catch (e: IOException) {
    Files.deleteIfExists(temporary)
    throw e
  }
}

private fun JsonObject.member(name: String): JsonElement = requireNotNull(get(name)) { "`$name` is missing" }

private fun JsonObject.string(name: String): String = member(name).stringValue(name)

private fun JsonElement.stringValue(name: String): String {
  require(isJsonPrimitive && asJsonPrimitive.isString) { "`$name` is not a string" }
  return asString
}
