package org.jetbrains.bsp.bazel.server.model

import org.jetbrains.bazel.commons.label.Label
import java.net.URI

data class Module(
  val label: Label,
  val isSynthetic: Boolean,
  val directDependencies: List<Label>,
  val languages: Set<Language>,
  val tags: Set<Tag>,
  val baseDirectory: URI,
  val sourceSet: SourceSet,
  val resources: Set<URI>,
  val outputs: Set<URI>,
  val sourceDependencies: Set<URI>,
  val languageData: LanguageData?,
  val environmentVariables: Map<String, String>,
)

// TODO [#BAZEL-721] - quite a naive predicate, but otherwise we'll need to have rule type info in Module instance
fun Module.isJvmLanguages() = languages.contains(Language.JAVA) || languages.contains(Language.KOTLIN) || languages.contains(Language.SCALA)
