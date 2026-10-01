package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus
import java.nio.charset.Charset
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.bufferedReader

/**
 * Cheaply extracts the package name declared in a JVM (Java, Kotlin, Scala) source file without parsing it.
 *
 * The file is scanned line by line for `package <name>` declarations. To keep sync fast on large projects, the file is
 * read with a small buffer and a one-byte charset; only lines that look like a package declaration are re-decoded
 * as UTF-8, so non-ASCII package names are still recognized.
 *
 * This is a heuristic: comments, string literals and annotations on the package clause are not taken into account.
 */
@ApiStatus.Internal
object JvmPackageNameParser {
  private val PACKAGE_PATTERN = Regex("^\\s*package\\s+([\\p{L}0-9_.]+)")
  private val ONE_BYTE_CHARSET = Charset.forName("ISO-8859-1")
  private const val BUFFER_SIZE = 256 // Should be enough to read a Java package name if it's on the first line

  /**
   * Returns the package of [source], or `null` if the file does not exist or declares no package.
   *
   * If [multipleLines] is `true`, all package declarations of the file are joined with `.`,
   * which supports Scala chained package clauses (`package a` followed by `package b` means `a.b`);
   * otherwise, the first declaration wins.
   */
  fun findPackage(source: Path, multipleLines: Boolean = false): String? {
    // avoid extra stat per file by handling exception
    val reader = try {
      source.bufferedReader(charset = ONE_BYTE_CHARSET, bufferSize = BUFFER_SIZE)
    }
    catch (_: NoSuchFileException) {
      return null
    }
    reader.use { bufferedReader ->
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
}
