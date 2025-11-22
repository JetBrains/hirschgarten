package org.jetbrains.bazel.sync_new.languages_impl.jvm

import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import org.jetbrains.bazel.sync_new.lang.SyncClassTag
import org.jetbrains.bazel.sync_new.lang.SyncTargetData

@SyncClassTag(serialId = JvmSyncLanguage.LANGUAGE_TAG)
data class JvmSyncTargetData(
  val classJars: List<BazelPath>
) : SyncTargetData
