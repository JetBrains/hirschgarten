package org.jetbrains.bazel.clion.workspace

import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocation
import java.nio.file.Path

fun WorkspaceTargetKey.presentable(): String {
  return label.toString() + configuration.shortChecksum?.let { " ($it)" }.orEmpty()
}

fun CcImportContext.resolveOutputLocation(location: OutputLocation): Path? {
  return outputResolver.resolve(location, snapshot.repoMapping.getLocalRepositories())
}

fun CcImportContext.resolveExecutionRootPath(path: String): Path? {
  return resolveOutputLocation(outputParser.parseExecrootPath(path))
}
