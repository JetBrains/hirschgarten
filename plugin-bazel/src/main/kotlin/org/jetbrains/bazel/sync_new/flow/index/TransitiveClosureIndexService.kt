package org.jetbrains.bazel.sync_new.flow.index

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntLists
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import org.jetbrains.bazel.sync_new.codec.Int2ObjectOpenHashMapCodec
import org.jetbrains.bazel.sync_new.codec.IntArrayCodec
import org.jetbrains.bazel.sync_new.codec.IntOpenHashSetCodec
import org.jetbrains.bazel.sync_new.codec.LongArrayCodec
import org.jetbrains.bazel.sync_new.codec.versionedCodecOf
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetTag
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdater
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.mutate
import org.jetbrains.bazel.sync_new.storage.storageContext
import java.util.BitSet

@Service(Service.Level.PROJECT)
class TransitiveClosureIndexService(
  private val project: Project,
) : SyncIndexUpdater {

  private val transitiveClosure: FlatStorage<TransitiveClosure> =
    project.storageContext.createFlatStore<TransitiveClosure>(
      "target2ReverseTransitiveExecutableTargetIds",
      StorageHints.USE_IN_MEMORY,
    )
      .withCreator { TransitiveClosure() }
      .withCodec {
        versionedCodecOf(
          version = 1,
          encode = { ctx, buffer, value ->
            Int2ObjectOpenHashMapCodec.encode(ctx, buffer, value.vertexId2TransitiveClosure) { buffer, value ->
              LongArrayCodec.encode(ctx, buffer, value.toLongArray())
            }
          },
          decode = { ctx, buffer, version ->
            check(version == 1) { "unsupported version" }
            val result = TransitiveClosure()
            result.idx2VertexId = IntArrayCodec.decode(ctx, buffer)
            result.vertexId2TransitiveClosure =
              Int2ObjectOpenHashMapCodec.decode(ctx, buffer) { buffer ->
                BitSet.valueOf(LongArrayCodec.decode(ctx, buffer))
              }
            result
          },
        )
      }
      .build()

  private val executableTargets: FlatStorage<IntOpenHashSet> =
    project.storageContext.createFlatStore<IntOpenHashSet>("executableTargets", StorageHints.USE_IN_MEMORY)
      .withCreator { IntOpenHashSet() }
      .withCodec {
        versionedCodecOf(
          version = 1,
          encode = { ctx, buffer, value ->
            IntOpenHashSetCodec.encode(ctx, buffer, value)
          },
          decode = { ctx, buffer, version ->
            check(version == 1) { "unsupported version" }
            IntOpenHashSetCodec.decode(ctx, buffer)
          },
        )
      }
      .build()

  override suspend fun updateIndexes(ctx: SyncContext, diff: SyncDiff) {
    if (ctx.scope.isFullSync) {
      transitiveClosure.reset()
      executableTargets.reset()
    }

    val (changed, removed) = diff.split
    for (removed in removed) {
      val target = removed.getBuildTarget() ?: continue
      executableTargets.mutate { set -> set.remove(target.vertexId) }
    }

    for (changed in changed) {
      val target = changed.getBuildTarget() ?: continue
      if (target.genericData.tags.contains(BazelTargetTag.EXECUTABLE)) {
        executableTargets.mutate { set -> set.add(target.vertexId) }
      }
    }

    computeTransitiveClosure(ctx)
  }

  private fun computeTransitiveClosure(ctx: SyncContext) {
    val execs = executableTargets.get().toIntArray()

    // construct vertexId -> index mapping for bitsets
    val exec2Index = Int2IntOpenHashMap()
    for (n in execs.indices) {
      exec2Index.put(execs[n], n)
    }
    transitiveClosure.mutate { it.idx2VertexId = execs }

    val result = Int2ObjectOpenHashMap<BitSet>()
    val state = Int2ByteOpenHashMap()
    val stack = ArrayDeque<Int>() // TODO: use IntArrayFIFOQueue

    val iter = ctx.graph.getAllVertexIds().iterator()
    while (iter.hasNext()) {
      val startVertexId = iter.nextInt()
      if (state.getOrDefault(startVertexId, TransitiveClosure.STATE_NOT_VISITED) == TransitiveClosure.STATE_VISITED) {
        continue
      }
      stack.addLast(startVertexId)
      while (stack.isNotEmpty()) {
        val vertexId = stack.last()

        when (state.getOrDefault(vertexId, TransitiveClosure.STATE_NOT_VISITED)) {
          TransitiveClosure.STATE_NOT_VISITED -> {
            state.put(vertexId, TransitiveClosure.STATE_VISITING)
            val rdeps = ctx.graph.getPredecessors(vertexId)
            for (n in rdeps.indices) {
              val rdepVertexId = rdeps.getInt(n)
              if (state.getOrDefault(rdepVertexId, TransitiveClosure.STATE_NOT_VISITED) != TransitiveClosure.STATE_VISITED) {
                stack.addLast(rdepVertexId)
              }
            }
          }

          TransitiveClosure.STATE_VISITING -> {
            stack.removeLast()
            state.put(vertexId, TransitiveClosure.STATE_VISITED)

            val rdeps = ctx.graph.getPredecessors(vertexId)
            var bits: BitSet? = null
            for (n in rdeps.indices) {
              val rdepBit = result.get(n) ?: continue
              if (bits == null) {
                bits = rdepBit.clone() as BitSet
              } else {
                bits.or(rdepBit)
              }
            }

            val execIdx = exec2Index.getOrDefault(vertexId, -1)
            if (execIdx >= 0) {
              if (bits == null) {
                bits = BitSet(execs.size)
                bits.set(execIdx)
              }
            }

            if (bits != null) {
              result[vertexId] = bits
            }
          }

          TransitiveClosure.STATE_VISITED -> {
            stack.removeLast()
          }
        }
      }
    }

    transitiveClosure.mutate { it.vertexId2TransitiveClosure = result }
  }

  fun getAllReverseTransitiveTargetIds(vertexId: Int): IntList = transitiveClosure.get().getAllReverseTransitiveTargetIds(vertexId, null)

  fun getAllReverseTransitiveExecutableTargetIds(vertexId: Int): IntList =
    transitiveClosure.get().getAllReverseTransitiveTargetIds(vertexId, executableTargets.get())
}

private class TransitiveClosure(
  var idx2VertexId: IntArray = IntArray(0),
  var vertexId2TransitiveClosure: Int2ObjectOpenHashMap<BitSet> = Int2ObjectOpenHashMap(),
) {
  companion object {
    const val STATE_NOT_VISITED: Byte = 0
    const val STATE_VISITING: Byte = 1
    const val STATE_VISITED: Byte = 2
  }

  fun getAllReverseTransitiveTargetIds(vertexId: Int, filter: IntSet? = null): IntList {
    val closure = vertexId2TransitiveClosure[vertexId] ?: return IntLists.EMPTY_LIST
    val result = IntArrayList()
    var idx = closure.nextSetBit(0)
    while (idx >= 0) {
      val vertexId = idx2VertexId[idx]
      if (filter == null || filter.contains(vertexId)) {
        result.add(vertexId)
      }
      idx = closure.nextSetBit(idx + 1)
    }
    return result
  }
}
