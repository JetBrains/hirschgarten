package org.jetbrains.bazel.server.sync.languages

import org.jetbrains.bazel.server.sync.languages.jvm.SourceRootGuesser
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

object JVMLanguagePluginParser {
  private val PACKAGE_PATTERN = Regex("^\\s*package\\s+([\\p{L}0-9_.]+)")
  private val ONE_BYTE_CHARSET = Charset.forName("ISO-8859-1")
  private const val BUFFER_SIZE = 256 // Should be enough to read a Java package name if it's on the first line

  fun calculateJVMSourceRootAndAdditionalData(source: Path, multipleLines: Boolean = false): SourceRootAndData {
    val sourcePackage =
      findPackage(source, multipleLines)
        ?: return SourceRootAndData(SourceRootGuesser.getSourcesRoot(source))
    val sourcePackagePath = Paths.get(sourcePackage.replace(".", "/"))
    val sourceRootEndIndex = source.nameCount - sourcePackagePath.nameCount - 1
    val sourceRoot =
      if (!source.parent.endsWith(sourcePackagePath)) {
        SourceRootGuesser.getSourcesRoot(source)
      } else {
        Paths.get("/").resolve(source.subpath(0, sourceRootEndIndex))
      }
    return SourceRootAndData(sourceRoot, jvmPackagePrefix = sourcePackage)
  }

  private fun findPackage(source: Path, multipleLines: Boolean): String? =
    File(source.toUri()).bufferedReader(charset = ONE_BYTE_CHARSET, bufferSize = BUFFER_SIZE).use { bufferedReader ->
      // Not using UTF-8 charset because it is slower to decode
      val packages =
        bufferedReader.lineSequence().mapNotNull { line ->
          if (!line.trimStart().startsWith("package")) return@mapNotNull null
          val decodedLine = line.toByteArray(ONE_BYTE_CHARSET).decodeToString()
          PACKAGE_PATTERN
            .find(decodedLine)
            ?.groups
            ?.get(1)
            ?.value
        }
      return if (multipleLines) {
        packages.joinToString(".").takeIf { it.isNotEmpty() }
      } else {
        packages.firstOrNull()
      }
    }
}
