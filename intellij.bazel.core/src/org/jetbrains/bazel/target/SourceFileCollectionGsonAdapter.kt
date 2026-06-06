package org.jetbrains.bazel.target

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.jetbrains.bazel.sync.workspace.snapshot.TrieSourceFileCollection
import org.jetbrains.bazel.sync.workspace.snapshot.TrieNode
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

// temporary serializer before serializing entire `WorkspaceSnapshot` for target utils
internal object SourceFileCollectionGsonAdapter : TypeAdapter<SourceFileCollection>() {
  private const val FIELD_ROOT = "a"
  private const val FIELD_TRIE = "b"
  private const val FIELD_EXTERNALS = "c"
  private const val FIELD_TERMINAL = "d"
  private const val FIELD_CHILDREN = "e"
  private const val FIELD_SEGMENT = "f"

  override fun write(out: JsonWriter, value: SourceFileCollection?) {
    if (value == null) {
      out.nullValue()
      return
    }
    val trie = value as? TrieSourceFileCollection
    if (trie == null) {
      require(value.isEmpty()) { "Unsupported `${SourceFileCollection::class}` type, `${value.javaClass}`" }
      out.beginObject()
      out.endObject()
      return
    }

    out.beginObject()

    trie.relativizeRoot?.let {
      out.name(FIELD_ROOT)
      out.value(it.invariantSeparatorsPathString)
    }

    if (!trie.root.isEmpty()) {
      out.name(FIELD_TRIE)
      writeNode(out, trie.root)
    }

    if (trie.externalFiles.isNotEmpty()) {
      out.name(FIELD_EXTERNALS)
      out.beginArray()
      for (path in trie.externalFiles) {
        out.value(path.invariantSeparatorsPathString)
      }
      out.endArray()
    }

    out.endObject()
  }

  private fun writeNode(out: JsonWriter, node: TrieNode, isRoot: Boolean = false) {
    out.beginObject()
    if (!isRoot) {
      out.name(FIELD_SEGMENT)
      out.value(node.segment)
    }
    if (node.isTerminal) {
      out.name(FIELD_TERMINAL)
      out.value(true)
    }
    if (node.children.isNotEmpty()) {
      out.name(FIELD_CHILDREN)
      out.beginArray()
      for (child in node.children) {
        writeNode(out, child)
      }
      out.endArray()
    }
    out.endObject()
  }

  override fun read(reader: JsonReader): SourceFileCollection? {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull()
      return null
    }
    reader.beginObject()

    var root: Path? = null
    var trieRoot: TrieNode? = null
    var externals: List<Path>? = null

    while (reader.hasNext()) {
      when (reader.nextName()) {
        FIELD_ROOT -> root = if (reader.peek() == JsonToken.NULL) {
          reader.nextNull(); null
        }
        else Path.of(reader.nextString())

        FIELD_TRIE -> trieRoot = readNode(reader, isRoot = true)
        FIELD_EXTERNALS -> externals = readPathArray(reader)
        else -> reader.skipValue()
      }
    }
    reader.endObject()

    if (root == null && trieRoot == null && externals == null) {
      return SourceFileCollection.EMPTY
    }

    return TrieSourceFileCollection(
      relativizeRoot = root,
      root = trieRoot ?: TrieNode(segment = ""),
      externalFiles = externals ?: emptyList(),
    )
  }

  private fun readNode(reader: JsonReader, isRoot: Boolean = false): TrieNode {
    var segment = ""
    var isTerminal = false
    val children = ArrayList<TrieNode>()
    reader.beginObject()
    while (reader.hasNext()) {
      when (reader.nextName()) {
        FIELD_SEGMENT -> segment = reader.nextString()
        FIELD_TERMINAL -> isTerminal = reader.nextBoolean()
        FIELD_CHILDREN -> {
          reader.beginArray()
          while (reader.hasNext()) {
            children.add(readNode(reader))
          }
          reader.endArray()
          children.sortBy { it.segment }
        }

        else -> reader.skipValue()
      }
    }
    reader.endObject()
    require(isRoot || segment.isNotEmpty()) { "illegal trie node" }
    return TrieNode(segment = segment, children = children, isTerminal = isTerminal)
  }

  private fun readPathArray(reader: JsonReader): List<Path> {
    reader.beginArray()
    val buf = ArrayList<Path>()
    while (reader.hasNext()) {
      buf.add(Path.of(reader.nextString()))
    }
    reader.endArray()
    return buf
  }
}
