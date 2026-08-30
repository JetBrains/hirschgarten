package org.jetbrains.bazel.tools.pluginDistribution

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

private data class Arguments(
  val expectedEntries: Path,
  val pluginZip: Path,
)

fun main(rawArgs: Array<String>) {
  val args = parseArguments(rawArgs.toList())

  val expectedEntries = Files.readAllLines(args.expectedEntries)
    .asSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .toSortedSet()
  require(expectedEntries.isNotEmpty()) { "${args.expectedEntries} names no entry" }

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
  var expectedEntries: Path? = null
  var pluginZip: Path? = null

  var index = 0
  while (index < rawArgs.size) {
    val key = rawArgs[index++]
    require(index < rawArgs.size) { "missing value for $key" }
    val value = rawArgs[index++]
    when (key) {
      "--expected_entries" -> expectedEntries = Paths.get(value)
      "--plugin_zip" -> pluginZip = Paths.get(value)
      else -> error("unknown argument: $key")
    }
  }

  return Arguments(
    expectedEntries = requireNotNull(expectedEntries) { "missing --expected_entries" },
    pluginZip = requireNotNull(pluginZip) { "missing --plugin_zip" },
  )
}
