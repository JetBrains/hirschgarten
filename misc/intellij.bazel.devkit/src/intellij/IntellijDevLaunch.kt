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
import java.nio.file.Path

/** The rule kind of a dev row that starts `java` over a runfiles home. */
internal const val INTELLIJ_DEV_JAVA_LAUNCHER_KIND: String = "intellij_dev_java_launcher"

/** The file name suffix of the launch file that `intellij_dev_java_launcher` writes next to the executable. */
internal const val INTELLIJ_DEV_LAUNCH_FILE_SUFFIX: String = ".launch.json"

private const val LAUNCH_FILE_VERSION = 2

/**
 * The variables that `bazel run` removes from the environment of the executable.
 * The launch states its own `JAVA_RUNFILES` and `RUNFILES_DIR`.
 */
private val BAZEL_RUN_REMOVED_VARIABLES = listOf("JAVA_RUNFILES", "RUNFILES_DIR", "RUNFILES_MANIFEST_FILE", "RUNFILES_MANIFEST_ONLY", "TEST_SRCDIR")

/**
 * The content of `<name>.launch.json`, which `intellij_dev_java_launcher` in `community/build/intellij_dev.bzl` writes.
 * Each path is absolute.
 *
 * @property java the `java` executable of the Java runtime.
 * @property argfile the JVM argument file. It ends with the class path, the main class and the program arguments of the row.
 * @property workingDirectory the directory where `bazel run` starts the executable.
 * @property environment the environment of `RunEnvironmentInfo`.
 */
@ApiStatus.Internal
data class IntellijDevLaunch(
  val java: String,
  val argfile: String,
  val runfilesDirectory: String,
  val workingDirectory: String,
  val environment: Map<String, String>,
) {
  companion object {
    /**
     * Reads the launch file [text].
     * It throws [IllegalArgumentException] when the text is not a launch file of version [LAUNCH_FILE_VERSION].
     */
    fun parse(text: String): IntellijDevLaunch {
      val root = JsonParser.parseString(text)
      require(root.isJsonObject) { "the root is not an object" }
      val launch = root.asJsonObject
      val version = launch.get("version")
      require(version != null && version.isJsonPrimitive && version.asJsonPrimitive.isNumber && version.asInt == LAUNCH_FILE_VERSION) {
        "the version is not $LAUNCH_FILE_VERSION"
      }
      return IntellijDevLaunch(
        java = launch.string("java"),
        argfile = launch.string("argfile"),
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

/**
 * The command line of [launch]: `java`, then [vmOptions], then the argument file, then [extraProgramArguments].
 * The argument file ends with the main class and the program arguments of the row.
 * So each VM option comes before the argument file, and each extra program argument comes after the program arguments of the row.
 */
@ApiStatus.Internal
fun intellijDevLaunchCommand(launch: IntellijDevLaunch, vmOptions: List<String>, extraProgramArguments: List<String>): List<String> =
  buildList {
    add(launch.java)
    addAll(vmOptions)
    add("@${launch.argfile}")
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
 */
@ApiStatus.Internal
fun intellijDevLaunchEnvironment(
  launch: IntellijDevLaunch,
  parentEnvironment: Map<String, String>,
  runConfigurationEnvironment: Map<String, String>,
  workspaceRoot: String,
): Map<String, String> {
  val result = LinkedHashMap(parentEnvironment)
  for (name in BAZEL_RUN_REMOVED_VARIABLES) {
    result.remove(name)
  }
  result["BUILD_WORKING_DIRECTORY"] = workspaceRoot
  result["BUILD_WORKSPACE_DIRECTORY"] = workspaceRoot
  result.putAll(launch.environment)
  result.putAll(runConfigurationEnvironment)
  return result
}

private fun JsonObject.member(name: String): JsonElement = requireNotNull(get(name)) { "`$name` is missing" }

private fun JsonObject.string(name: String): String = member(name).stringValue(name)

private fun JsonElement.stringValue(name: String): String {
  require(isJsonPrimitive && asJsonPrimitive.isString) { "`$name` is not a string" }
  return asString
}
