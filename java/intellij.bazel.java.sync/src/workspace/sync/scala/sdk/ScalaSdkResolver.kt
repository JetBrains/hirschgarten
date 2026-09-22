package org.jetbrains.bazel.scala.sdk

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import java.util.regex.Pattern

internal object ScalaSdkResolver {
  fun resolveScalaVersion(targetInfo: TargetIdeInfo): String? {
    if (!targetInfo.hasScalaTargetInfo()) {
      return null
    }
    return targetInfo.scalaTargetInfo.compilerClasspathList
      .mapNotNull { extractVersion(it.relativePath.substringAfterLast('/')) }
      .maxOfOrNull { it }
  }

  private fun extractVersion(fileName: String): String? {
    val matcher = VERSION_PATTERN.matcher(fileName)
    return if (matcher.matches()) matcher.group(1) else null
  }

  private val VERSION_PATTERN =
    Pattern.compile("(?:processed_)?scala3?-(?:library|compiler|reflect)(?:_3)?-([.\\d]+)\\.jar")
}
