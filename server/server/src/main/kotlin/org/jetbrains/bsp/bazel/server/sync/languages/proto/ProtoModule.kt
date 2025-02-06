package org.jetbrains.bsp.bazel.server.sync.languages.proto

import org.jetbrains.bsp.bazel.server.model.LanguageData
import java.net.URI

data class ProtoModule(
  val sources: List<URI> = emptyList(),
  val ruleKind: String
) : LanguageData
