package org.jetbrains.bazel.sync_new.flow.diff.vfs

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.ofLabel
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
    project.storageContext.createKVStore<Path, Set<Label>>("bazel.sync.vfs.build2targets", StorageHints.USE_IN_MEMORY)
      .withKeyCodec { ofPath() }
      .withValueCodec { ofSet(ofLabel()) }
      .build()

  // target -> BUILD file
  internal val target2Build: KVStore<Label, Path> =
    project.storageContext.createKVStore<Label, Path>("bazel.sync.vfs.target2build", StorageHints.USE_IN_MEMORY)
      .withKeyCodec { ofLabel() }
      .withValueCodec { ofPath() }
      .build()

  suspend fun invalidateAll() {
    build2Targets.clear()
    target2Build.clear()
  }

}
