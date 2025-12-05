package org.jetbrains.bazel.sync_new.lang.store.persistent

import com.dynatrace.hash4j.hashing.HashValue128
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.Int2ObjectOpenHashMapCodec
import org.jetbrains.bazel.sync_new.codec.IntArrayListCodec
import org.jetbrains.bazel.sync_new.codec.hash128Codec
import org.jetbrains.bazel.sync_new.codec.ofInt
import org.jetbrains.bazel.sync_new.codec.versionedCodecOf
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntity
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityCreator
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.IncrementalResourceId
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.set

private const val EMPTY_ID: Int = 0

abstract class PersistentIncrementalEntityStore<R : IncrementalResourceId, E : IncrementalEntity> : IncrementalEntityStore<R, E> {
  data class PersistentMetadata(
    var nextId: Int = 1,
    var id2Successors: Int2ObjectOpenHashMap<IntArrayList> = Int2ObjectOpenHashMap(),
    var id2Predecessors: Int2ObjectOpenHashMap<IntArrayList> = Int2ObjectOpenHashMap(),
  ) {
    companion object {
      val codec: Codec<PersistentMetadata> = versionedCodecOf(
        version = 1,
        encode = { ctx, buffer, value ->
          buffer.writeVarInt(value.nextId)
          Int2ObjectOpenHashMapCodec.encode(ctx, buffer, value.id2Successors) { buffer, value ->
            IntArrayListCodec.encode(
              ctx,
              buffer,
              value,
            )
          }
          Int2ObjectOpenHashMapCodec.encode(ctx, buffer, value.id2Predecessors) { buffer, value ->
            IntArrayListCodec.encode(
              ctx,
              buffer,
              value,
            )
          }
        },
        decode = { ctx, buffer, version ->
          check(version == 1) { "unsupported version" }
          val metadata = PersistentMetadata()
          metadata.nextId = buffer.readVarInt()
          metadata.id2Successors = Int2ObjectOpenHashMapCodec.decode(ctx, buffer) { buffer -> IntArrayListCodec.decode(ctx, buffer) }
          metadata.id2Predecessors = Int2ObjectOpenHashMapCodec.decode(ctx, buffer) { buffer -> IntArrayListCodec.decode(ctx, buffer) }
          metadata
        },
      )
    }
  }

  protected abstract val metadataStore: FlatStorage<PersistentMetadata>
  protected abstract val resourceId2EntityStore: KVStore<Int, E>
  protected abstract val resourceHash2Id: KVStore<HashValue128, Int>
  protected abstract val id2Resource: KVStore<Int, R>

  override fun createEntity(resourceId: R, creator: IncrementalEntityCreator<E>): E {
    val id = getOrCreateIdFromResourceId(resourceId)
    return resourceId2EntityStore.computeIfAbsent(id) { creator.create(id) }
      ?: error("Failed to create entity")
  }

  override fun modifyEntity(resourceId: R, modifier: (E) -> E): E? {
    val id = getIdFromResourceId(resourceId)
    if (id == EMPTY_ID) {
      return null
    }
    return resourceId2EntityStore.compute(id) { _, v ->
      if (v == null) {
        null
      } else {
        modifier(v)
      }
    }
  }

  override fun removeEntity(resourceId: R): E? {
    val id = getIdFromResourceId(resourceId)
    if (id == EMPTY_ID) {
      return null
    }
    val entity = resourceId2EntityStore.remove(id)
    if (entity != null) {
      val metadata = metadataStore.get()
      id2Resource.remove(id)

      val successors = metadata.id2Successors.get(id)
      if (successors != null) {
        for (i in successors.indices) {
          val successorId = successors.getInt(i)
          val predecessorsList = metadata.id2Predecessors.get(successorId)
          if (predecessorsList != null) {
            predecessorsList.rem(id)
          }
        }
      }

      val predecessors = metadata.id2Predecessors.get(id)
      if (predecessors != null) {
        for (i in predecessors.indices) {
          val predecessorId = predecessors.getInt(i)
          val successorsList = metadata.id2Successors.get(predecessorId)
          if (successorsList != null) {
            successorsList.rem(id)
          }
        }
      }

      metadata.id2Successors.remove(id)
      metadata.id2Predecessors.remove(id)

      metadataStore.set(metadata)
    }
    return entity
  }

  override fun getEntity(resourceId: R): E? {
    val id = getIdFromResourceId(resourceId)
    if (id == EMPTY_ID) {
      return null
    }
    return resourceId2EntityStore[id]
  }

  override fun addDependency(from: R, to: R) {
    val fromId = getOrCreateIdFromResourceId(from)
    val toId = getOrCreateIdFromResourceId(to)
    if (fromId == EMPTY_ID || toId == EMPTY_ID) {
      return
    }
    metadataStore.modify {
      it.id2Successors.computeIfAbsent(fromId) { IntArrayList() }.add(toId)
      it.id2Predecessors.computeIfAbsent(toId) { IntArrayList() }.add(fromId)
      it
    }
  }

  override fun getAllEntities(): Sequence<E> = resourceId2EntityStore.values()

  override fun getTransitiveDependants(resourceId: R): Sequence<R> {
    val id = getIdFromResourceId(resourceId)
    if (id == EMPTY_ID) {
      return emptySequence()
    }
    val metadata = metadataStore.get()
    val idSeq = sequence {
      val queue = ArrayDeque<Int>() // TODO: use IntArrayFIFOQueue
      val visited = IntOpenHashSet()
      queue.add(id)
      while (true) {
        val id = queue.removeFirstOrNull() ?: break
        if (visited.contains(id)) {
          continue
        }
        visited.add(id)
        yield(id)
        val successors = metadata.id2Successors[id]
        if (successors != null) {
          queue.addAll(successors)
        }
      }
    }
    return idSeq.mapNotNull { id2Resource[it] }
  }

  override fun getDirectReferrers(resourceId: R): Sequence<R> {
    val id = getIdFromResourceId(resourceId)
    if (id == EMPTY_ID) {
      return emptySequence()
    }
    val metadata = metadataStore.get()
    return metadata.id2Predecessors[id]?.asSequence()
      ?.mapNotNull { id2Resource[it] }
      ?: emptySequence()
  }

  override fun clear() {
    resourceId2EntityStore.clear()
    resourceHash2Id.clear()
    id2Resource.clear()
    metadataStore.reset()
  }

  protected abstract fun hashResourceId(resourceId: R): HashValue128

  private fun getIdFromResourceId(resourceId: R): Int {
    return resourceHash2Id[hashResourceId(resourceId)] ?: EMPTY_ID
  }

  private fun getOrCreateIdFromResourceId(resourceId: R): Int {
    val id = getIdFromResourceId(resourceId)
    if (id != EMPTY_ID) {
      return id
    }
    val newId = metadataStore.modify { it.copy(nextId = it.nextId + 1) }.nextId
    resourceHash2Id[hashResourceId(resourceId)] = newId
    id2Resource[newId] = resourceId
    return newId
  }
}

inline fun <reified R : IncrementalResourceId, reified E : IncrementalEntity> createPersistentIncrementalEntityStore(
  storageContext: StorageContext,
  name: String,
  crossinline resourceIdCodec: CodecBuilder.() -> Codec<R>,
  crossinline entityCodec: CodecBuilder.() -> Codec<E>,
  crossinline idHasher: R.() -> HashValue128,
): PersistentIncrementalEntityStore<R, E> = object : PersistentIncrementalEntityStore<R, E>() {
  override val metadataStore: FlatStorage<PersistentMetadata> = storageContext.createFlatStore<PersistentMetadata>("bazel.sync.lang.entity_store.${name}.metadata")
    .withCreator { PersistentMetadata() }
    .withCodec { PersistentMetadata.codec }
    .build()

  override val resourceId2EntityStore: KVStore<Int, E> =
    storageContext.createKVStore<Int, E>("bazel.sync.lang.entity_store.${name}.entities", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofInt() }
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

  override fun hashResourceId(resourceId: R): HashValue128 = idHasher(resourceId)

}
