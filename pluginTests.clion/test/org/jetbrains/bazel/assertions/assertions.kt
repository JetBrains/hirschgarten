package org.jetbrains.bazel.assertions

import org.assertj.core.api.AbstractIterableAssert
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

fun <S : AbstractIterableAssert<S, A, E, *>, A : Iterable<E>, E> S.haveExactlyOne(condition: Condition<in E>): S {
  haveExactly(1, condition)
  return this
}
