package org.jetbrains.bsp.bazel.server.sync.firstStep.mappings

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.bsp.bazel.server.sync.languages.JVMLanguagePluginParser
import org.jetbrains.bsp.protocol.EnhancedSourceItem
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

fun Target.toBspSourcesItem(workspaceRoot: Path): SourcesItem {
  val sourceFiles = calculateAllExistingSourceFiles(workspaceRoot)
  val sourceFilesAndData = sourceFiles.map { it to JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(it) }

  val items = sourceFilesAndData.map { EnhancedSourceItem(it.first.toUri().toString(), SourceItemKind.FILE, false, it.second.data) }

  return SourcesItem(
    BuildTargetIdentifier(rule.name),
    items,
  ).apply {
    roots = sourceFilesAndData.map { it.second.sourceRoot }.map { it.toUri().toString() }.distinct()
  }
}

private fun Target.calculateAllExistingSourceFiles(workspaceRoot: Path): List<Path> =
  getListAttribute(SRCS_NAME).map { it.bazelFileFormatToPath(workspaceRoot) }.filter { it.exists() }

fun String.bazelFileFormatToPath(workspaceRoot: Path): Path {
  val withoutColons = replace(':', '/')
  val withoutTargetPrefix = withoutColons.trimStart('/')
  val relativePath = Path(withoutTargetPrefix)

  return workspaceRoot.resolve(relativePath)
}
