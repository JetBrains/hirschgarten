package org.jetbrains.bazel.server.sync.languages.kotlin

import com.intellij.openapi.diagnostic.Logger
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.reader
import kotlin.io.path.relativeTo
import kotlin.io.path.writer

object KotlinManifestUtil {
  private val logger = Logger.getInstance(KotlinManifestUtil::class.java)
  //TODO, this could be made more generic I'm sure
  /**
   * Copies the original params file updating certain fields to reduce compile time
   * and point to a different output
   */
  fun updateAndWriteCompileParams(
    originalParams: Path,
    tempDir: Path,
    outputJar: Path,
    inputFile: Path,
    workspaceRoot: Path,
    targetJar: Path
  ): Path {
    val params = tempDir.resolve("compile-0.params")
    originalParams.reader().buffered().use { ips ->
      params.writer().use { os ->
        var line = ips.readLine()
        val sourceFile = inputFile.relativeTo(workspaceRoot)
        while (line != null) {
          fun OutputStreamWriter.writeLn(line: String) = write("$line\n")
          when (line) {
            "--output" -> {
              os.writeLn(line)
              os.writeLn(outputJar.pathString)
              ips.readLine()
            }

            "--kotlin_output_jdeps" -> {
              os.writeLn(line)
              os.writeLn(tempDir.resolve("output-kt.jdpes").pathString)
              ips.readLine()
            }

            "--sources" -> {
              os.writeLn(line)
              line = ips.readLine()
              os.writeLn(sourceFile.pathString)
              while (line?.startsWith("--") == false) {
                line = ips.readLine()
              }
              continue
            }

            "--direct_dependencies" -> {
              os.writeLn(line)
              line = ips.readLine()
              while (line?.startsWith("--") == false) {
                os.write("$line\n")
                line = ips.readLine()
              }
              //Add the modules jar to the dependencies
              if (targetJar.notExists()) {
                logger.error("Bazel module jar not found: $targetJar")
              }
              os.writeLn(targetJar.pathString)
              continue
            }

            "--classpath" -> {
              os.writeLn(line)
              line = ips.readLine()
              while (line?.startsWith("--") == false) {
                os.write("$line\n")
                line = ips.readLine()
              }
              //Add the modules jar to the dependencies
              if (targetJar.notExists()) {
                logger.error("Bazel module jar not found: $targetJar")
              }
              os.writeLn(targetJar.pathString)
              continue
            }

            else -> {
              os.writeLn(line)
            }
          }
          line = ips.readLine()
        }
      }
    }
    return params
  }
}
