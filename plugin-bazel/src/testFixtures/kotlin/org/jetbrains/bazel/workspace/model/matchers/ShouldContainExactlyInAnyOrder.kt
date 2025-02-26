package org.jetbrains.bazel.workspace.model.matchers

import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldHaveSize

fun <A, E> Collection<A>.shouldContainExactlyInAnyOrder(assertion: (actual: A, expected: E) -> Unit, expectedValues: Collection<E>) {
  this shouldHaveSize expectedValues.size
  this.forAll { actual -> expectedValues.forAny { assertion(actual, it) } }
}
