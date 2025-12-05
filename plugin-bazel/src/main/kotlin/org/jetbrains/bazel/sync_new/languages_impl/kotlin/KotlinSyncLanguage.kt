package org.jetbrains.bazel.sync_new.languages_impl.kotlin

import org.jetbrains.bazel.sync_new.lang.SyncLanguage

object KotlinSyncLanguage : SyncLanguage<KotlinSyncTargetData> {
  const val LANGUAGE_TAG: Long = 7581531561983786586L

  override val serialId: Long = LANGUAGE_TAG
  override val name: String = "kotlin"
}
