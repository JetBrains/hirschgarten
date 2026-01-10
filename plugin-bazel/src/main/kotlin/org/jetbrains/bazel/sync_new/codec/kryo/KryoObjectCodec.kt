package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput
import com.esotericsoftware.kryo.kryo5.io.ByteBufferOutput
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.minlog.Log
import com.esotericsoftware.kryo.kryo5.objenesis.instantiator.ObjectInstantiator
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.serializers.CollectionSerializer
import com.esotericsoftware.kryo.kryo5.serializers.DefaultArraySerializers
import com.esotericsoftware.kryo.kryo5.serializers.DefaultSerializers
import com.esotericsoftware.kryo.kryo5.serializers.MapSerializer
import com.esotericsoftware.kryo.kryo5.unsafe.UnsafeByteBufferInput
import com.esotericsoftware.kryo.kryo5.unsafe.UnsafeByteBufferOutput
import com.esotericsoftware.kryo.kryo5.util.DefaultClassResolver
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.util.Pool
import com.intellij.openapi.components.service
import com.intellij.util.containers.BidirectionalMap
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.codec.HasByteBuffer
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Calendar
import java.util.Collections
import java.util.Currency
import java.util.Date
import java.util.EnumSet
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.Locale
import java.util.TimeZone
import java.util.TreeMap
import java.util.TreeSet
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.emptySet


private val kryoPool = object : Pool<Kryo>(
  /* threadSafe = */ true,
  /* softReferences = */ false,
  /* maximumCapacity = */ 32,
) {
  override fun create(): Kryo {
    Log.WARN()
    val kryo = Kryo(DefaultClassResolver(), null)
    kryo.classLoader = KryoObjectCodec::class.java.classLoader
    kryo.isRegistrationRequired = false
    kryo.warnUnregisteredClasses = true
    kryo.setAutoReset(true)
    kryo.references = true
    kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

    val sealedSerializer = KryoSealedInterfaceSerializerFactory(
      serializer = KryoCompositeSerializeFactory(
        factories = listOf(
          KryoEnumSerializerFactory(),
          KryoTaggedCompositeSerializeFactory(),
        ),
        fallback = FieldSerializerFactory(),
      ),
    )

    kryo.setDefaultSerializer(
      KryoCompositeSerializeFactory(
        factories = listOf(
          sealedSerializer,
          KryoEnumSerializerFactory(),
          KryoTaggedCompositeSerializeFactory(),
        ),
        fallback = FieldSerializerFactory(),
      ),
    )

    kryo.registerPrimitiveSerializers()
    kryo.registerFastUtilSerializers()

    registerDefaultSerializers(kryo)

    for (registered in service<KryoRegistryService>().registeredTypes) {
      kryo.register(registered.type, registered.serialId)
    }

    return kryo
  }

  private fun registerDefaultSerializers(kryo: Kryo) {
    kryo.register(ByteArray::class.java, DefaultArraySerializers.ByteArraySerializer())
    kryo.register(CharArray::class.java, DefaultArraySerializers.CharArraySerializer())
    kryo.register(ShortArray::class.java, DefaultArraySerializers.ShortArraySerializer())
    kryo.register(IntArray::class.java, DefaultArraySerializers.IntArraySerializer())
    kryo.register(LongArray::class.java, DefaultArraySerializers.LongArraySerializer())
    kryo.register(FloatArray::class.java, DefaultArraySerializers.FloatArraySerializer())
    kryo.register(DoubleArray::class.java, DefaultArraySerializers.DoubleArraySerializer())
    kryo.register(BooleanArray::class.java, DefaultArraySerializers.BooleanArraySerializer())
    kryo.register(Array<String>::class.java, DefaultArraySerializers.StringArraySerializer())
    kryo.register(Array<Any>::class.java, DefaultArraySerializers.ObjectArraySerializer(kryo, Array<Any>::class.java))
    kryo.register(BigInteger::class.java, DefaultSerializers.BigIntegerSerializer())
    kryo.register(BigDecimal::class.java, DefaultSerializers.BigDecimalSerializer())
    kryo.register(Class::class.java, DefaultSerializers.ClassSerializer())
    kryo.register(EnumSet::class.java, DefaultSerializers.EnumSetSerializer())
    kryo.register(StringBuffer::class.java, DefaultSerializers.StringBufferSerializer())
    kryo.register(StringBuilder::class.java, DefaultSerializers.StringBuilderSerializer())
    kryo.register(TreeSet::class.java, DefaultSerializers.TreeSetSerializer())
    kryo.register(TreeMap::class.java, DefaultSerializers.TreeMapSerializer())

    kryo.register(Collections.EMPTY_LIST.javaClass, DefaultSerializers.CollectionsEmptyListSerializer())
    kryo.register(Collections.EMPTY_MAP.javaClass, DefaultSerializers.CollectionsEmptyMapSerializer())
    kryo.register(Collections.EMPTY_SET.javaClass, DefaultSerializers.CollectionsEmptySetSerializer())
    kryo.register(listOf(null).javaClass, DefaultSerializers.CollectionsSingletonListSerializer())
    kryo.register(Collections.singletonMap<Any, Any>(null, null).javaClass, DefaultSerializers.CollectionsSingletonMapSerializer())
    kryo.register(setOf(null).javaClass, DefaultSerializers.CollectionsSingletonSetSerializer())

    kryo.register(TreeSet::class.java, DefaultSerializers.TreeSetSerializer())
    kryo.register(TreeMap::class.java, DefaultSerializers.TreeMapSerializer())

    kryo.registerCollectionSerializer { LinkedList<Any>() }
    kryo.registerCollectionSerializer { LinkedHashSet<Any>() }
    kryo.registerCollectionSerializer { HashSet<Any>() }
    kryo.registerCollectionSerializer { ArrayList<Any>() }
    kryo.registerMapSerializer { LinkedHashMap<Any, Any>() }
    kryo.registerMapSerializer { BidirectionalMap<Any, Any>() }
    kryo.registerCollectionSerializer { buildList { add(null) } }
    kryo.registerCollectionSerializer { buildSet { add(null) } }
    kryo.registerMapSerializer { buildMap { put(null, null) } }
    kryo.registerSingletonSerializer { emptyList<Any>() }
    kryo.registerSingletonSerializer { emptyMap<Any, Any>() }
    kryo.registerSingletonSerializer { emptySet<Any>() }
  }

}

private inline fun <reified T : Collection<*>> Kryo.registerCollectionSerializer(
  type: Class<out T> = T::class.java,
  crossinline create: (size: Int) -> T,
) {
  val serializer = object : CollectionSerializer<T>() {
    override fun create(kryo: Kryo?, input: Input?, type: Class<out T?>?, size: Int): T {
      return create(size)
    }
  }
  register(type, serializer)
}

private inline fun <reified T : Map<*, *>> Kryo.registerMapSerializer(
  type: Class<out T> = T::class.java,
  crossinline create: (size: Int) -> T,
) {
  val serializer = object : MapSerializer<T>() {
    override fun create(kryo: Kryo?, input: Input?, type: Class<out T?>?, size: Int): T {
      return create(size)
    }
  }
  register(type, serializer)
}

private inline fun <reified T : Any> Kryo.registerSingletonSerializer(crossinline getter: () -> T) {
  val value = getter()
  register(value.javaClass).instantiator = ObjectInstantiator { value }
}

private inline fun <T> useKryo(crossinline block: (kryo: Kryo) -> T): T {
  val kryo = kryoPool.obtain()
  try {
    return block(kryo)
  }
  finally {
    kryoPool.free(kryo)
  }
}

// TODO: fix direct byte buffer writes
class KryoObjectCodec<T>(
  private val type: Class<T>,
  private val initialBufferSize: Int,
  private val useDirectBuffers: Boolean = false,
) : Codec<T> {

  override fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: T,
  ) {
    if (useDirectBuffers && buffer is HasByteBuffer) {
      encodeWithByteBuffer(ctx, buffer, value)
    }
    else {
      encodeWithByteArray(ctx, buffer, value)
    }
  }

  private fun encodeWithByteBuffer(
    ctx: CodecContext,
    buffer: HasByteBuffer,
    value: T,
  ) {
    val isDirect = buffer.buffer.isDirect
    val output = if (isDirect) {
      UnsafeByteBufferOutput(initialBufferSize, Int.MAX_VALUE)
    }
    else {
      ByteBufferOutput(initialBufferSize, Int.MAX_VALUE)
    }

    output.use { out ->
      useKryo { kryo -> kryo.writeObject(out, value) }
      out.flush()

      val byteBuffer = out.byteBuffer
      val limit = byteBuffer.position()
      byteBuffer.flip()

      check(byteBuffer.limit() == limit) { "ByteBuffer flip failed: expected limit=$limit, got ${byteBuffer.limit()}" }

      val length = byteBuffer.remaining()
      buffer.writeVarInt(length)

      val readOnlySlice = byteBuffer.asReadOnlyBuffer()
      buffer.writeBuffer(readOnlySlice)
    }
  }

  private fun encodeWithByteArray(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: T,
  ) {
    Output(initialBufferSize, Int.MAX_VALUE).use { out ->
      useKryo { kryo -> kryo.writeObject(out, value) }
      out.flush()

      val position = out.position()
      buffer.writeVarInt(position)

      val bytes = out.buffer
      buffer.writeBytes(bytes = bytes, offset = 0, length = position)
    }
  }

  override fun decode(ctx: CodecContext, buffer: CodecBuffer): T {
    val length = buffer.readVarInt()

    if (useDirectBuffers && buffer is HasByteBuffer) {
      return decodeWithByteBuffer(ctx, buffer, length)
    }
    else {
      return decodeWithByteArray(ctx, buffer, length)
    }
  }

  private fun decodeWithByteBuffer(
    ctx: CodecContext,
    buffer: HasByteBuffer,
    length: Int,
  ): T {
    val byteBuffer = buffer.readBuffer(length)
    if (byteBuffer.remaining() != length) {
      error("ByteBuffer has wrong remaining: expected=$length, actual=${byteBuffer.remaining()}")
    }

    val input = if (byteBuffer.isDirect) {
      UnsafeByteBufferInput(byteBuffer)
    }
    else {
      ByteBufferInput(byteBuffer)
    }

    return input.use { inp ->
      val result = useKryo { kryo -> kryo.readObject(inp, type) }
      if (inp.available() > 0) {
        error("Not all data was read: ${inp.available()} bytes remaining")
      }
      result
    }
  }

  private fun decodeWithByteArray(
    ctx: CodecContext,
    buffer: CodecBuffer,
    length: Int,
  ): T {
    val bytes = ByteArray(length)
    buffer.readBytes(bytes)

    return Input(bytes).use { inp ->
      useKryo { kryo ->
        kryo.readObject(inp, type)
      }
    }
  }
}

inline fun <reified T> CodecBuilder.ofKryo(initialBufferSize: Int = 256): Codec<T> = KryoObjectCodec(T::class.java, initialBufferSize)

