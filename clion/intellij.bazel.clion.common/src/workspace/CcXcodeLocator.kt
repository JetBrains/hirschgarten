package org.jetbrains.bazel.clion.workspace

import com.intellij.build.events.MessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.jetbrains.bazel.clion.BazelCLionCommonBundle
import org.jetbrains.bazel.clion.sync.CcToolchainBuildTarget
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection
import org.jetbrains.bsp.protocol.RunParams
import java.nio.file.InvalidPathException
import java.nio.file.Path

private val XCODE_LOCATOR = Label.parse("@bazel_tools//tools/osx:xcode-locator")

private const val NO_CONVENIENCE_SYMLINKS = "--experimental_convenience_symlinks=ignore";

internal class CcXcodeLocator(private val ctx: CcImportContext, private val scope: CoroutineScope) {

  data class Result(val developerDir: Path, val sdkRoot: Path)

  private val cache = mutableMapOf<CcToolchainBuildTarget.XcodeInfo, Deferred<Result?>>()

  suspend fun resolve(info: CcToolchainBuildTarget.XcodeInfo?): Result? {
    if (info == null || info.xcodeVersion.isEmpty() || info.macosSdkVersion.isEmpty()) return null

    return cache.computeIfAbsent(info) { scope.async { doResolve(info) } }.await()
  }

  private suspend fun doResolve(info: CcToolchainBuildTarget.XcodeInfo): Result? {
    val developerDir = locateDeveloperDir(info) ?: return null
    val sdkVersion = "MacOSX${info.macosSdkVersion}.sdk"

    return Result(
      developerDir = developerDir,
      sdkRoot = developerDir.resolve("Platforms", "MacOSX.platform", "Developer", "SDKs", sdkVersion)
    )
  }

  private suspend fun locateDeveloperDir(info: CcToolchainBuildTarget.XcodeInfo): Path? {
    val result = ctx.project.connection.runWithServer { server ->
      val params = RunParams(
        taskId = ctx.taskId,
        target = XCODE_LOCATOR,
        additionalBazelParams = listOf(NO_CONVENIENCE_SYMLINKS),
        arguments = listOf(info.xcodeVersion),
        checkVisibility = true,
        captureStdout = true,
      )

      server.buildTargetRun(params)
    }

    if (result.statusCode != BazelStatus.SUCCESS) return null
    val path = result.stdout ?: return null

    return try {
      Path.of(path.trim())
    } catch (_: InvalidPathException) {
      null
    }
  }

  suspend fun reportProblems() {
    val problems = cache.entries.mapNotNull { (info, deferred) ->
      if (deferred.await() != null) return@mapNotNull null
      BazelCLionCommonBundle.message("cc.xcode.locate.failed", info.xcodeVersion, info.macosSdkVersion)
    }

    if (problems.isEmpty()) return

    ctx.reportEvent(
      message = BazelCLionCommonBundle.message("cc.xcode.resolve.failed"),
      description = problems.joinToString("\n"),
      severity = MessageEvent.Kind.WARNING,
    )
  }
}