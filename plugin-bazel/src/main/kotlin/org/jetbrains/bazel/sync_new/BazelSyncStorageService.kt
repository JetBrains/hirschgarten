package org.jetbrains.bazel.sync_new

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.jetbrains.bazel.sync_new.proto.BazelSyncSnapshot
import org.jetbrains.bazel.sync_new.codec.converterOf
import org.jetbrains.bazel.sync_new.codec.proto.ofProtoMessage
import org.jetbrains.bazel.sync_new.codec.withConverter
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStorage
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator

@Service(Service.Level.PROJECT)
class BazelSyncStorageService(val storageContext: StorageContext) {
  internal val snapshotStorage = storageContext.createFlatStorage<SyncSnapshot>("bazel.sync.snapshot", StorageHints.USE_IN_MEMORY)
    .withCodec {
      ofProtoMessage<BazelSyncSnapshot>()
        .withConverter(SyncSnapshot.converter)
    }
    .withCreator { SyncSnapshot() }
    .build()
}

data class SyncSnapshot(
  val targetHashes: MutableMap<HashValue128, String> = ConcurrentHashMap()
) {
  companion object {
    internal val converter = converterOf<BazelSyncSnapshot, SyncSnapshot>(
      to = { proto ->
        val obj = SyncSnapshot()
        for (targetHash in proto.targetHashesList) {
          val hash = HashValue128(targetHash.hashHi, targetHash.hashLo)
          obj.targetHashes[hash] = targetHash.canonicalLabel
        }
        obj
      },
      from = { obj ->
        val builder = BazelSyncSnapshot.newBuilder()
        for ((hash, label) in obj.targetHashes) {
          val targetHash = BazelSyncSnapshot.TargetHash.newBuilder()
            .setHashHi(hash.mostSignificantBits)
            .setHashLo(hash.leastSignificantBits)
            .build()
          builder.addTargetHashes(targetHash)
        }
        builder.build()
      },
    )
  }

}
