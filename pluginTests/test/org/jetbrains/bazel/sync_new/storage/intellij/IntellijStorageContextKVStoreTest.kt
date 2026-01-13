package org.jetbrains.bazel.sync_new.storage.intellij

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageContextKVStoreTest
import org.jetbrains.bazel.sync_new.storage.mvstore.MVStoreStorageContext
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.provider.Arguments

@TestApplication
@Disabled("wip")
internal class IntellijStorageContextKVStoreTest : StorageContextKVStoreTest() {
  override fun createStorageContext(
    project: Project,
    disposable: Disposable,
  ): StorageContext = IntellijStorageContext(project, disposable)

  companion object {
    @JvmStatic
    fun testCases(): List<Arguments> = combine(defaultTestCases, defaultHintSets)
  }
}
