package org.jetbrains.bazel.cpp.sync.xcode

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.cpp.sync.configuration.BazelConfigurationToolchainResolver
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.io.File
import java.nio.file.Path

class XCodeCompilerSettingProviderImpl : XCodeCompilerSettingProvider {
  companion object {
    private val logger: Logger = Logger.getInstance(BazelConfigurationToolchainResolver::class.java)
  }

  val queryXcodeVersionStarlarkFile = """
def format(target):
    return "{} {}".format(providers(target)["XcodeProperties"].xcode_version, providers(target)["XcodeProperties"].default_macos_sdk_version) if providers(target) and "XcodeProperties" in providers(target) else ""
  """

  val queryXcodeTarget = "deps(\"@bazel_tools//tools/osx:current_xcode_config\")"

  override fun fromContext(bazelRunner: BazelRunner, workspaceContext: WorkspaceContext): XCodeCompilerSettings? {
    val (xcodeVersion, sdkVersion) = runBlocking { queryXcodeAndSDKVersion(bazelRunner, workspaceContext) } ?: return null
    if (xcodeVersion.isBlank() || xcodeVersion == "None") {
      return queryXcodeCommandLineSetting(sdkVersion)
    } else {
      return runBlocking { queryXcodeAppSetting(xcodeVersion, sdkVersion, bazelRunner, workspaceContext) }
    }
  }

  private suspend fun queryXcodeAndSDKVersion(bazelRunner: BazelRunner, workspaceContext: WorkspaceContext): Pair<String, String>? {
    val bazelCompilerWrapper =
      File.createTempFile("xcode_query", ".cquery")
    bazelCompilerWrapper.writeText(queryXcodeVersionStarlarkFile)
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        cquery {
          options.add(queryXcodeTarget)
          options.add("--output=starlark")
          options.add("--starlark:file=${bazelCompilerWrapper.toPath().toAbsolutePath()}")
        }
      }
    val cqueryResult =
      bazelRunner
        .runBazelCommand(command, serverPidFuture = null, shouldLogInvocation = false)
        .waitAndGetResult(ensureAllOutputRead = true)
    if (cqueryResult.isNotSuccess || cqueryResult.stdout == "") {
      return null
    }
    val output = cqueryResult.stdout
    for (line in output.split("\n")) {
      if (line.isBlank()) continue
      val versions = line.split(' ')
      if (versions.size != 2) {
        logger.error("Unexpected version format: $line")
        return null
      }
      return Pair(versions[0], versions[1])
    }
    return null
  }

  private suspend fun queryXcodeAppSetting(
    xcodeVersion: String,
    sdkVersion: String,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): XCodeCompilerSettings {
    // query the development dir
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        run(Label.parse("@bazel_tools//tools/osx:xcode-locator")) {
          programArguments.add(xcodeVersion)
        }
      }
    val result =
      bazelRunner
        .runBazelCommand(command, serverPidFuture = null)
        .waitAndGetResult(ensureAllOutputRead = true)
    val developerDir = result.stdout.trim()
    return XCodeCompilerSettings(
      Path.of(developerDir),
      Path.of(
        developerDir,
        "Platforms",
        "MacOSX.platform",
        "Developer",
        "SDKs",
        "MacOSX$sdkVersion.sdk",
      ),
    )
  }

  private fun queryXcodeCommandLineSetting(sdkVersion: String): XCodeCompilerSettings? {
    val developerDir: Path = Path.of("/Library/Developer/CommandLineTools")
    // Some users might not have command line tools installed.
    // This should never trigger as Bazel requires that at least one of Xcode or CLTs are in the system to work.
    // However, *if* it triggers (e.g. because Bazel changes this requirement),
    // we want to handle the case.
    if (!developerDir.toFile().exists()) {
      return null
    }

    val settings = XCodeCompilerSettings(developerDir, developerDir.resolve("SDKs").resolve("MacOSX.sdk"))
    // now we verify whether there is a clang
    // Sometimes, a macOS upgrade can corrupt the installation of CommandLineTools.
    // We detect that we can at least run `clang --version`.
    val clang = Path.of(settings.developerDir.toString(), "usr", "bin", "clang")
    if (!clang.toFile().exists()) {
      logger.error("Non-existing clang path: $clang")
      return null
    }
    val returnValue = ProcessBuilder(clang.toAbsolutePath().toString(), "--version").start().waitFor()
    if (returnValue != 0) {
      logger.error("Failed to run clang --version, return value: $returnValue")
      return null
    }
    return settings
  }
}
