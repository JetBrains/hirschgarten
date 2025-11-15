package org.jetbrains.bazel.sync_new.storage

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

interface BazelStorageService {
  val context: StorageContext
}

val Project.storageContext: StorageContext
  get() = service<BazelStorageService>().context
