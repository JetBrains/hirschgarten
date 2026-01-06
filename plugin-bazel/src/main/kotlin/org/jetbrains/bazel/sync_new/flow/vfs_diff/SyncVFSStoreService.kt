package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofLabel
import org.jetbrains.bazel.sync_new.codec.ofMutableSet
import org.jetbrains.bazel.sync_new.codec.ofPath
import org.jetbrains.bazel.sync_new.codec.ofSet
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.storageContext
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class SyncVFSStoreService(
  private val project: Project,
) {

  // BUILD file -> target
  internal val build2Targets: KVStore<HashValue128, Set<Label>> =
    project.storageContext.createKVStore<HashValue128, Set<Label>>("bazel.sync.vfs.build2targets", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofSet(ofLabel()) }
      .build()

  // target -> BUILD file
  internal val target2Build: KVStore<HashValue128, Path> =
    project.storageContext.createKVStore<HashValue128, Path>("bazel.sync.vfs.target2build", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofPath() }
      .build()

  internal val source2Target: KVStore<HashValue128, Set<Label>> =
    project.storageContext.createKVStore<HashValue128, Set<Label>>("bazel.sync.vfs.source2target", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofSet(ofLabel()) }
      .build()

  internal val target2Source: KVStore<HashValue128, MutableSet<Path>> =
    project.storageContext.createKVStore<HashValue128, MutableSet<Path>>("bazel.sync.vfs.target2source", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofMutableSet(ofPath()) }
      .build()

  suspend fun invalidateAll() {
    build2Targets.clear()
    target2Build.clear()
    source2Target.clear()
    target2Source.clear()
  }

}
