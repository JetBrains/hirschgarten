package org.jetbrains.bazel.sync.workspace.model

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path

data class Module(
  val label: Label,
  val directDependencies: List<Label>,
  val language: LanguageClass,
  val tags: Set<Tag>,
  val baseDirectory: Path,
  val sources: List<SourceItem>,
  val resources: Set<Path>,
  val languageData: LanguageData?,
  val kindString: String,
  val target: BspTargetInfo.TargetInfo
)
