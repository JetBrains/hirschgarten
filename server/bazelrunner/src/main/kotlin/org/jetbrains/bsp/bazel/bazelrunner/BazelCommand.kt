package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import java.nio.file.Path

interface HasProgramArguments {
  // Will be forwarded directly to `bazel run <target> -- <arguments>` or set using `--test_arg=<arg>` for `bazel test`
  val programArguments: MutableList<String>
}

interface HasEnvironment {
  // In case of `bazel test`, it will be set using `--test_env=<name=value>`
  // In case of `bazel build`, it will be set using `--action_env=<name=value>`
  // `bazel run` needs to be set up inside the ProcessBuilder
  val environment: MutableMap<String, String>

  // In case of `bazel test, it will be set using `--test_env=<name>`
  // In case of `bazel build`, it will be set using `--action_env=<name>`
  // In case of `bazel run` the environment is always inherited
  // TODO: this is not used in the current implementation
  val inheritedEnvironment: List<String>
}

interface HasWorkingDirectory {
  var workingDirectory: Path?
}

interface HasMultipleTargets {
  // Will be added as `bazel <command> -- target1 target2 ...`
  val targets: MutableList<BuildTargetIdentifier>

  // Will be added as `bazel <command> -- <targets> -target1 -target2 ...`
  val excludedTargets: MutableList<BuildTargetIdentifier>

  fun addTargetsFromSpec(targetsSpec: TargetsSpec) {
    targets.addAll(targetsSpec.values)
    excludedTargets.addAll(targetsSpec.excludedValues)
  }

  fun targetCommandLine(): List<String> {
    val commandLine = mutableListOf<String>("--")
    commandLine.addAll(targets.map { it.uri })
    commandLine.addAll(excludedTargets.map { "-${it.uri}" })
    return commandLine
  }
}

interface HasSingleTarget {
  val target: BuildTargetIdentifier
}

// See https://bazel.build/reference/command-line-reference#commands
abstract class BazelCommand(val bazelBinary: String) {
  // See https://bazel.build/reference/command-line-reference#startup-options
  val startupOptions: MutableList<String> = mutableListOf()

  // See https://bazel.build/reference/command-line-reference#options-common-to-all-commands and command-specific options
  val options: MutableList<String> = mutableListOf(BazelFlag.toolTag())

  abstract fun makeCommandLine(): List<String>

  // See https://bazel.build/reference/command-line-reference#flag--build_event_binary_file
  fun useBes(besOutputFile: Path) {
    options.addAll(
      listOf(
        "--build_event_binary_file=${besOutputFile.toAbsolutePath()}",
        "--bes_outerr_buffer_size=10",
      ),
    )
  }

  class Run(bazelBinary: String, override val target: BuildTargetIdentifier) :
    BazelCommand(bazelBinary),
    HasProgramArguments,
    HasEnvironment,
    HasSingleTarget,
    HasWorkingDirectory {
    override val programArguments: MutableList<String> = mutableListOf()
    override val environment: MutableMap<String, String> = mutableMapOf()
    override val inheritedEnvironment: MutableList<String> = mutableListOf()
    override var workingDirectory: Path? = null

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>()

      commandLine.add(bazelBinary)
      commandLine.addAll(startupOptions)
      commandLine.add("run")
      commandLine.addAll(options)
      commandLine.add(target.uri)

      if (programArguments.isNotEmpty()) {
        commandLine.add("--")
        commandLine.addAll(programArguments)
      }

      return commandLine
    }
  }

  class Build(bazelBinary: String) :
    BazelCommand(bazelBinary),
    HasEnvironment,
    HasMultipleTargets {
    override val targets: MutableList<BuildTargetIdentifier> = mutableListOf()
    override val excludedTargets: MutableList<BuildTargetIdentifier> = mutableListOf()
    override val environment: MutableMap<String, String> = mutableMapOf()
    override val inheritedEnvironment: MutableList<String> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("build")
      commandLine.addAll(options)
      commandLine.addAll(environment.map { (key, value) -> "--action_env=$key=$value" })
      commandLine.addAll(inheritedEnvironment.map { "--action_env=$it" })
      commandLine.addAll(targetCommandLine())

      return commandLine
    }
  }

  class Test(bazelBinary: String) :
    BazelCommand(bazelBinary),
    HasEnvironment,
    HasMultipleTargets,
    HasProgramArguments {
    override val targets: MutableList<BuildTargetIdentifier> = mutableListOf()
    override val excludedTargets: MutableList<BuildTargetIdentifier> = mutableListOf()
    override val environment: MutableMap<String, String> = mutableMapOf()
    override val inheritedEnvironment: MutableList<String> = mutableListOf()
    override val programArguments: MutableList<String> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("test")
      commandLine.addAll(options)
      commandLine.addAll(environment.map { (key, value) -> "--test_env=$key=$value" })
      commandLine.addAll(inheritedEnvironment.map { "--test_env=$it" })
      commandLine.addAll(programArguments.map { "--test_arg=$it" })
      commandLine.addAll(targetCommandLine())

      return commandLine
    }
  }

  class Coverage(bazelBinary: String) :
    BazelCommand(bazelBinary),
    HasEnvironment,
    HasMultipleTargets,
    HasProgramArguments {
    override val targets: MutableList<BuildTargetIdentifier> = mutableListOf()
    override val excludedTargets: MutableList<BuildTargetIdentifier> = mutableListOf()
    override val environment: MutableMap<String, String> = mutableMapOf()
    override val inheritedEnvironment: MutableList<String> = mutableListOf()
    override val programArguments: MutableList<String> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("coverage")
      commandLine.addAll(options)
      commandLine.addAll(environment.map { (key, value) -> "--test_env=$key=$value" })
      commandLine.addAll(inheritedEnvironment.map { "--test_env=$it" })
      commandLine.addAll(programArguments.map { "--test_arg=$it" })
      commandLine.addAll(targetCommandLine())

      return commandLine
    }
  }

  // TODO: perhaps it's possible to install multiple targets at once?
  class MobileInstall(bazelBinary: String, override val target: BuildTargetIdentifier) :
    BazelCommand(bazelBinary),
    HasSingleTarget {
    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("mobile-install")
      commandLine.addAll(options)
      commandLine.add(target.uri)

      return commandLine
    }
  }

  class Query(bazelBinary: String) :
    BazelCommand(bazelBinary),
    HasMultipleTargets {
    override val targets: MutableList<BuildTargetIdentifier> = mutableListOf()
    override val excludedTargets: MutableList<BuildTargetIdentifier> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("query")
      commandLine.addAll(options)
      commandLine.addAll(targetCommandLine())

      return commandLine
    }
  }

  class CQuery(bazelBinary: String) :
    BazelCommand(bazelBinary),
    HasMultipleTargets {
    override val targets: MutableList<BuildTargetIdentifier> = mutableListOf()
    override val excludedTargets: MutableList<BuildTargetIdentifier> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("cquery")
      commandLine.addAll(options)
      commandLine.addAll(targetCommandLine())

      return commandLine
    }
  }

  abstract class SimpleCommand(bazelBinary: String, val command: List<String>) : BazelCommand(bazelBinary) {
    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.addAll(command)
      commandLine.addAll(options)

      return commandLine
    }
  }

  class Info(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("info"))

  class Clean(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("clean"))

  class ModGraph(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("mod", "graph"))

  class ModPath(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("mod", "path"))

  class ModShowRepo(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("mod", "show_repo"))
}
