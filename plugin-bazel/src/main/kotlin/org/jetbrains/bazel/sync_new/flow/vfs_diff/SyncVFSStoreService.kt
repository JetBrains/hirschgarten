package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
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
  internal val build2Targets: KVStore<Path, Set<Label>> =
    project.storageContext.createKVStore<Path, Set<Label>>("bazel.sync.vfs.build2targets", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofPath() }
      .withValueCodec { ofSet(ofLabel()) }
      .build()

  // target -> BUILD file
  internal val target2Build: KVStore<Label, Path> =
    project.storageContext.createKVStore<Label, Path>("bazel.sync.vfs.target2build", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofLabel() }
      .withValueCodec { ofPath() }
      .build()

  internal val source2Target: KVStore<Path, Set<Label>> =
    project.storageContext.createKVStore<Path, Set<Label>>("bazel.sync.vfs.source2target", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofPath() }
      .withValueCodec { ofSet(ofLabel()) }
      .build()

  internal val target2Source: KVStore<Label, MutableSet<Path>> =
    project.storageContext.createKVStore<Label, MutableSet<Path>>("bazel.sync.vfs.target2source", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofLabel() }
      .withValueCodec { ofMutableSet(ofPath()) }
      .build()

  suspend fun invalidateAll() {
    build2Targets.clear()
    target2Build.clear()
    source2Target.clear()
    target2Source.clear()
  }

}
