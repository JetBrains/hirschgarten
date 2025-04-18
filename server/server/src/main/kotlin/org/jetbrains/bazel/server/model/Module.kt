package org.jetbrains.bazel.server.model

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path

data class Module(
  val label: Label,
  val isSynthetic: Boolean,
  val directDependencies: List<Label>,
  val languages: Set<LanguageClass>,
  val tags: Set<Tag>,
  val baseDirectory: Path,
  val sources: List<SourceItem>,
  val resources: Set<Path>,
  val sourceDependencies: Set<Path>,
  val languageData: LanguageData?,
  val environmentVariables: Map<String, String>,
  val kindString: String,
)

// TODO [#BAZEL-721] - quite a naive predicate, but otherwise we'll need to have rule type info in Module instance
fun Module.isJvmLanguages() =
  languages.contains(LanguageClass.JAVA) || languages.contains(LanguageClass.KOTLIN) || languages.contains(LanguageClass.SCALA)
