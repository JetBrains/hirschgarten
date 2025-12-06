package org.jetbrains.bazel.sync_new.storage.in_memory

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.new_sync.storage.NoopCodec
import org.jetbrains.bazel.new_sync.storage.NoopPersistentStoreOwner
import org.junit.jupiter.api.Test

class InMemoryFlatStoreTest {
  @Test
  fun `test correct single-writer modify semantics`() {
    data class Counter(val counter: Int)

    val store = InMemoryFlatStore(
      owner = NoopPersistentStoreOwner(),
      name = "test",
      codec = NoopCodec(),
      creator = { Counter(0) }
    )

    fun increment() = store.modify { it.copy(counter = it.counter + 1) }.counter

    increment().shouldBe(1)
    increment().shouldBe(2)
    increment().shouldBe(3)
  }
}
