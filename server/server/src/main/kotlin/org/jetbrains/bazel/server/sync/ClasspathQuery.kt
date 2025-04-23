package org.jetbrains.bazel.server.sync

import com.google.gson.JsonSyntaxException
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.utils.toJson
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path

object ClasspathQuery {
  suspend fun classPathQuery(
    target: Label,
    bspInfo: BspInfo,
    bazelRunner: BazelRunner,
    workspaceContext: WorkspaceContext,
  ): JvmClasspath {
    val queryFile = bspInfo.bazelBspDir().resolve("aspects/runtime_classpath_query.bzl")
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
      val classpaths = bazelGson.fromJson(cqueryResult.stdout.toJson(), JvmClasspath::class.java)
      return classpaths
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
        allOpts.maxByOrNull { it.runtime_classpath.size + it.compile_classpath.size }!!
      } else {
        throw e
      }
    }
  }

  data class JvmClasspath(val runtime_classpath: List<Path>, val compile_classpath: List<Path>)
}
