package org.jetbrains.workspace.model.constructors

import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.TextDocumentIdentifier

public class BuildTargetId(
  uri: String,
) : BuildTargetIdentifier(uri)

public class BuildTarget(
  id: BuildTargetIdentifier,
  displayName: String? = null,
  baseDirectory: String? = null,
  tags: List<String> = emptyList(),
  languageIds: List<String> = emptyList(),
  dependencies: List<BuildTargetIdentifier> = emptyList(),
  capabilities: BuildTargetCapabilities = BuildTargetCapabilities(),
  dataKind: String? = null,
  data: Any? = null,
) : ch.epfl.scala.bsp4j.BuildTarget(id, tags, languageIds, dependencies, capabilities) {
  init {
    super.setDisplayName(displayName)
    super.setBaseDirectory(baseDirectory)
    super.setDataKind(dataKind)
    super.setData(data)
  }
}

public class SourceItem(
  uri: String,
  kind: SourceItemKind,
  generated: Boolean = false,
) : ch.epfl.scala.bsp4j.SourceItem(uri, kind, generated)

public class SourcesItem(
  target: BuildTargetIdentifier,
  sources: List<SourceItem>,
  roots: List<String> = emptyList(),
) : ch.epfl.scala.bsp4j.SourcesItem(target, sources) {
  init {
    super.setRoots(roots)
  }
}

public class TextDocumentId(
  uri: String,
) : TextDocumentIdentifier(uri)
