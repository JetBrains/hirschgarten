package org.jetbrains.bsp.protocol

import java.net.URI

data class ProtoBuildTarget(
  val sources: List<URI>,
  val ruleKind: String,
)
