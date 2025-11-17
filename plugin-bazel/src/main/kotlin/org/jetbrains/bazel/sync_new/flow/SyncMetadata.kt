package org.jetbrains.bazel.sync_new.flow

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.jetbrains.bazel.sync_new.proto.BazelPath
import com.jetbrains.bazel.sync_new.proto.BazelSyncMetadata
import org.jetbrains.bazel.sync_new.codec.converterOf
import java.nio.file.Path

data class SyncMetadata(
  val repoMapping: SyncRepoMapping = DisabledSyncRepoMapping,
) {
  companion object {
    internal val converter = converterOf<BazelSyncMetadata, SyncMetadata>(
      to = { proto ->
        SyncMetadata(
          repoMapping = if (proto.hasRepoMapping()) {
            BzlmodSyncRepoMapping.converter.to(proto.repoMapping)
          } else {
            DisabledSyncRepoMapping
          }
        )
      },
      from = { obj ->
        val repoMapping = when (obj.repoMapping) {
          is BzlmodSyncRepoMapping -> BzlmodSyncRepoMapping.converter.from(obj.repoMapping)
          DisabledSyncRepoMapping -> null
        }
        BazelSyncMetadata.newBuilder()
          .setRepoMapping(repoMapping)
          .build()
      }
    )
  }
}

sealed interface SyncRepoMapping

object DisabledSyncRepoMapping : SyncRepoMapping

data class BzlmodSyncRepoMapping(
  val apparentToCanonical: BiMap<String, String> = HashBiMap.create(),
  val canonicalToPath: Map<String, Path> = mapOf()
) : SyncRepoMapping{
  companion object {
    internal val converter = converterOf<BazelSyncMetadata.RepoMapping, BzlmodSyncRepoMapping>(
      to = { proto ->
        BzlmodSyncRepoMapping(
          apparentToCanonical = HashBiMap.create(proto.apparentToCanonicalMap),
          canonicalToPath = proto.canonicalToPathMap.mapValues { Path.of(it.value.path) }
        )
      },
      from = { obj ->
        val canonicalToPath = obj.canonicalToPath.mapValues {
          BazelPath.newBuilder()
            .setPath(it.value.toString())
            .build()
        }
        BazelSyncMetadata.RepoMapping.newBuilder()
          .putAllApparentToCanonical(obj.apparentToCanonical)
          .putAllCanonicalToPath(canonicalToPath)
          .build()
      }
    )
  }
}
