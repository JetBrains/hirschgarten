package org.jetbrains.bazel.tools.pluginDistribution

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

private data class Arguments(
  val pluginContentYaml: Path,
  val pluginZip: Path,
  val pluginRoot: String,
)

fun main(rawArgs: Array<String>) {
  val args = parseArguments(rawArgs.toList())
  val pluginRoot = args.pluginRoot.trim('/')
  require(pluginRoot.isNotEmpty()) { "plugin_root must not be empty" }

  val expectedEntries = Files.readAllLines(args.pluginContentYaml)
    .asSequence()
    .map { it.trim() }
    .filter { it.startsWith("- name: lib/") }
    .map { "$pluginRoot/${it.removePrefix("- name: ")}" }
    .toSortedSet()

  val actualEntries = ZipFile(args.pluginZip.toFile()).use { zip ->
    zip.entries().asSequence()
      .filterNot { it.isDirectory }
      .map { it.name }
      .toSortedSet()
  }

  val missingEntries = expectedEntries - actualEntries
  val extraEntries = actualEntries - expectedEntries
  require(missingEntries.isEmpty() && extraEntries.isEmpty()) {
    buildList {
      if (missingEntries.isNotEmpty()) {
        add("Missing entries from plugin zip:\n${missingEntries.joinToString("\n")}")
      }
      if (extraEntries.isNotEmpty()) {
        add("Extra entries in plugin zip:\n${extraEntries.joinToString("\n")}")
      }
    }.joinToString("\n\n")
  }
}

private fun parseArguments(rawArgs: List<String>): Arguments {
  var pluginContentYaml: Path? = null
  var pluginZip: Path? = null
  var pluginRoot: String? = null

  var index = 0
  while (index < rawArgs.size) {
    val key = rawArgs[index++]
    require(index < rawArgs.size) { "missing value for $key" }
    val value = rawArgs[index++]
    when (key) {
      "--plugin_content_yaml" -> pluginContentYaml = Paths.get(value)
      "--plugin_zip" -> pluginZip = Paths.get(value)
      "--plugin_root" -> pluginRoot = value
      else -> error("unknown argument: $key")
    }
  }

  return Arguments(
    pluginContentYaml = requireNotNull(pluginContentYaml) { "missing --plugin_content_yaml" },
    pluginZip = requireNotNull(pluginZip) { "missing --plugin_zip" },
    pluginRoot = requireNotNull(pluginRoot) { "missing --plugin_root" },
  )
}
