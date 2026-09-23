package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.PathsTrie
import org.jetbrains.bazel.sync.workspace.snapshot.TrieNode
import org.jetbrains.bazel.sync.workspace.snapshot.TrieOutputLocationCollection
import org.jetbrains.bazel.sync.workspace.snapshot.TrieSourceFileCollection
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.OutputRoot
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path
import java.util.TreeMap
import kotlin.io.path.Path

// TODO: Ideas of improving overall snapshot size:
//        - using string pool for segments inside each path/label
//        - use string pool IDs instead of inline encoded string for trie serializer
//  but those are risky because, string reader/writer use ConcurrentHashMap and key values are boxed,
//  so we get boxing, atomics maybe even full monitor lock overhead for each segment which is not ideal,
//  that's why it would need extensive profiling

internal object SnapshotSerializers {
  fun registerBuiltinTypes(kryo: Kryo) {
    // hierarchies serialized via canonical strings, look at `SnapshotClassResolver` for more context
    kryo.register(Path::class.java, PathSerializer())
    kryo.register(Label::class.java, LabelSerializer())

    // common collections
    kryo.register(ArrayList::class.java)
    kryo.register(LinkedHashMap::class.java)
    kryo.register(HashMap::class.java)
    kryo.register(LinkedHashSet::class.java)
    kryo.register(HashSet::class.java)

    // every serializable type have to be explicitly registered
    // listOf, mapOf and similar functions produce different implementations of collection
    // based on element count, that's why we have to register each of them separately
    kryo.register(emptyList<Any>().javaClass, SingletonSerializer(emptyList<Any>()))
    kryo.register(emptyMap<Any, Any>().javaClass, SingletonSerializer(emptyMap<Any, Any>()))
    kryo.register(emptySet<Any>().javaClass, SingletonSerializer(emptySet<Any>()))
    kryo.register(listOf(0, 0).javaClass)
    kryo.register(listOf(0).javaClass)
    kryo.register(mapOf(0 to 0).javaClass)
    kryo.register(IntArray::class.java)
    kryo.register(LongArray::class.java)
    kryo.register(Array<String>::class.java)
    kryo.register(setOf(0).javaClass)
    kryo.register(TreeMap::class.java)
    kryo.register(Array<IntArray>::class.java)
    kryo.register(Array<WorkspaceTargetKey>::class.java)

    // fastutil primitive maps have a single generic parameter, which breaks Kryo's default MapSerializer
    kryo.register(Object2IntOpenHashMap::class.java, Object2IntOpenHashMapSerializer())
    kryo.register(Int2ObjectOpenHashMap::class.java, Int2ObjectOpenHashMapSerializer())
    kryo.register(Object2ObjectOpenHashMap::class.java)
    kryo.register(IntOpenHashSet::class.java)

    // custom collections
    kryo.register(Int2ObjectBiMap::class.java, Int2ObjectBiMapSerializer())

    // anonymous-object singletons cannot pass WorkspaceTypeContributor schema validation
    kryo.register(SourceFileCollection.EMPTY.javaClass, SingletonSerializer(SourceFileCollection.EMPTY))
    kryo.register(WorkspaceTargetGraph.EMPTY.javaClass, SingletonSerializer(WorkspaceTargetGraph.EMPTY))

    kryo.register(TrieSourceFileCollection::class.java, TrieSourceFileCollectionSerializer())
    kryo.register(OutputLocationCollection.EMPTY.javaClass, SingletonSerializer(OutputLocationCollection.EMPTY))
    kryo.register(TrieOutputLocationCollection::class.java, TrieOutputLocationCollectionSerializer())
    kryo.register(OutputRoot::class.java, OutputRootSerializer())
  }

  fun singletonSerializerFor(type: Class<*>): VersionedKryoSerializer<*> =
    SingletonSerializer(type.getDeclaredField("INSTANCE").get(null))
}

// manual trie serializer to avoid reflective access, optimized based on profiling results
internal class TrieSourceFileCollectionSerializer : VersionedKryoSerializer<TrieSourceFileCollection>() {
  init {
    isImmutable = true
  }

  override val binaryFormatVersion: Int = 2

  override fun write(kryo: Kryo, output: Output, obj: TrieSourceFileCollection) {
    kryo.writeObjectOrNull(output, obj.relativizeRoot, Path::class.java)
    output.writeVarInt(obj.externalFiles.size, true)
    for (path in obj.externalFiles) {
      kryo.writeObject(output, path)
    }
    writeTrieNode(kryo, output, obj.trie.root)
  }

  override fun read(kryo: Kryo, input: Input, type: Class<out TrieSourceFileCollection>): TrieSourceFileCollection {
    val relativizeRoot = kryo.readObjectOrNull(input, Path::class.java)
    val externalCount = input.readVarInt(true)
    val externalFiles = ArrayList<Path>(externalCount)
    repeat(externalCount) {
      externalFiles.add(kryo.readObject(input, Path::class.java))
    }
    val trie = PathsTrie()
    readTrieNode(kryo, input, trie.root)
    return TrieSourceFileCollection(relativizeRoot = relativizeRoot, trie = trie, externalFiles = externalFiles)
  }
}

internal class TrieOutputLocationCollectionSerializer : VersionedKryoSerializer<TrieOutputLocationCollection>() {
  init {
    isImmutable = true
  }

  override val binaryFormatVersion: Int = 2

  override fun write(kryo: Kryo, output: Output, obj: TrieOutputLocationCollection) {
    output.writeVarInt(obj.roots.size, true)
    for ((root, trie) in obj.roots) {
      kryo.writeClassAndObject(output, root)
      writeTrieNode(kryo, output, trie.root)
    }
  }

  override fun read(kryo: Kryo, input: Input, type: Class<out TrieOutputLocationCollection>): TrieOutputLocationCollection {
    val rootCount = input.readVarInt(true)
    val roots = LinkedHashMap<OutputLocation, PathsTrie>(rootCount)
    repeat(rootCount) {
      val root = kryo.readClassAndObject(input) as OutputLocation
      val trie = PathsTrie()
      readTrieNode(kryo, input, trie.root)
      roots[root] = trie
    }
    return TrieOutputLocationCollection(roots)
  }
}

internal class OutputRootSerializer : VersionedKryoSerializer<OutputRoot>() {
  init {
    isImmutable = true
  }

  override val binaryFormatVersion: Int = 2

  override fun write(kryo: Kryo, output: Output, obj: OutputRoot) {
    output.writeVarInt(obj.segments.size, true)
    for (segment in obj.segments) {
      kryo.writePooledString(output, segment)
    }
  }

  override fun read(kryo: Kryo, input: Input, type: Class<out OutputRoot>): OutputRoot {
    val segmentCount = input.readVarInt(true)
    val segments = ArrayList<String>(segmentCount)
    repeat(segmentCount) {
      segments.add(kryo.readPooledString(input))
    }
    return OutputRoot.of(segments)
  }
}

private fun writeTrieNode(kryo: Kryo, output: Output, node: TrieNode) {
  output.writeVarInt((node.children.orEmpty().size shl 1) or (if (node.isTerminal) 1 else 0), true)
  for (child in node.children.orEmpty()) {
    kryo.writePooledString(output, child.segment)
    writeTrieNode(kryo, output, child)
  }
}

private fun readTrieNode(kryo: Kryo, input: Input, node: TrieNode) {
  val header = input.readVarInt(true)
  node.isTerminal = (header and 1) != 0
  repeat(header ushr 1) {
    val child = TrieNode(segment = kryo.readPooledString(input))
    node.getOrCreateChildren().add(child)
    readTrieNode(kryo, input, child)
  }
}

internal class LabelSerializer : VersionedKryoSerializer<Label>() {
  override val binaryFormatVersion: Int = 2

  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Label,
  ) {
    kryo.writePooledString(output, obj.toString())
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Label>,
  ): Label = Label.parse(kryo.readPooledString(input))
}

internal class Object2IntOpenHashMapSerializer : VersionedKryoSerializer<Object2IntOpenHashMap<Any>>() {
  override val binaryFormatVersion: Int = 1

  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Object2IntOpenHashMap<Any>,
  ) {
    output.writeVarInt(obj.size, true)
    for (entry in obj.object2IntEntrySet()) {
      kryo.writeClassAndObject(output, entry.key)
      output.writeInt(entry.intValue)
    }
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Object2IntOpenHashMap<Any>>,
  ): Object2IntOpenHashMap<Any> {
    val size = input.readVarInt(true)
    val map = Object2IntOpenHashMap<Any>(size)
    repeat(size) {
      val key = kryo.readClassAndObject(input)
      map.put(key, input.readInt())
    }
    return map
  }
}

internal class Int2ObjectOpenHashMapSerializer : VersionedKryoSerializer<Int2ObjectOpenHashMap<Any>>() {
  override val binaryFormatVersion: Int = 1

  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Int2ObjectOpenHashMap<Any>,
  ) {
    output.writeVarInt(obj.size, true)
    for (entry in obj.int2ObjectEntrySet()) {
      output.writeInt(entry.intKey)
      kryo.writeClassAndObject(output, entry.value)
    }
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Int2ObjectOpenHashMap<Any>>,
  ): Int2ObjectOpenHashMap<Any> {
    val size = input.readVarInt(true)
    val map = Int2ObjectOpenHashMap<Any>(size)
    repeat(size) {
      val key = input.readInt()
      map.put(key, kryo.readClassAndObject(input))
    }
    return map
  }
}

internal class Int2ObjectBiMapSerializer : VersionedKryoSerializer<Int2ObjectBiMap<Any>>() {
  override val binaryFormatVersion: Int = 1

  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Int2ObjectBiMap<Any>,
  ) {
    output.writeVarInt(obj.forward.size, true)
    for ((key, value) in obj.forward.int2ObjectEntrySet()) {
      output.writeInt(key)
      kryo.writeClassAndObject(output, value)
    }
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Int2ObjectBiMap<Any>>,
  ): Int2ObjectBiMap<Any> {
    val forward = Int2ObjectOpenHashMap<Any>()
    val reverse = Object2IntOpenHashMap<Any>()
    val size = input.readVarInt(true)
    repeat(size) {
      val key = input.readInt()
      val value = kryo.readClassAndObject(input)
      forward.put(key, value)
      reverse.put(value, key)
    }
    return Int2ObjectBiMap(forward, reverse)
  }

}

private class SingletonSerializer(private val instance: Any) : VersionedKryoSerializer<Any>() {
  override val binaryFormatVersion: Int = 1

  override fun write(kryo: Kryo, output: Output, obj: Any) = Unit

  override fun read(kryo: Kryo, input: Input, type: Class<out Any>): Any = instance
}

private class PathSerializer : VersionedKryoSerializer<Path>() {
  init {
    isImmutable = true
  }

  override val binaryFormatVersion: Int = 4

  override fun write(kryo: Kryo, output: Output, path: Path) = kryo.writePooledString(output, path.toString())

  override fun read(kryo: Kryo, input: Input, type: Class<out Path>): Path = Path(kryo.readPooledString(input))
}

private const val POOLED_STRING_INLINE: Byte = 0
private const val POOLED_STRING_REF: Byte = 1

internal fun Kryo.writePooledString(output: Output, str: String) {
  val table = this.graphContext.get(STRING_TABLE_WRITE_KEY) as StringTableWriter?
  if (table != null) {
    output.writeByte(POOLED_STRING_REF)
    output.writeVarInt(table.idFor(str), true)
  }
  else {
    output.writeByte(POOLED_STRING_INLINE)
    output.writeString(str)
  }
}

internal fun Kryo.readPooledString(input: Input): String {
  return when (val marker = input.readByte()) {
    POOLED_STRING_REF -> {
      val table = this.graphContext.get(STRING_TABLE_READ_KEY) as? StringTableReader?
                  ?: error("string table is required to decode string ref")
      table.get(input.readVarInt(true))
    }

    POOLED_STRING_INLINE -> input.readString()

    else -> error("invalid string marker: $marker")
  }
}
