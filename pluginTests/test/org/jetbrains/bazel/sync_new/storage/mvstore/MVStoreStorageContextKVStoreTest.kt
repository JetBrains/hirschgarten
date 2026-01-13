package org.jetbrains.bazel.sync_new.storage.mvstore

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageContextKVStoreTest
import org.junit.jupiter.params.provider.Arguments

@TestApplication
internal class MVStoreStorageContextKVStoreTest : StorageContextKVStoreTest() {
  override fun createStorageContext(
    project: Project,
    disposable: Disposable,
  ): StorageContext = MVStoreStorageContext(project, disposable)

  companion object {
    @JvmStatic
    fun testCases(): List<Arguments> = combine(defaultTestCases, defaultHintSets)
  }
}
