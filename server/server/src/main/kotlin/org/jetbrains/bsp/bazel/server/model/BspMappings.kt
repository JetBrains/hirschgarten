package org.jetbrains.bsp.bazel.server.model

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.TextDocumentIdentifier
import java.net.URI

object BspMappings {
  fun toBspId(module: Module): BuildTargetIdentifier = BuildTargetIdentifier(module.label.toString())

  fun toBspTag(tag: Tag): String? =
    when (tag) {
      Tag.APPLICATION -> BuildTargetTag.APPLICATION
      Tag.TEST -> BuildTargetTag.TEST
      Tag.LIBRARY -> BuildTargetTag.LIBRARY
      Tag.INTELLIJ_PLUGIN -> "intellij-plugin"
      Tag.NO_IDE -> BuildTargetTag.NO_IDE
      Tag.NO_BUILD, Tag.MANUAL -> null
    }

  fun toBspUri(uri: URI): String = uri.toString()

  fun getModules(project: AspectSyncProject, targets: List<BuildTargetIdentifier>): Set<Module> =
    toLabels(targets).mapNotNull(project::findModule).toSet()

  fun toUri(textDocument: TextDocumentIdentifier): URI = URI.create(textDocument.uri)

  fun toLabels(targets: List<BuildTargetIdentifier>): Set<Label> = targets.map(BuildTargetIdentifier::uri).map { Label.parse(it) }.toSet()
}
