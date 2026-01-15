package org.jetbrains.bazel.sync_new.storage.mvstore

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageContextFlatStoreTest
import org.jetbrains.bazel.sync_new.storage.StorageContextKVStoreTest
import org.jetbrains.bazel.sync_new.storage.mvstore.MVStoreStorageContext
import org.jetbrains.bazel.sync_new.storage.rocksdb.RocksdbStorageContext
import org.junit.jupiter.params.provider.Arguments

@TestApplication
internal class MVStoreStorageContextFlatStoreTest : StorageContextFlatStoreTest() {
  override fun createStorageContext(
    project: Project,
    disposable: Disposable,
  ): StorageContext = MVStoreStorageContext(project, disposable)
}
