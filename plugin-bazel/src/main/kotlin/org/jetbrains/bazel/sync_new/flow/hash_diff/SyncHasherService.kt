package org.jetbrains.bazel.sync_new.flow.hash_diff

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.SyncConsoleTask
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.DefaultStorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class SyncHasherService(
  private val project: Project,
) {
  //  target label hash -> target hash
  internal val target2Hash: KVStore<HashValue128, HashValue128> =
    project.storageContext.createKVStore<HashValue128, HashValue128>("bazel.sync.target2Hash", DefaultStorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofHash128() }
      .build()

  suspend fun computeHashDiff(diff: SyncColdDiff, task: SyncConsoleTask): SyncColdDiff {
    val ctx = SyncHasherContext(
      project = project,
      service = this,
    )
    return SyncHasherProcessor().process(ctx, task, diff)
  }
}
