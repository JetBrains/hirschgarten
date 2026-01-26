package org.jetbrains.bazel.server.sync

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.ExecUtils
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JvmToolchainInfo

object JvmToolchainQuery {
  suspend fun jvmToolchainQuery(
    bspInfo: BspInfo,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): JvmToolchainInfo {
    val target = Label.parse("@bazel_tools//tools/jdk:current_java_toolchain")
    val queryFile = bspInfo.bazelBspDir.resolve("aspects/toolchain_query.bzl")
    val command =
      bazelRunner.buildBazelCommand(workspaceContext = workspaceContext, inheritProjectviewOptionsOverride = true) {
        cquery {
          targets.add(target)
          options.addAll(listOf("--starlark:file=$queryFile", "--output=starlark"))
        }
      }
    val cqueryResult =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false)
        .waitAndGetResult()
    if (cqueryResult.isNotSuccess) throw RuntimeException("Could not query target '$target' for jvm toolchain info")
    try {
      val classpaths = Gson().fromJson(cqueryResult.stdout.inputStream().reader(), JvmToolchainInfo::class.java)
      return classpaths
    } catch (e: JsonSyntaxException) {
      // sometimes Bazel returns two values to a query when multiple configurations apply to a target
      return if (cqueryResult.stdoutLines.size > 1) {
        val allOpts = cqueryResult.stdoutLines.map { Gson().fromJson(it, JvmToolchainInfo::class.java) }
        allOpts.first()
      } else {
        throw e
      }
    }
  }

  suspend fun jvmToolchainQueryForTarget(
    bspInfo: BspInfo,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
    target: Label,
  ): JvmToolchainInfo {
    val aqueryExpression = "mnemonic(\"Javac\", $target)"
    val command =
      bazelRunner.buildBazelCommand(workspaceContext = workspaceContext, inheritProjectviewOptionsOverride = true) {
        aquery {
          options.addAll(listOf("--output=jsonproto", aqueryExpression))
        }
      }
    val aqueryResult =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false)
        .waitAndGetResult()
    if (aqueryResult.isNotSuccess) throw RuntimeException("Could not query target '$target' for jvm toolchain info")

    return parseJavaHomeAndToolchainPathFromAquery(aqueryResult.stdout)
  }

  /**
   * Extract java_home, toolchain_path, and jvm_opts from aquery result.
   * Uses -jar as delimiter: everything before -jar are JVM options, everything after are compiler options.
   */
  private fun parseJavaHomeAndToolchainPathFromAquery(stdout: ByteArray): JvmToolchainInfo {
    val gson = Gson()
    val jsonObject = gson.fromJson(stdout.inputStream().reader(), JsonObject::class.java)
    val actions = jsonObject.getAsJsonArray("actions")

    if (actions.isEmpty) {
      throw RuntimeException("No Javac actions found in aquery result")
    }

    val javacAction =
      actions.firstOrNull()?.asJsonObject
        ?: throw RuntimeException("No Javac actions found in aquery result")
    val arguments = javacAction.getAsJsonArray("arguments")

    var javaHome = ""
    var toolchainPath = ""
    val jvmOpts = mutableListOf<String>()

    // Find -jar position to use as delimiter
    var jarIndex = -1
    for (i in 0 until arguments.size()) {
      if (arguments[i].asString == "-jar") {
        jarIndex = i
        break
      }
    }

    if (jarIndex == -1) {
      throw RuntimeException("No -jar argument found in javac command")
    }

    // Extract java_home from first argument
    if (arguments.size() > 0) {
      val javaExec = arguments[0].asString
      val javaExecName = ExecUtils.calculateExecutableName("java")
      if (javaExec.endsWith("/bin/$javaExecName") || javaExec.endsWith("/$javaExecName")) {
        javaHome = javaExec.substring(0, javaExec.lastIndexOf("/bin/$javaExecName"))
      } else if (javaExec.contains("/bin/$javaExecName")) {
        javaHome = javaExec.substring(0, javaExec.indexOf("/bin/$javaExecName"))
      }
    }

    // Extract toolchain_path from argument after -jar
    if (jarIndex + 1 < arguments.size()) {
      toolchainPath = arguments[jarIndex + 1].asString
    }

    // Extract JVM options: everything between java executable and -jar
    for (i in 1 until jarIndex) {
      val arg = arguments[i].asString
      jvmOpts.add(arg)
    }

    return JvmToolchainInfo(
      java_home = javaHome,
      toolchain_path = toolchainPath,
      jvm_opts = jvmOpts,
    )
  }
}
