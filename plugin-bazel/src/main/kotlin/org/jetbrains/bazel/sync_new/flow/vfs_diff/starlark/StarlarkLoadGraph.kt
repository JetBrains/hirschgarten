package org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofSet
import org.jetbrains.bazel.sync_new.graph.PersistentDirectedGraph
import org.jetbrains.bazel.sync_new.graph.SimpleGraphTraversalDirection
import org.jetbrains.bazel.sync_new.graph.SimpleGraphTraversalResult
import org.jetbrains.bazel.sync_new.graph.SimpleGraphTraverser
import org.jetbrains.bazel.sync_new.graph.SimpleGraphVisitor
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.storageContext
import java.nio.file.Path

class StarlarkLoadGraph(
  private val project: Project,
) {

  val graph: PersistentDirectedGraph<HashValue128, StarlarkFileNode> =
    object : PersistentDirectedGraph<HashValue128, StarlarkFileNode>() {
      override val id2Vertex: KVStore<HashValue128, StarlarkFileNode> =
        project.storageContext.createKVStore<HashValue128, StarlarkFileNode>(
          "bazel.sync.vfs_diff.starlark.id2File",
          StorageHints.USE_PAGED_STORE,
        )
          .withKeyCodec { ofHash128() }
          .withValueCodec { ofKryo() }
          .build()

      override val id2Successors: KVStore<HashValue128, Set<HashValue128>> =
        project.storageContext.createKVStore<HashValue128, Set<HashValue128>>(
          "bazel.sync.vfs_diff.starlark.id2Successors",
          StorageHints.USE_PAGED_STORE,
        )
          .withKeyCodec { ofHash128() }
          .withValueCodec { ofSet(ofHash128()) }
          .build()

      override val id2Predecessors: KVStore<HashValue128, Set<HashValue128>> =
        project.storageContext.createKVStore<HashValue128, Set<HashValue128>>(
          "bazel.sync.vfs_diff.starlark.id2Successors",
          StorageHints.USE_PAGED_STORE,
        )
          .withKeyCodec { ofHash128() }
          .withValueCodec { ofSet(ofHash128()) }
          .build()

      override fun getVertexId(vertex: StarlarkFileNode): HashValue128 = StarlarkFileUtils.hashWorkspacePath(vertex.workspacePath)

    }

  fun getBuildPredecessors(file: Path): List<StarlarkFileNode> {
    val file = getStarlarkFile(file) ?: return emptyList()
    val files = mutableListOf<StarlarkFileNode>()
    val visitor = SimpleGraphVisitor<StarlarkFileNode> {
      if (it.kind == StarlarkFileKind.BUILD) {
        files.add(it)
        SimpleGraphTraversalResult.SKIP
      } else {
        SimpleGraphTraversalResult.CONTINUE
      }
    }
    SimpleGraphTraverser.topological(
      graph = graph,
      roots = listOf(file),
      visitor = visitor,
      direction = SimpleGraphTraversalDirection.UPWARD,
    )
    return files
  }

  fun getStarlarkFile(file: Path): StarlarkFileNode? = graph.getVertexById(StarlarkFileUtils.hashWorkspacePath(file))

}
