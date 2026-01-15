package org.jetbrains.bazel.sync_new.storage

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.codecBuilderOf
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource

internal abstract class StorageContextFlatStoreTest : StorageContextContractTest() {
  companion object {
    data class ImmutableData(
      val text: String?,
    )

    data class MutableData(
      val list: MutableList<String>,
    )

    @JvmStatic
    internal val immutableTestCase = object : FlatStoreTestCase<ImmutableData>(
      name = "immutable",
      type = ImmutableData::class.java,
      codec = { ofKryo() },
    ) {
      override fun create(): ImmutableData {
        return ImmutableData(text = null)
      }
    }

    @JvmStatic
    internal val mutableTestCase = object : FlatStoreTestCase<MutableData>(
      name = "mutable",
      type = MutableData::class.java,
      codec = { ofKryo() },
    ) {
      override fun create(): MutableData {
        return MutableData(list = mutableListOf("1"))
      }
    }

    internal val defaultTestCases = listOf(immutableTestCase, mutableTestCase)
  }

  inner class FlatStoreTestContext<V : Any>(
    val name: String,
    val testCase: FlatStoreTestCase<V>,
  ) {
    fun open(block: FlatStorage<V>.() -> Unit) {
      openContext(
        builder = {
          createFlatStore(
            name = name + "_" + testCase.name,
            type = testCase.type,
          ).withCodec { testCase.codec(codecBuilderOf()) }
            .withCreator { testCase.create() }
            .build()
        },
        block = {
          block()
        },
      )
    }

  }

  fun <V : Any> useContext(name: String, testCase: FlatStoreTestCase<V>, reset: Boolean = true, block: FlatStoreTestContext<V>.() -> Unit) {
    val context = FlatStoreTestContext(
      name = name,
      testCase = testCase,
    )
    if (reset) {
      context.open {
        reset()
      }
    }
    block(context)
  }

  @ParameterizedTest
  @FieldSource("defaultTestCases")
  fun <V : Any> `test initial value without reset`(testCase: FlatStoreTestCase<V>) {
    useContext("test initial value without reset", testCase, reset = false) {
      open {
        get().shouldBe(testCase.create())
      }
    }
  }

  @ParameterizedTest
  @FieldSource("defaultTestCases")
  fun <V : Any> `test reset clears data`(testCase: FlatStoreTestCase<V>) {
    useContext("test reset clears data", testCase, reset = false) {
      open {
        reset()
        get().shouldBe(testCase.create())
      }
    }
  }

  @ParameterizedTest
  @FieldSource("defaultTestCases")
  fun <V : Any> `test mutate updates value`(testCase: FlatStoreTestCase<V>) {
    useContext("test mutate updates value", testCase) {
      open {
        val initial = get()
        mutate { value ->
          when (value) {
            is ImmutableData -> {}
            is MutableData -> {
              value.list.add("modified")
            }
          }
        }
        val updated = get()
        when (updated) {
          is ImmutableData -> {
            (updated === initial).shouldBe(true)
            updated.text.shouldBe(null)
          }
          is MutableData -> {
            (updated === initial).shouldBe(true)
            updated.list.size.shouldBe(2)
            updated.list[1].shouldBe("modified")
          }
        }
      }
    }
  }

  @ParameterizedTest
  @FieldSource("defaultTestCases")
  fun <V : Any> `test multiple mutations`(testCase: FlatStoreTestCase<V>) {
    useContext("test multiple mutations", testCase) {
      open {
        when (testCase) {
          is FlatStoreTestCase<*> -> {
            if (testCase.type == ImmutableData::class.java) {
              // For immutable data, use modify() which does copy-on-write
              modify { ImmutableData(text = "first") as V }
              modify { ImmutableData(text = "second") as V }
              val result = get() as ImmutableData
              result.text.shouldBe("second")
            } else {
              // For mutable data, use mutate() which mutates in place
              mutate { value ->
                (value as MutableData).list.add("first")
              }
              mutate { value ->
                (value as MutableData).list.add("second")
              }
              val result = get() as MutableData
              result.list.size.shouldBe(3)
              result.list[1].shouldBe("first")
              result.list[2].shouldBe("second")
            }
          }
        }
      }
    }
  }

  @ParameterizedTest
  @FieldSource("defaultTestCases")
  fun <V : Any> `test data persists after restart`(testCase: FlatStoreTestCase<V>) {
    useContext("test data persists after restart", testCase) {
      open {
        when (testCase.type) {
          ImmutableData::class.java -> modify { ImmutableData(text = "persisted") as V }
          else -> mutate { value -> (value as MutableData).list.add("persisted") }
        }
      }
      open {
        val result = get()
        when (result) {
          is ImmutableData -> result.text.shouldBe("persisted")
          is MutableData -> {
            result.list.size.shouldBe(2)
            result.list[1].shouldBe("persisted")
          }
        }
      }
    }
  }

  @ParameterizedTest
  @FieldSource("defaultTestCases")
  fun <V : Any> `test data accumulates across multiple restarts`(testCase: FlatStoreTestCase<V>) {
    useContext("test data accumulates across multiple restarts", testCase) {
      open {
        when (testCase.type) {
          ImmutableData::class.java -> modify { ImmutableData(text = "first") as V }
          else -> mutate { value -> (value as MutableData).list.add("first") }
        }
      }
      open {
        when (testCase.type) {
          ImmutableData::class.java -> modify { ImmutableData(text = "second") as V }
          else -> mutate { value -> (value as MutableData).list.add("second") }
        }
      }
      open {
        when (testCase.type) {
          ImmutableData::class.java -> modify { ImmutableData(text = "third") as V }
          else -> mutate { value -> (value as MutableData).list.add("third") }
        }
      }
      open {
        val result = get()
        when (result) {
          is ImmutableData -> result.text.shouldBe("third")
          is MutableData -> {
            result.list.size.shouldBe(4)
            result.list[1].shouldBe("first")
            result.list[2].shouldBe("second")
            result.list[3].shouldBe("third")
          }
        }
      }
    }
  }


}

internal abstract class FlatStoreTestCase<V>(
  val name: String,
  val type: Class<V>,
  val codec: CodecBuilder.() -> Codec<V>,
) {
  abstract fun create(): V

  override fun toString(): String = name
}
