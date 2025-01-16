package org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain

import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import java.io.File
import java.nio.file.Path
import java.util.*

/**
 * Interface for fetching Xcode information from the system.
 * This has to be an interface and accompanying global service to allow mocking in tests.
 */
interface XCodeCompilerSettingProvider {
  fun fromContext(bazelRunner: BazelRunner): XCodeCompilerSettings?
}

class XCodeCompilerSettingProviderImpl : XCodeCompilerSettingProvider {
  val queryXcodeVersionStarlarkFile = """
def format(target):
    return "{} {}".format(providers(target)["XcodeProperties"].xcode_version, providers(target)["XcodeProperties"].default_macos_sdk_version) if providers(target) and "XcodeProperties" in providers(target) else ""
  """

  val queryXcodeTarget = "deps(\"@bazel_tools//tools/osx:current_xcode_config\")"

  override fun fromContext(bazelRunner: BazelRunner): XCodeCompilerSettings? {
    val (xcodeVersion, sdkVersion) = queryXcodeAndSDKVersion(bazelRunner) ?: return null
    if (xcodeVersion.isBlank() || xcodeVersion == "None") {
      return queryXcodeCommandLineSetting(sdkVersion)
    } else {
      return queryXcodeAppSetting(xcodeVersion, sdkVersion, bazelRunner)
    }

  }

  private fun queryXcodeAndSDKVersion(bazelRunner: BazelRunner): Pair<String, String>? {
    val bazelCompilerWrapper =
      File.createTempFile("xcode_query", ".cquery")
    bazelCompilerWrapper.writeText(queryXcodeVersionStarlarkFile)
    val command = bazelRunner.buildBazelCommand {
      cquery {
        options.add(queryXcodeTarget)
        options.add("--output=starlark")
        options.add("--starlark:file=${bazelCompilerWrapper.toPath().toAbsolutePath()}")
      }
    }
    val cqueryResult =
      bazelRunner
        .runBazelCommand(command, serverPidFuture = null, shouldLogInvocation = false)
        .waitAndGetResult({}, ensureAllOutputRead = true)
    if (cqueryResult.isNotSuccess || cqueryResult.stdout == "") {
      return null
    }
    val output = cqueryResult.stdout
    for (line in output.split("\n")) {
      if (line.isBlank()) continue
      val versions = line.split(' ')
      if (versions.size != 2) {
        // todo: log something here
        return null
      }
      return Pair(versions[0], versions[1])
    }
    return null
  }


  private fun queryXcodeAppSetting(
    xcodeVersion: String,
    sdkVersion: String,
    bazelRunner: BazelRunner
  ): XCodeCompilerSettings {
    //query the development dir
    val command = bazelRunner.buildBazelCommand {
      run(Label.parse("@bazel_tools//tools/osx:xcode-locator")) {
        programArguments.add(xcodeVersion)
      }
    }
    val result = bazelRunner.runBazelCommand(command, serverPidFuture = null)
      .waitAndGetResult({}, ensureAllOutputRead = true)
    val developerDir = result.stdout.trim()
    return XCodeCompilerSettings(
      Path.of(developerDir), Path.of(
        developerDir, "Platforms", "MacOSX.platform", "Developer", "SDKs",
        "MacOSX${sdkVersion}.sdk"
      )
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
    val clang= Path.of(settings.developerDir.toString(), "usr", "bin", "clang");
    if (!clang.toFile().exists()) {
      // todo: log something
      return null
    }
    val returnValue=ProcessBuilder(clang.toAbsolutePath().toString(), "--version").start().waitFor()
    if (returnValue != 0) {
      // todo: log something
      return null
    }
    return settings


  }
}

class XCodeCompilerSettings(val developerDir: Path, val sdkRootPath: Path) {
  fun asEnvironmentVariables(

  ): Map<String, String> {
    return mapOf(
      "DEVELOPER_DIR" to developerDir.toString(),
      "SDKROOT" to sdkRootPath.toString(),
    )
  }
}
