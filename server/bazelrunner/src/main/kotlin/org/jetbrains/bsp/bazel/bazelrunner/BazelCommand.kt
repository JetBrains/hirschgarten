package org.jetbrains.bsp.bazel.bazelrunner

import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.commons.utils.OsFamily
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import java.nio.file.Path

interface HasProgramArguments {
  // Will be forwarded directly to `bazel run <target> -- <arguments>` or set using `--test_arg=<arg>` for `bazel test`
  val programArguments: MutableList<String>
}

interface HasAdditionalBazelOptions {
  // Will be forwarded directly to `bazel <command> <options>`
  val additionalBazelOptions: MutableList<String>
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
  val targets: MutableList<Label>

  // Will be added as `bazel <command> -- <targets> -target1 -target2 ...`
  val excludedTargets: MutableList<Label>

  fun addTargetsFromSpec(targetsSpec: TargetsSpec) {
    targets.addAll(targetsSpec.values)
    excludedTargets.addAll(targetsSpec.excludedValues)
  }

  fun targetCommandLine(): List<String> {
    val commandLine = mutableListOf("--")
    commandLine.addAll(targets.map { it.toString() })
    commandLine.addAll(excludedTargets.map { "-$it" })
    return commandLine
  }
}

interface HasSingleTarget {
  val target: Label
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
        "--build_event_publish_all_actions",
      ),
    )
  }

  class Run(bazelBinary: String, override val target: Label) :
    BazelCommand(bazelBinary),
    HasProgramArguments,
    HasEnvironment,
    HasSingleTarget,
    HasWorkingDirectory,
    HasAdditionalBazelOptions {
    override val programArguments: MutableList<String> = mutableListOf()
    override val environment: MutableMap<String, String> = mutableMapOf()
    override val inheritedEnvironment: MutableList<String> = mutableListOf()
    override var workingDirectory: Path? = null
    override val additionalBazelOptions: MutableList<String> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf<String>()

      commandLine.add(bazelBinary)
      commandLine.addAll(startupOptions)
      commandLine.add("run")
      commandLine.addAll(options)
      commandLine.addAll(additionalBazelOptions)
      commandLine.add(target.toString())

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
    override val targets: MutableList<Label> = mutableListOf()
    override val excludedTargets: MutableList<Label> = mutableListOf()
    override val environment: MutableMap<String, String> = mutableMapOf()
    override val inheritedEnvironment: MutableList<String> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf(bazelBinary)

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
    HasProgramArguments,
    HasAdditionalBazelOptions {
    override val targets: MutableList<Label> = mutableListOf()
    override val excludedTargets: MutableList<Label> = mutableListOf()
    override val environment: MutableMap<String, String> = mutableMapOf()
    override val inheritedEnvironment: MutableList<String> = mutableListOf()
    override val programArguments: MutableList<String> = mutableListOf()
    override val additionalBazelOptions: MutableList<String> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("test")
      commandLine.addAll(options)
      commandLine.addAll(additionalBazelOptions)
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
    override val targets: MutableList<Label> = mutableListOf()
    override val excludedTargets: MutableList<Label> = mutableListOf()
    override val environment: MutableMap<String, String> = mutableMapOf()
    override val inheritedEnvironment: MutableList<String> = mutableListOf()
    override val programArguments: MutableList<String> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf(bazelBinary)

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
  class MobileInstall(bazelBinary: String, override val target: Label) :
    BazelCommand(bazelBinary),
    HasSingleTarget {
    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("mobile-install")
      commandLine.addAll(options)
      commandLine.add(target.toString())

      return commandLine
    }
  }

  class Query(bazelBinary: String, private val allowManualTargetsSync: Boolean) :
    BazelCommand(bazelBinary),
    HasMultipleTargets {
    override val targets: MutableList<Label> = mutableListOf()
    override val excludedTargets: MutableList<Label> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("query")
      commandLine.addAll(options)
      commandLine.add(queryString(allowManualTargetsSync))

      return commandLine
    }

    fun queryString(allowManualTargetsSync: Boolean): String {
      if (targets.isEmpty()) return ""
      val includesString = targets.joinToString(separator = " + ")
      val excludesString = excludedTargets.joinToString(separator = " - ")
      val targetString = if (excludesString.isEmpty()) includesString else "$includesString - $excludesString"
      return if (allowManualTargetsSync) {
        targetString
      } else {
        excludeManualTargetsQueryString(targetString)
      }
    }

    private fun excludeManualTargetsQueryString(targetString: String): String =
      if (OsFamily.inferFromSystem() == OsFamily.WINDOWS) {
        "attr('tags', '^((?!manual).)*$', $targetString)"
      } else {
        "attr(\"tags\", \"^((?!manual).)*$\", $targetString)"
      }
  }

  class CQuery(bazelBinary: String) :
    BazelCommand(bazelBinary),
    HasMultipleTargets {
    override val targets: MutableList<Label> = mutableListOf()
    override val excludedTargets: MutableList<Label> = mutableListOf()

    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.add("cquery")
      commandLine.addAll(options)
      commandLine.addAll(targetCommandLine())

      return commandLine
    }
  }

  abstract class SimpleCommand(bazelBinary: String, val command: List<String>) : BazelCommand(bazelBinary) {
    override fun makeCommandLine(): List<String> {
      val commandLine = mutableListOf(bazelBinary)

      commandLine.addAll(startupOptions)
      commandLine.addAll(command)
      commandLine.addAll(options)

      return commandLine
    }
  }

  class Version(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("version"))

  class Info(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("info"))

  class Clean(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("clean"))

  class ModGraph(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("mod", "graph"))

  class ModPath(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("mod", "path"))

  class ModShowRepo(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("mod", "show_repo"))

  class ModDumpRepoMapping(bazelBinary: String) : SimpleCommand(bazelBinary, listOf("mod", "dump_repo_mapping"))

  class FileQuery(bazelBinary: String, filePath: String) : SimpleCommand(bazelBinary, listOf("query", filePath))
}
