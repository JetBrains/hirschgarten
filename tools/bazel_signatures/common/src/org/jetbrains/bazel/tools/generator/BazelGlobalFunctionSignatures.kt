package org.jetbrains.bazel.tools.generator

import com.google.gson.GsonBuilder
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class FunctionSignature(
  val name: String,
  val doc: String?,
  val environment: List<String>,
  val params: List<ParamSignature>,
  val returnType: String?,
)

data class ParamSignature(
  val name: String,
  val doc: String?,
  val defaultValue: String?,
  val named: Boolean,
  val positional: Boolean,
  val required: Boolean,
)

fun writeSignaturesJson(functions: List<FunctionSignature>, outputPath: Path) {
  val sorted = functions.normalized()
  val json = GsonBuilder()
    .setPrettyPrinting()
    .disableHtmlEscaping()
    .create()
    .toJson(sorted)
  val previousFile = findPreviousVersionFile(outputPath)
  if (previousFile != null && previousFile.readText() == json) {
    println("# Skipped: $outputPath — identical to ${previousFile.name}")
    return
  }

  outputPath.writeText(json)
  println("# Written: $outputPath (${sorted.size} functions)")
}


/**
 * Cleans up proto doc strings for HTML rendering:
 * - Removes links that won't work in IntelliJ (anchors, relative paths)
 * - Strips bare markdown reference links like `[GoArchive]`
 * - Keeps actual URL links
 * - Converts markdown HTTP links to `<a>` tags
 * - Converts markdown backticks to `<code>` tags
 */
fun String.cleanupStardoc(): String? = ifEmpty { null }
  ?.replace(Regex("<a\\s[^>]*href\\s*=\\s*[\"'](?!https?://)[^\"']*[\"'][^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL), "$1")
  ?.replace(Regex("""<a\s[^>]*href\s*=\s*["'](?!https?://)[^"']*["'][^>]*>""", RegexOption.DOT_MATCHES_ALL), "")
  ?.replace(Regex("""\[(.*?)]\(\s*(https?://[^)]*)\)""", RegexOption.DOT_MATCHES_ALL), """<a href="$2">$1</a>""")
  ?.replace(Regex("""\[(.*?)]\(\s*(?!https?://)[^)]*\)""", RegexOption.DOT_MATCHES_ALL), "$1")
  ?.replace(Regex("""\[([A-Za-z]\w*)](?!\()"""), "$1")
  ?.replace(Regex("```(.*?)```", RegexOption.DOT_MATCHES_ALL), "<code>$1</code>")
  ?.replace(Regex("`(.*?)`"), "<code>$1</code>")

private fun List<FunctionSignature>.normalized(): List<FunctionSignature> =
  mergeDuplicatesByName()
    .sortedBy { it.name }

/**
 * It handles functions that are declared separately for each environment e.g. `select` in builtins
 */
private fun List<FunctionSignature>.mergeDuplicatesByName(): List<FunctionSignature> =
  groupingBy { it.name }
    .reduce { name, acc, signature ->
      if (acc.params != signature.params || acc.doc != signature.doc || acc.returnType != signature.returnType) {
        println("# Warning: duplicate signatures for '$name' differ beyond environment; keeping the first variant's params/doc/returnType")
      }
      acc.copy(environment = (acc.environment + signature.environment).distinct())
    }
    .values
    .map { it.copy(environment = it.environment.sorted()) }
    .toList()

/**
 * Given an output path like `.../builtins@9.2.0.json`, finds the existing file with
 * the highest version for the same prefix (e.g. `builtins@9.1.1.json`).
 */
private fun findPreviousVersionFile(outputPath: Path): Path? {
  val fileName = outputPath.name
  val prefix = fileName.substringBefore('@')
  val dir = outputPath.parent ?: return null
  return dir.listDirectoryEntries("$prefix@*.json")
    .filter { it.name != fileName }
    .maxWithOrNull { a, b ->
      val vA = a.name.substringAfter('@').removeSuffix(".json")
      val vB = b.name.substringAfter('@').removeSuffix(".json")
      vA.compareAsVersions(vB)
    }
}
