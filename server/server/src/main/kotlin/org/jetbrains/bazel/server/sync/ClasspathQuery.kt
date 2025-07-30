package org.jetbrains.bazel.server.sync

import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.utils.toJson
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BspJvmClasspath
import java.nio.file.Path

object ClasspathQuery {
  suspend fun classPathQuery(
    target: Label,
    bspInfo: BspInfo,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): BspJvmClasspath {
    val queryFile = bspInfo.bazelBspDir.resolve("aspects/runtime_classpath_query.bzl")
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
    if (cqueryResult.isNotSuccess) throw RuntimeException("Could not query target '$target' for runtime classpath")
    try {
      return bazelGson
        .fromJson(cqueryResult.stdout.toJson(), JvmClasspath::class.java)
        .toProtocolModel()
    } catch (e: JsonSyntaxException) {
      // sometimes Bazel returns two values to a query when multiple configurations apply to a target
      return if (cqueryResult.stdoutLines.size > 1) {
        val allOpts =
          cqueryResult.stdoutLines.mapNotNull {
            try {
              bazelGson.fromJson(it, JvmClasspath::class.java)
            } catch (_: JsonSyntaxException) {
              null
            }
          }
        allOpts
          .maxByOrNull { it.runtimeClasspath.size + it.compileClasspath.size }!!
          .toProtocolModel()
      } else {
        throw e
      }
    }
  }

  data class JvmClasspath(
    @SerializedName("runtime_classpath") val runtimeClasspath: List<Path>,
    @SerializedName("runtime_classpath") val compileClasspath: List<Path>,
  ) {
    fun toProtocolModel() = BspJvmClasspath(runtimeClasspath, compileClasspath)
  }
}
