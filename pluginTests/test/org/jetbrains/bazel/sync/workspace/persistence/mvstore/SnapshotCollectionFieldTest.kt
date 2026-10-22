package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Attribute
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import org.jetbrains.bazel.sync.workspace.persistence.type
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.TreeMap

private data class CollectionsHolder(
  val list: List<String>,
  val set: Set<String>,
  val map: Map<String, String>,
)

private data class SharedListHolder(val first: List<String>, val second: List<String>)

private data class RegisteredCollectionHolder(val ids: IntOpenHashSet)

private data class NullableCollectionHolder(val list: List<String>?)

private data class UnregisteredCollectionHolder(val ids: IntSet)

private data class OtherCollectionHolder(val values: Collection<String>)

class SnapshotCollectionFieldTest {
  private val snapshotKryo = SnapshotKryo(
    SnapshotTypeUniverse.build(
      buildList {
        type<CollectionsHolder>()
        type<SharedListHolder>()
        type<NullableCollectionHolder>()
        type<RegisteredCollectionHolder>()
        type<UnregisteredCollectionHolder>()
        type<OtherCollectionHolder>()
      },
    ),
  )

  private val protobufList: List<String> =
    Attribute.newBuilder()
      .setName("tags")
      .setType(Attribute.Discriminator.STRING_LIST)
      .addAllStringListValue(listOf("first", "second"))
      .build()
      .stringListValueList

  @Test
  fun `unregistered values of list, set and map fields read back as the plain variant`() {
    protobufList.javaClass shouldNotBe ArrayList::class.java
    val holder = CollectionsHolder(
      list = protobufList,
      set = Collections.unmodifiableSet(linkedSetOf("b", "a")),
      map = Collections.unmodifiableMap(linkedMapOf("k" to "v")),
    )

    val restored = serializeAndDeserialize(holder) as CollectionsHolder

    restored shouldBe holder
    restored.list.javaClass shouldBe ArrayList::class.java
    restored.set.javaClass shouldBe HashSet::class.java
    restored.map.javaClass shouldBe HashMap::class.java
  }

  @Test
  fun `registered values of list, set and map fields keep their type`() {
    val holder = CollectionsHolder(list = listOf("a"), set = linkedSetOf("b", "a"), map = TreeMap(mapOf("k" to "v")))

    val restored = serializeAndDeserialize(holder) as CollectionsHolder

    restored shouldBe holder
    restored.list.javaClass shouldBe listOf("a").javaClass
    restored.set.javaClass shouldBe LinkedHashSet::class.java
    restored.set shouldContainExactly listOf("b", "a")
    restored.map.javaClass shouldBe TreeMap::class.java
  }

  @Test
  fun `empty unregistered values of list, set and map fields read back as the shared empty instances`() {
    val holder = CollectionsHolder(list = Collections.emptyList(), set = Collections.emptySet(), map = Collections.emptyMap())

    val restored = serializeAndDeserialize(holder) as CollectionsHolder

    restored.list shouldBeSameInstanceAs emptyList<String>()
    restored.set shouldBeSameInstanceAs emptySet<String>()
    restored.map shouldBeSameInstanceAs emptyMap<String, String>()
  }

  @Test
  fun `list field with references reads one shared value back as one instance`() {
    val restored = serializeAndDeserialize(SharedListHolder(protobufList, protobufList)) as SharedListHolder

    restored.first shouldBe protobufList
    restored.second shouldBeSameInstanceAs restored.first
  }

  @Test
  fun `field of a registered collection type reads back as that type`() {
    val restored = serializeAndDeserialize(RegisteredCollectionHolder(IntOpenHashSet(intArrayOf(1, 2)))) as RegisteredCollectionHolder

    restored.ids shouldBe IntOpenHashSet(intArrayOf(1, 2))
  }

  @Test
  fun `registered value of another collection interface field reads back`() {
    val restored = serializeAndDeserialize(UnregisteredCollectionHolder(IntOpenHashSet(intArrayOf(1, 2)))) as UnregisteredCollectionHolder

    restored.ids shouldBe IntOpenHashSet(intArrayOf(1, 2))
  }

  @Test
  fun `list field without references encodes null`() {
    snapshotKryo.valuePool.use { kryo ->
      val nullBytes = kryo.toSerializedBytes(NullableCollectionHolder(null))
      val emptyBytes = kryo.toSerializedBytes(NullableCollectionHolder(emptyList()))

      kryo.readClassAndObject(Input(nullBytes)) shouldBe NullableCollectionHolder(null)
      kryo.readClassAndObject(Input(emptyBytes)) shouldBe NullableCollectionHolder(emptyList())
      emptyBytes.size shouldBe nullBytes.size
    }
  }

  @Test
  fun `unregistered collection outside a list, set or map field is rejected on write`() {
    snapshotKryo.pool.use { kryo ->
      shouldThrowAny { kryo.toSerializedBytes(protobufList) }.message shouldContain "Class is not registered"
      shouldThrowAny { kryo.toSerializedBytes(OtherCollectionHolder(protobufList)) }.message shouldContain "Class is not registered"
      shouldThrowAny { kryo.toSerializedBytes(UnregisteredCollectionHolder(IntSets.EMPTY_SET)) }.message shouldContain
        "Class is not registered"
    }
  }

  private fun serializeAndDeserialize(value: Any): Any =
    snapshotKryo.pool.use { kryo -> kryo.readClassAndObject(Input(kryo.toSerializedBytes(value))) }

  private fun Kryo.toSerializedBytes(value: Any): ByteArray {
    val output = Output(4096, -1)
    writeClassAndObject(output, value)
    return output.toBytes()
  }
}
