package org.jetbrains.bazel.assertions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import java.util.function.Predicate

internal fun <T : Any> T?.assertNotNull(): T {
  assertThat(this).isNotNull()
  return requireNotNull(this)
}

internal fun <T : Any> condition(description: String? = null, predicate: Predicate<T>): Condition<T> {
  return Condition(predicate, description)
}
