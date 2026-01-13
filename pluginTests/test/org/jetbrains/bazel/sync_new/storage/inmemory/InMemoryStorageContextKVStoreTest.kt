package org.jetbrains.bazel.sync_new.storage.inmemory

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageContextKVStoreTest
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryStorageContext
import org.jetbrains.bazel.sync_new.storage.mvstore.MVStoreStorageContext
import org.junit.jupiter.params.provider.Arguments

@TestApplication
internal class InMemoryStorageContextKVStoreTest : StorageContextKVStoreTest() {
  override fun createStorageContext(
    project: Project,
    disposable: Disposable,
  ): StorageContext = InMemoryStorageContext(project, disposable)

  companion object {
    @JvmStatic
    fun testCases(): List<Arguments> = combine(defaultTestCases, defaultHintSets)
  }
}
