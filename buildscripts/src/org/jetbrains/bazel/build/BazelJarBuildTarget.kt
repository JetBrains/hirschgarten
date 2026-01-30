package org.jetbrains.bazel.build

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.intellij.build.BuildPaths.Companion.ULTIMATE_HOME
import org.jetbrains.intellij.build.buildJar
import org.jetbrains.intellij.build.impl.createCompilationContext
import org.jetbrains.intellij.build.impl.moduleOutputAsSource
import org.jetbrains.intellij.build.telemetry.TraceManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

object BazelJarBuildTarget {
  private val bazelModulesToPack =
    listOf(
      "intellij.bazel.commons",
      "intellij.bazel.protobuf",
      "intellij.bazel.kotlin.k2",
    )

  /**
   * Packs some Bazel modules to be consumed by Fleet,
   * temporarily, until switching to building with Bazel
   */
  @OptIn(ExperimentalPathApi::class)
  @Suppress("RAW_RUN_BLOCKING")
  @JvmStatic
  fun main(args: Array<String>) {
    val outDir = ULTIMATE_HOME.resolve("out/bazel-plugin")
    outDir.deleteRecursively()
    outDir.createDirectories()

    val jarFile =
      runBlocking(Dispatchers.Default) {
        val context =
          createCompilationContext(
            projectHome = ULTIMATE_HOME,
            defaultOutputRoot = outDir,
          )

        context.compileModules(bazelModulesToPack)
        val jarFile = outDir.resolve("bazel.jar")
        buildJar(
          targetFile = jarFile,
          sources =
            bazelModulesToPack.map { moduleName ->
              val module =
                context.projectModel.project.findModuleByName(moduleName)
                  ?: error("Module '$moduleName' not found")
              moduleOutputAsSource(module, outputProvider = context.outputProvider)
            },
        )
        jarFile
      }

    // flush logging before println
    runBlocking {
      TraceManager.flush()
    }

    println("Bazel jar is at $jarFile")
  }
}
