package org.jetbrains.bazel.sync_new.flow

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.bazel.sync_new.proto.BazelSyncMetadata
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofLabel
import org.jetbrains.bazel.sync_new.codec.proto.ofProtoMessage
import org.jetbrains.bazel.sync_new.codec.withConverter
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStorage
import org.jetbrains.bazel.sync_new.storage.createSortedKVStorage
import org.jetbrains.bazel.sync_new.storage.hash.hash128Comparator
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class SyncStoreService(
  private val project: Project,
) {
  val targetHashes =
    project.storageContext.createSortedKVStorage<HashValue128, Label>("bazel.sync.targetHashes", StorageHints.USE_PAGED_STORE)
      .withKeyComparator { hash128Comparator() }
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofLabel() }
      .build()

  val syncMetadata = project.storageContext.createFlatStorage<SyncMetadata>("bazel.sync.syncMetadata", StorageHints.USE_IN_MEMORY)
    .withCreator { SyncMetadata() }
    .withCodec {
      ofProtoMessage<BazelSyncMetadata>()
        .withConverter(SyncMetadata.converter)
    }
    .build()

}
