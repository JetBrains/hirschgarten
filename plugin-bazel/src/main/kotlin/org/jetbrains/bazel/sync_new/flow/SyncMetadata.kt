package org.jetbrains.bazel.sync_new.flow

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.jetbrains.bazel.sync_new.proto.BazelPath
import com.jetbrains.bazel.sync_new.proto.BazelSyncMetadata
import org.jetbrains.bazel.sync_new.codec.converterOf
import java.nio.file.Path

data class SyncMetadata(
  val repoMapping: SyncRepoMapping = DisabledSyncRepoMapping,
)

sealed interface SyncRepoMapping

object DisabledSyncRepoMapping : SyncRepoMapping

data class BzlmodSyncRepoMapping(
  val apparentToCanonical: BiMap<String, String> = HashBiMap.create(),
  val canonicalToPath: Map<String, Path> = mapOf()
) : SyncRepoMapping
