package org.jetbrains.bazel.sync_new.storage

import com.google.common.primitives.Primitives
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.codecBuilderOf
import org.jetbrains.bazel.sync_new.codec.ofInt
import org.jetbrains.bazel.sync_new.codec.ofString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal abstract class StorageContextKVStoreTest : StorageContextContractTest() {
  companion object {
    internal val defaultTestCases = listOf(
      object : KVStoreTestCase<Int, String>(
        name = "Int->String",
        keyType = Int::class.java,
        valueType = String::class.java,
        keyCodec = { ofInt() },
        valueCodec = { ofString() },
      ) {
        override fun generateKey(idx: Int) = idx
        override fun generateValue(idx: Int) = "value-$idx"
      },
      object : KVStoreTestCase<String, String>(
        name = "String->String",
        keyType = String::class.java,
        valueType = String::class.java,
        keyCodec = { ofString() },
        valueCodec = { ofString() },
      ) {
        override fun generateKey(idx: Int) = "key-$idx"
        override fun generateValue(idx: Int) = "value-$idx"
      },
    )

    internal val defaultHintSets: Array<Array<StorageHints>> = arrayOf(
      arrayOf(DefaultStorageHints.USE_IN_MEMORY),
      arrayOf(DefaultStorageHints.USE_PAGED_STORE),
    )

    internal fun combine(cases: List<KVStoreTestCase<*, *>>, hints: Array<Array<StorageHints>>): List<Arguments> =
      cases.flatMap { testCase -> hints.map { Arguments.of(testCase, it) } }

    internal const val TEST_NAME = "[{index}] {0} with hints={1}"
  }

  inner class KVStoreTestContext<K : Any, V : Any>(
    val name: String,
    val testCase: KVStoreTestCase<K, V>,
    val hints: Array<StorageHints>,
  ) {
    fun open(block: KVStore<K, V>.() -> Unit) {
      openContext(
        builder = {
          createKVStore(
            name = name,
            hints = hints,
            keyType = Primitives.wrap(testCase.keyType),
            valueType = Primitives.wrap(testCase.valueType),
          )
            .withKeyCodec { testCase.keyCodec(codecBuilderOf()) }
            .withValueCodec { testCase.valueCodec(codecBuilderOf()) }
            .build()
        },
        block = {
          block()
        },
      )
    }
  }

  protected fun <K : Any, V : Any> useContext(
    name: String,
    testCase: KVStoreTestCase<K, V>,
    hints: Array<StorageHints>,
    block: KVStoreTestContext<K, V>.() -> Unit,
  ) {
    val ctx = KVStoreTestContext(name, testCase, hints)
    ctx.open {
      clear()
    }
    block(ctx)
  }

  protected fun <K : Any, V : Any> generate(testCase: KVStoreTestCase<K, V>, n: Int): List<Pair<K, V>> {
    return (0..<n).map { testCase.generateKey(it) to testCase.generateValue(it) }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test put get persistent simple`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test put get persistent simple", testCase, hints) {
      val data = generate(testCase, 3)
      open {
        put(data[0].first, data[0].second)
        put(data[1].first, data[1].second)
        put(data[2].first, data[2].second)

        get(data[0].first).shouldBe(data[0].second)
        get(data[1].first).shouldBe(data[1].second)
        get(data[2].first).shouldBe(data[2].second)
      }

      open {
        get(data[0].first).shouldBe(data[0].second)
        get(data[1].first).shouldBe(data[1].second)
        get(data[2].first).shouldBe(data[2].second)
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test update existing key`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test update existing key", testCase, hints) {
      val data = generate(testCase, 5)
      open {
        put(data[0].first, data[0].second)
        put(data[1].first, data[1].second)
        
        put(data[0].first, data[2].second)
        
        get(data[0].first).shouldBe(data[2].second)
        get(data[1].first).shouldBe(data[1].second)
      }

      open {
        get(data[0].first).shouldBe(data[2].second)
        get(data[1].first).shouldBe(data[1].second)
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test remove key`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test remove key", testCase, hints) {
      val data = generate(testCase, 3)
      open {
        put(data[0].first, data[0].second)
        put(data[1].first, data[1].second)
        put(data[2].first, data[2].second)

        remove(data[1].first)

        get(data[0].first).shouldBe(data[0].second)
        get(data[1].first).shouldBe(null)
        get(data[2].first).shouldBe(data[2].second)
      }

      open {
        get(data[0].first).shouldBe(data[0].second)
        get(data[1].first).shouldBe(null)
        get(data[2].first).shouldBe(data[2].second)
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test get non-existent key returns null`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test get non-existent key", testCase, hints) {
      val data = generate(testCase, 2)
      open {
        put(data[0].first, data[0].second)
        
        get(data[1].first).shouldBe(null)
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test remove non-existent key`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test remove non-existent key", testCase, hints) {
      val data = generate(testCase, 2)
      open {
        put(data[0].first, data[0].second)
        
        remove(data[1].first)
        
        get(data[0].first).shouldBe(data[0].second)
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test clear all keys`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test clear all keys", testCase, hints) {
      val data = generate(testCase, 3)
      open {
        put(data[0].first, data[0].second)
        put(data[1].first, data[1].second)
        put(data[2].first, data[2].second)

        clear()

        get(data[0].first).shouldBe(null)
        get(data[1].first).shouldBe(null)
        get(data[2].first).shouldBe(null)
      }

      open {
        get(data[0].first).shouldBe(null)
        get(data[1].first).shouldBe(null)
        get(data[2].first).shouldBe(null)
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test iterate over entries`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test iterate over entries", testCase, hints) {
      val data = generate(testCase, 3)
      open {
        put(data[0].first, data[0].second)
        put(data[1].first, data[1].second)
        put(data[2].first, data[2].second)

        val entries = mutableListOf<Pair<K, V>>()
        iterator().use { iter ->
          while (iter.hasNext()) {
            entries.add(iter.next())
          }
        }
        
        entries.toSet().shouldBe(data.toSet())
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test iterate over empty store`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test iterate over empty store", testCase, hints) {
      open {
        val entries = mutableListOf<Pair<K, V>>()
        iterator().use { iter ->
          while (iter.hasNext()) {
            entries.add(iter.next())
          }
        }
        
        entries.shouldBe(emptyList())
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test iterate keys`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test iterate keys", testCase, hints) {
      val data = generate(testCase, 3)
      open {
        put(data[0].first, data[0].second)
        put(data[1].first, data[1].second)
        put(data[2].first, data[2].second)

        val keys = mutableListOf<K>()
        keys().use { iter ->
          while (iter.hasNext()) {
            keys.add(iter.next())
          }
        }

        keys.toSet().shouldBe(data.map { it.first }.toSet())
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test iterate values`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test iterate values", testCase, hints) {
      val data = generate(testCase, 3)
      open {
        put(data[0].first, data[0].second)
        put(data[1].first, data[1].second)
        put(data[2].first, data[2].second)

        val values = mutableListOf<V>()
        values().use { iter ->
          while (iter.hasNext()) {
            values.add(iter.next())
          }
        }

        values.toSet().shouldBe(data.map { it.second }.toSet())
      }
    }
  }

  @ParameterizedTest(name = TEST_NAME)
  @MethodSource("testCases")
  fun <K : Any, V : Any> `test multiple put-remove cycles`(testCase: KVStoreTestCase<K, V>, hints: Array<StorageHints>) {
    useContext("test multiple put-remove cycles", testCase, hints) {
      val data = generate(testCase, 10)
      open {
        for (i in 0..2) {
          put(data[i].first, data[i].second)
        }
        
        remove(data[1].first)
        
        for (i in 3..5) {
          put(data[i].first, data[i].second)
        }
        
        remove(data[3].first)
        remove(data[4].first)

        get(data[0].first).shouldBe(data[0].second)
        get(data[1].first).shouldBe(null)
        get(data[2].first).shouldBe(data[2].second)
        get(data[3].first).shouldBe(null)
        get(data[4].first).shouldBe(null)
        get(data[5].first).shouldBe(data[5].second)
      }

      open {
        get(data[0].first).shouldBe(data[0].second)
        get(data[1].first).shouldBe(null)
        get(data[2].first).shouldBe(data[2].second)
        get(data[3].first).shouldBe(null)
        get(data[4].first).shouldBe(null)
        get(data[5].first).shouldBe(data[5].second)
      }
    }
  }

}

internal abstract class KVStoreTestCase<K : Any, V : Any>(
  val name: String,
  val keyType: Class<K>,
  val valueType: Class<V>,
  val keyCodec: CodecBuilder.() -> Codec<K>,
  val valueCodec: CodecBuilder.() -> Codec<V>,
) {
  abstract fun generateKey(idx: Int): K
  abstract fun generateValue(idx: Int): V
  
  override fun toString(): String = name
}

internal fun Array<StorageHints>.toDisplayString(): String {
  return joinToString(", ") { it.javaClass.simpleName }
}
