package org.jetbrains.bazel.server.sync

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.jetbrains.bazel.bazelrunner.BazelRunner
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
    val queryFile = bspInfo.bazelBspDir().resolve("aspects/toolchain_query.bzl")
    val command =
      bazelRunner.buildBazelCommand(workspaceContext = workspaceContext, inheritProjectviewOptionsOverride = true) {
        cquery {
          targets.add(target)
          options.addAll(listOf("--starlark:file=$queryFile", "--output=starlark"))
        }
      }
    val cqueryResult =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
        .waitAndGetResult(ensureAllOutputRead = true)
    if (cqueryResult.isNotSuccess) throw RuntimeException("Could not query target '$target' for jvm toolchain info")
    try {
      val classpaths = Gson().fromJson(cqueryResult.stdout, JvmToolchainInfo::class.java)
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
}
