package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.sync_new.codec.ofInt
import org.jetbrains.bazel.sync_new.codec.ofString
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.asClosingSequence
import org.jetbrains.bazel.sync_new.storage.createKVStore

internal class RocksdbKVStoreTest : LightPlatformTestCase() {

  fun `test iterator does not return removed values`() {
    fun openStore(block: KVStore<Int, String>.() -> Unit) = openContext(
      builder = {
        createKVStore<Int, String>("test_iterator_removed_values", StorageHints.USE_PAGED_STORE)
          .withKeyCodec { ofInt() }
          .withValueCodec { ofString() }
          .build()
      },
      block = block,
    )

    openStore {
      put(1, "one")
      put(2, "two")
      put(3, "three")
      put(4, "four")
      put(5, "five")

      remove(2)
      remove(4)

      val result = iterator().asClosingSequence().toList()
      result.shouldContainExactlyInAnyOrder(listOf(Pair(1, "one"), Pair(3, "three"), Pair(5, "five")))

      val keysResult = keys().asClosingSequence().toList()
      keysResult.shouldContainExactlyInAnyOrder(listOf(1, 3, 5))

      val valuesResult = values().asClosingSequence().toList()
      valuesResult.shouldContainExactlyInAnyOrder(listOf("one", "three", "five"))
    }
  }

  fun `test iterator consistency simple`() {
    fun openStore(block: KVStore<Int, String>.() -> Unit) = openContext(
      builder = {
        createKVStore<Int, String>("test_iterator_consistency_simple", StorageHints.USE_PAGED_STORE)
          .withKeyCodec { ofInt() }
          .withValueCodec { ofString() }
          .build()
      },
      block = block,
    )

    openStore {
      put(1, "one")
      put(2, "two")
      put(3, "three")

      iterator().asClosingSequence().toList()
        .shouldContainExactlyInAnyOrder(listOf(Pair(1, "one"), Pair(2, "two"), Pair(3, "three")))

      remove(1)

      iterator().asClosingSequence().toList()
        .shouldContainExactlyInAnyOrder(listOf(Pair(2, "two"), Pair(3, "three")))
    }

    openStore {
      iterator().asClosingSequence().toList()
        .shouldContainExactlyInAnyOrder(listOf(Pair(2, "two"), Pair(3, "three")))
    }
  }

  fun `test iterator does not show removed values before flush`() {
    fun openStore(block: KVStore<Int, String>.() -> Unit) = openContext(
      builder = {
        createKVStore<Int, String>("test_removed_before_flush", StorageHints.USE_PAGED_STORE)
          .withKeyCodec { ofInt() }
          .withValueCodec { ofString() }
          .build()
      },
      block = block,
    )

    openStore {
      put(1, "one")
      put(2, "two")
      put(3, "three")
      put(4, "four")
      put(5, "five")
    }

    openStore {
      remove(2)
      remove(4)

      val result = iterator().asClosingSequence().toList()
      result.shouldContainExactlyInAnyOrder(listOf(Pair(1, "one"), Pair(3, "three"), Pair(5, "five")))

      val keysResult = keys().asClosingSequence().toList()
      keysResult.shouldContainExactlyInAnyOrder(listOf(1, 3, 5))

      val valuesResult = values().asClosingSequence().toList()
      valuesResult.shouldContainExactlyInAnyOrder(listOf("one", "three", "five"))
    }
  }

  fun `test remove and reinsert before flush`() {
    fun openStore(block: KVStore<Int, String>.() -> Unit) = openContext(
      builder = {
        createKVStore<Int, String>("test_remove_reinsert_before_flush", StorageHints.USE_PAGED_STORE)
          .withKeyCodec { ofInt() }
          .withValueCodec { ofString() }
          .build()
      },
      block = block,
    )

    openStore {
      put(1, "one")
      put(2, "two")

      remove(1, useReturn = false)
      put(1, "one-new")

      iterator().asClosingSequence().toList()
        .shouldContainExactlyInAnyOrder(
          listOf(
            1 to "one-new",
            2 to "two",
          ),
        )
    }

    openStore {
      iterator().asClosingSequence().toList()
        .shouldContainExactlyInAnyOrder(
          listOf(
            1 to "one-new",
            2 to "two",
          ),
        )
    }
  }

  fun `test iterator sees dirty deletes before flush`() {
    fun openStore(block: KVStore<Int, String>.() -> Unit) = openContext(
      builder = {
        createKVStore<Int, String>("test_iterator_dirty_delete", StorageHints.USE_PAGED_STORE)
          .withKeyCodec { ofInt() }
          .withValueCodec { ofString() }
          .build()
      },
      block = block,
    )

    openStore {
      put(1, "one")
      put(2, "two")
      put(3, "three")

      remove(2, useReturn = false)
      remove(3, useReturn = false)

      iterator().asClosingSequence().toList()
        .shouldContainExactlyInAnyOrder(
          listOf(
            1 to "one",
          ),
        )
    }
  }

  fun `test heavy churn on single key`() {
    fun openStore(block: KVStore<Int, String>.() -> Unit) = openContext(
      builder = {
        createKVStore<Int, String>("test_heavy_churn_single_key", StorageHints.USE_PAGED_STORE)
          .withKeyCodec { ofInt() }
          .withValueCodec { ofString() }
          .build()
      },
      block = block,
    )

    openStore {
      repeat(1000) { i ->
        put(1, "v$i")
        if (i % 2 == 0) {
          remove(1, useReturn = false)
        }
      }

      val current = get(1)
      assertEquals("v999", current)

      iterator().asClosingSequence().toList()
        .shouldContainExactlyInAnyOrder(
          listOf(
            1 to "v999",
          ),
        )
    }

    openStore {
      iterator().asClosingSequence().toList()
        .shouldContainExactlyInAnyOrder(
          listOf(
            1 to "v999",
          ),
        )
    }
  }

  fun `test many keys remove and restart`() {
    fun openStore(block: KVStore<Int, String>.() -> Unit) = openContext(
      builder = {
        createKVStore<Int, String>("test_many_keys_remove_restart", StorageHints.USE_PAGED_STORE)
          .withKeyCodec { ofInt() }
          .withValueCodec { ofString() }
          .build()
      },
      block = block,
    )

    openStore {
      for (i in 0 until 1000) {
        put(i, "v$i")
      }
    }

    openStore {
      for (i in 0 until 1000 step 2) {
        remove(i, useReturn = false)
      }
      for (i in 0 until 1000 step 4) {
        put(i, "v${i}-new")
      }

      val entries = iterator().asClosingSequence().toList()
      val expected = mutableMapOf<Int, String>()
      for (i in 0 until 1000) {
        val s = when {
          i % 4 == 0 -> "v${i}-new"
          i % 2 == 0 -> null
          else       -> "v$i"
        }
        if (s != null) {
          expected[i] = s
        }
      }

      entries.shouldContainExactlyInAnyOrder(expected.entries.map { it.key to it.value })
    }

    openStore {
      val entries = iterator().asClosingSequence().toList()
      val expected = mutableMapOf<Int, String>()
      for (i in 0 until 1000) {
        val s = when {
          i % 4 == 0 -> "v${i}-new"
          i % 2 == 0 -> null
          else       -> "v$i"
        }
        if (s != null) {
          expected[i] = s
        }
      }

      entries.shouldContainExactlyInAnyOrder(expected.entries.map { it.key to it.value })
    }
  }

  private fun <S : KVStore<*, *>> openContext(builder: RocksdbStorageContext.() -> S, block: S.() -> Unit) {
    val disposable = Disposer.newDisposable()
    val ctx = RocksdbStorageContext(project = project, disposable = disposable)
    val store = builder(ctx)
    block(store)
    Disposer.dispose(disposable)
  }

}
