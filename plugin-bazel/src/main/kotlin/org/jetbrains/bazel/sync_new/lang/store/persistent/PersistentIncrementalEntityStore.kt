package org.jetbrains.bazel.sync_new.lang.store.persistent

import com.dynatrace.hash4j.hashing.HashValue128
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.IntArrayListCodec
import org.jetbrains.bazel.sync_new.codec.hash128Codec
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.codec.ofInt
import org.jetbrains.bazel.sync_new.codec.versionedCodecOf
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntity
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityCreator
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.IncrementalResourceId
import org.jetbrains.bazel.sync_new.lang.store.IncrementalResourceIdCreator
import org.jetbrains.bazel.sync_new.lang.store.idInt
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.set

abstract class PersistentIncrementalEntityStore<R : IncrementalResourceId, E : IncrementalEntity> : IncrementalEntityStore<R, E> {
  protected abstract val metadataStore: FlatStorage<PersistentIncrementalEntityStoreMetadata>
  protected abstract val resourceId2EntityStore: KVStore<HashValue128, E>
  protected abstract val resourceHash2Id: KVStore<HashValue128, Int>
  protected abstract val id2Resource: KVStore<Int, R>

  // TODO: maybe make it in memory only?
  protected abstract val id2Successors: KVStore<Int, IntArrayList>

  override fun createEntity(resourceId: R, creator: IncrementalEntityCreator<E>): E {
    val hash = hashResourceId(resourceId)
    val intId = getIdFromResourceId(resourceId)
    return resourceId2EntityStore.computeIfAbsent(hash) { creator.create(intId) }
      ?: error("Failed to create entity")
  }

  override fun modifyEntity(resourceId: R, modifier: (E) -> E): E? {
    val hash = hashResourceId(resourceId)
    return resourceId2EntityStore.compute(hash) { k, v ->
      if (v == null) {
        null
      } else {
        modifier(v)
      }
    }
  }

  override fun removeEntity(resourceId: R): E? {
    val entity = resourceId2EntityStore.remove(hashResourceId(resourceId))
    if (entity != null) {
      val resource = id2Resource.remove(getIdFromResourceId(resourceId))
      resource?.let { resourceHash2Id.remove(hashResourceId(it)) }
    }
    return entity
  }

  override fun getEntity(resourceId: R): E? {
    return resourceId2EntityStore[hashResourceId(resourceId)]
  }

  override fun createResourceId(creator: IncrementalResourceIdCreator<R>): R {
    val newId = metadataStore.modify { it.copy(resourceIdCounter = it.resourceIdCounter + 1) }.resourceIdCounter
    val newResource = creator.create(newId)
    if (newResource.idInt != newId) {
      error("Resource id mismatch")
    }
    id2Resource[newId] = newResource
    resourceHash2Id[hashResourceId(newResource)] = newId
    return newResource
  }

  override fun addDependency(from: R, to: R) {
    val fromId = getIdFromResourceId(from)
    val toId = getIdFromResourceId(to)
    id2Successors.compute(fromId) { _, v ->
      val list = v ?: IntArrayList(IntArray(toId))
      list.add(toId)
      list
    }
  }

  override fun getTransitiveDependants(resourceId: R): Sequence<R> = sequence {
    val queue = ArrayDeque<Int>()
    val visited = IntOpenHashSet()
    queue.add(getIdFromResourceId(resourceId))
    while (queue.isNotEmpty()) {
      val id = queue.removeFirstOrNull()
      if (id == null || visited.contains(id)) {
        continue
      }
      visited.add(id)
      id2Resource[id]?.let { yield(it) }
      id2Successors[id]?.forEach { queue.add(it) }
    }
  }

  protected abstract fun hashResourceId(resourceId: R): HashValue128

  private fun getIdFromResourceId(resourceId: R): Int {
    if (resourceId.idInt <= 0) {
      val id = resourceHash2Id[hashResourceId(resourceId)]
      if (id != null) {
        return id
      }
    }
    return resourceId.idInt
  }
}

inline fun <reified R : IncrementalResourceId, reified E : IncrementalEntity> createPersistentIncrementalEntityStore(
  storageContext: StorageContext,
  name: String,
  crossinline resourceIdCodec: CodecBuilder.() -> Codec<R>,
  crossinline entityCodec: CodecBuilder.() -> Codec<E>,
  crossinline idHasher: R.() -> HashValue128,
) = object : PersistentIncrementalEntityStore<R, E>() {
  override val metadataStore: FlatStorage<PersistentIncrementalEntityStoreMetadata> =
    storageContext.createFlatStore<PersistentIncrementalEntityStoreMetadata>("bazel.sync.lang.entity_store.${name}")
      .withCreator { PersistentIncrementalEntityStoreMetadata() }
      .withCodec { ofKryo() }
      .build()
  override val resourceId2EntityStore: KVStore<HashValue128, E> =
    storageContext.createKVStore<HashValue128, E>("bazel.sync.lang.entity_store.${name}.entities", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { hash128Codec }
      .withValueCodec { entityCodec() }
      .build()
  override val resourceHash2Id: KVStore<HashValue128, Int> =
    storageContext.createKVStore<HashValue128, Int>(
      "bazel.sync.lang.entity_store.${name}.resource_hash_to_id",
      StorageHints.USE_PAGED_STORE,
    )
      .withKeyCodec { hash128Codec }
      .withValueCodec { ofInt() }
      .build()

  override val id2Resource: KVStore<Int, R> =
    storageContext.createKVStore<Int, R>("bazel.sync.lang.entity_store.${name}.resources", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofInt() }
      .withValueCodec { resourceIdCodec() }
      .build()

  override val id2Successors: KVStore<Int, IntArrayList> =
    storageContext.createKVStore<Int, IntArrayList>("bazel.sync.lang.entity_store.${name}.successors", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofInt() }
      .withValueCodec {
        versionedCodecOf(
          version = 1,
          encode = { ctx, buffer, value ->
            IntArrayListCodec.encode(ctx, buffer, value)
          },
          decode = { ctx, buffer, version ->
            check(version == 1) { "unsupported version" }
            IntArrayListCodec.decode(ctx, buffer)
          },
        )
      }

  override fun hashResourceId(resourceId: R): HashValue128 = idHasher(resourceId)

}
