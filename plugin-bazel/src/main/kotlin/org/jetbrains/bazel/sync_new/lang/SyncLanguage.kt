package org.jetbrains.bazel.sync_new.lang

import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex

interface SyncLanguage<T : SyncTargetData> {
  val serialId: Long
  val name: String
}

inline fun <reified T : SyncTargetData> SyncLanguage<T>.getLangData(vertex: BazelTargetVertex): T? {
  return vertex.targetData.get(serialId) as? T?
}
