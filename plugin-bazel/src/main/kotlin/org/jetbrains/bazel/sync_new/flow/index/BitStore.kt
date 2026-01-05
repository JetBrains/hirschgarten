package org.jetbrains.bazel.sync_new.flow.index

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.jetbrains.bazel.sync_new.codec.IntOpenHashSetCodec
import org.jetbrains.bazel.sync_new.codec.LongArrayCodec
import org.jetbrains.bazel.sync_new.codec.versionedCodecOf
import java.util.BitSet

/**
 * bit set implementation with size-dependent backing storage
 * main purpose of this store to balance memory usage(also size on disk) and performance
 * at low-store-sized bitset will be used, otherwise hashset will be used
 */
sealed interface BitStore {
  fun set(bit: Int)
  fun clear(bit: Int)
  fun or(other: BitStore)

  fun each(fn: (Int) -> Unit)
  fun copy(): BitStore

  companion object {
    private const val DEFAULT_THRESHOLD = 512

    private const val BITSET_TYPE = 1
    private const val HASHSET_TYPE = 2
    internal val codec = versionedCodecOf(
      version = 1,
      encode = { ctx, buffer, value ->
        when (value) {
          is BitSetBitStore -> {
            buffer.writeVarInt(BITSET_TYPE)
            LongArrayCodec.encode(ctx, buffer, value.set.toLongArray())
          }

          is HashSetBitStore -> {
            buffer.writeVarInt(HASHSET_TYPE)
            IntOpenHashSetCodec.encode(ctx, buffer, value.set)
          }
        }
      },
      decode = { ctx, buffer, version ->
        check(version == 1) { "unsupported version" }
        val type = buffer.readVarInt()
        when (type) {
          BITSET_TYPE -> {
            val bitset = BitSet.valueOf(LongArrayCodec.decode(ctx, buffer))
            BitSetBitStore(bitset)
          }

          HASHSET_TYPE -> {
            val hashset = IntOpenHashSetCodec.decode(ctx, buffer)
            HashSetBitStore(hashset)
          }

          else -> error("unsupported type")
        }
      },
    )

    fun create(size: Int, threshold: Int = DEFAULT_THRESHOLD): BitStore {
      return if (size <= threshold) {
        BitSetBitStore(BitSet(size))
      }
      else {
        HashSetBitStore(IntOpenHashSet())
      }
    }
  }
}

private class BitSetBitStore(val set: BitSet) : BitStore {
  override fun or(other: BitStore) {
    set.or(other.bitset)
  }

  override fun copy(): BitStore {
    return BitSetBitStore(set.clone() as BitSet)
  }

  override fun set(bit: Int) {
    set.set(bit)
  }

  override fun clear(bit: Int) {
    set.clear(bit)
  }

  override fun each(fn: (Int) -> Unit) {
    var idx = set.nextSetBit(0)
    while (idx >= 0) {
      fn(idx)
      idx = set.nextSetBit(idx + 1)
    }
  }

  private val BitStore.bitset
    get() = (this as? BitSetBitStore)?.set ?: error("unsupported store")
}

private class HashSetBitStore(val set: IntOpenHashSet) : BitStore {
  override fun set(bit: Int) {
    set.add(bit)
  }

  override fun clear(bit: Int) {
    set.remove(bit)
  }

  override fun or(other: BitStore) {
    set.addAll(other.hashset)
  }

  override fun each(fn: (Int) -> Unit) {
    // using iterator() to avoid boxing
    val iter = set.iterator()
    while (iter.hasNext()) {
      fn(iter.nextInt())
    }
  }

  override fun copy(): BitStore {
    return HashSetBitStore(set.clone())
  }

  private val BitStore.hashset
    get() = (this as? HashSetBitStore)?.set ?: error("unsupported store")
}
