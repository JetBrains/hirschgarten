package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.InterningOutputLocationCollection
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.OutputRoot
import org.jetbrains.bsp.protocol.StringsCollection
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
    kryo.register(WorkspaceTargetGraph.EMPTY.javaClass, SingletonSerializer(WorkspaceTargetGraph.EMPTY))

    kryo.register(OutputLocationCollection.EMPTY.javaClass, SingletonSerializer(OutputLocationCollection.EMPTY))
    kryo.register(InterningOutputLocationCollection::class.java, InterningOutputLocationCollectionSerializer())
    kryo.register(OutputRoot::class.java, OutputRootSerializer())

    val outputLocationTypes = OutputLocation::class.sealedSubclasses + listOf(OutputLocation::class)
    outputLocationTypes.forEach { type -> kryo.register(type.java, OutputLocationSerializer()) }

    kryo.register(StringsCollection::class.java, StringsCollectionSerializer())
  }

  fun singletonSerializerFor(type: Class<*>): VersionedKryoSerializer<*> =
    SingletonSerializer(type.getDeclaredField("INSTANCE").get(null))
}

// locations go through the ref table, so equal locations are stored once per generation
internal class InterningOutputLocationCollectionSerializer : VersionedKryoSerializer<InterningOutputLocationCollection>() {
  init {
    isImmutable = true
  }

  override val binaryFormatVersion: Int = 3

  override fun write(kryo: Kryo, output: Output, obj: InterningOutputLocationCollection) {
    kryo.writeRefList(output, obj.locations)
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out InterningOutputLocationCollection>,
  ): InterningOutputLocationCollection {
    val locations = kryo.readRefList(input) { it as OutputLocation }
    return InterningOutputLocationCollection.create(locations)
  }
}

// the whole collection goes through the ref table, so equal collections are stored once per generation,
// a ref value is written inline with pooled strings
internal class StringsCollectionSerializer : VersionedKryoSerializer<StringsCollection>() {
  init {
    isImmutable = true
  }

  override val binaryFormatVersion: Int = 1

  override fun write(kryo: Kryo, output: Output, obj: StringsCollection) {
    output.writeVarInt(obj.size, true)
    if (obj.isEmpty()) {
      return
    }
    val table = kryo.graphContext.get(REF_TABLE_WRITE_KEY) as RefTableWriter?
    if (table != null) {
      output.writeByte(REF_ID)
      output.writeVarInt(table.idFor(obj), true)
    }
    else {
      output.writeByte(REF_INLINE)
      for (value in obj.asList()) {
        kryo.writePooledString(output, value)
      }
    }
  }

  override fun read(kryo: Kryo, input: Input, type: Class<out StringsCollection>): StringsCollection {
    val size = input.readVarInt(true)
    if (size == 0) {
      return StringsCollection.EMPTY
    }
    return when (val marker = input.readByte()) {
      REF_ID -> {
        val table = kryo.graphContext.get(REF_TABLE_READ_KEY) as? RefTableReader?
                    ?: error("ref table is required to decode ref")
        table.get(input.readVarInt(true)) as StringsCollection
      }

      REF_INLINE -> {
        val strings = ArrayList<String>(size)
        repeat(size) {
          strings.add(kryo.readPooledString(input))
        }
        StringsCollection.of(strings)
      }

      else -> error("invalid ref marker: $marker")
    }
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

private class OutputLocationSerializer : VersionedKryoSerializer<OutputLocation>() {
  companion object {
    private const val LOCATION_WORKSPACE_ID: Byte = 0
    private const val LOCATION_OUTPUT_ID: Byte = 1
    private const val LOCATION_EXTERNAL_ID: Byte = 2
    private const val LOCATION_HOST_ID: Byte = 3
    private const val RELATIVE_PATH_SEPARATOR = "/"
  }

  override val binaryFormatVersion: Int = 1

  override fun write(
    kryo: Kryo,
    output: Output,
    obj: OutputLocation,
  ) {
    when (obj) {
      is OutputLocation.Workspace -> {
        output.writeByte(LOCATION_WORKSPACE_ID)
        kryo.writePooledPathString(output, obj.relativePath)
      }

      is OutputLocation.Output -> {
        output.writeByte(LOCATION_OUTPUT_ID)
        kryo.writeRef(output, obj.root)
        kryo.writePooledPathString(output, obj.relativePath)
      }

      is OutputLocation.External -> {
        output.writeByte(LOCATION_EXTERNAL_ID)
        kryo.writePooledString(output, obj.repoName)
        kryo.writePooledPathString(output, obj.relativePath)
        output.writeBoolean(obj.siblingLayout)
      }

      is OutputLocation.Host -> {
        output.writeByte(LOCATION_HOST_ID)
        kryo.writePooledPathString(output, obj.absolutePath)
      }
    }
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out OutputLocation>,
  ): OutputLocation =
    when (input.readByte()) {
      LOCATION_WORKSPACE_ID ->
        OutputLocation.Workspace(relativePath = kryo.readPooledPathString(input))

      LOCATION_OUTPUT_ID -> {
        val outputRoot = kryo.readRef(input) as OutputRoot
        val relativePath = kryo.readPooledPathString(input)
        OutputLocation.Output(outputRoot, relativePath)
      }

      LOCATION_EXTERNAL_ID -> {
        val repoName = kryo.readPooledString(input)
        val relativePath = kryo.readPooledPathString(input)
        val siblingLayout = input.readBoolean()
        OutputLocation.External(repoName, relativePath, siblingLayout)
      }

      LOCATION_HOST_ID ->
        OutputLocation.Host(absolutePath = kryo.readPooledPathString(input))


      else -> error("Unknown `${OutputLocation::class}` discriminator")
    }

  private fun Kryo.writePooledPathString(output: Output, str: String) {
    val segments = str.split(RELATIVE_PATH_SEPARATOR)
    output.writeVarInt(segments.size, true)
    for (segment in segments) {
      this.writePooledString(output, segment)
    }
  }

  private fun Kryo.readPooledPathString(input: Input): String {
    val size = input.readVarInt(true)
    val segments = ArrayList<String>(size)
    repeat(size) { segments += this.readPooledString(input) }
    return segments.joinToString(separator = RELATIVE_PATH_SEPARATOR)
  }

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

private const val REF_INLINE: Byte = 0
private const val REF_ID: Byte = 1

internal fun Kryo.writeRef(output: Output, obj: Any) {
  val table = this.graphContext.get(REF_TABLE_WRITE_KEY) as RefTableWriter?
  if (table != null) {
    output.writeByte(REF_ID)
    output.writeVarInt(table.idFor(obj), true)
  }
  else {
    output.writeByte(REF_INLINE)
    writeClassAndObject(output, obj)
  }
}

internal fun Kryo.readRef(input: Input): Any {
  return when (val marker = input.readByte()) {
    REF_ID -> {
      val table = this.graphContext.get(REF_TABLE_READ_KEY) as? RefTableReader?
                  ?: error("ref table is required to decode ref")
      table.get(input.readVarInt(true))
    }

    REF_INLINE -> readClassAndObject(input)

    else -> error("invalid ref marker: $marker")
  }
}

internal fun Kryo.writeRefList(output: Output, values: List<Any>) {
  output.writeVarInt(values.size, true)
  if (values.isEmpty()) {
    return
  }
  val table = this.graphContext.get(REF_TABLE_WRITE_KEY) as RefTableWriter?
  if (table != null) {
    output.writeByte(REF_ID)
    for (value in values) {
      output.writeVarInt(table.idFor(value), true)
    }
  }
  else {
    output.writeByte(REF_INLINE)
    for (value in values) {
      writeClassAndObject(output, value)
    }
  }
}

internal inline fun <T> Kryo.readRefList(input: Input, cast: (Any) -> T): List<T> {
  val size = input.readVarInt(true)
  if (size == 0) {
    return emptyList()
  }
  val result = ArrayList<T>(size)
  when (val marker = input.readByte()) {
    REF_ID -> {
      val table = this.graphContext.get(REF_TABLE_READ_KEY) as? RefTableReader?
                  ?: error("ref table is required to decode ref")
      repeat(size) {
        val obj = table.get(input.readVarInt(true))
        result.add(cast(obj))
      }
    }

    REF_INLINE -> repeat(size) {
      val obj = readClassAndObject(input)
      result.add(cast(obj))
    }

    else -> error("invalid ref marker: $marker")
  }
  return result
}
