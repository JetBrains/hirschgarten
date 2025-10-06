package org.jetbrains.bazel.workspace.model.matchers

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should

@Deprecated(
  message = "Use shouldBeEqual(expected) instead after migrating to Kotest 5.6.0 or higher.",
  ReplaceWith(
    expression = "shouldBeEqual(expected)",
    imports = ["io.kotest.matchers.equals"],
  ),
)
infix fun <A : Any> A.shouldBeEqual(expected: A): A =
  apply {
    should(
      matcher =
        object : Matcher<A> {
          override fun test(value: A) =
            MatcherResult(
              passed = value == expected,
              failureMessageFn = { "$value should be equal to $expected" },
              negatedFailureMessageFn = { "$value should not be equal to $expected" },
            )
        },
    )
  }
