package org.jetbrains.bazel.sync_new.languages_impl.jvm

import org.jetbrains.bazel.sync_new.lang.SyncLanguage

object JvmSyncLanguage : SyncLanguage {
  const val LANGUAGE_TAG: Long = 1333051237017459801L

  override val serialId: Long = LANGUAGE_TAG
  override val name: String = "jvm"
}
