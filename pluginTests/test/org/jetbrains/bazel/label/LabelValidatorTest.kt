package org.jetbrains.bazel.label

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LabelValidatorTest {

  @ParameterizedTest
  @ValueSource(
    strings = [
      "my_lib",
      "Foo",
      "lib123",
      "my-lib",
      "lib.jar",
      "some/target",
      "lib+extra",
      "lib=foo",
      "name@scope",
      "lib!",
      "name~tilde",
      "a#b",
      "a\$b",
      "a&b",
      "a'b",
      "a(b)",
      "a*b",
      "a,b",
      "a;b",
      "a<b>",
      "a?b",
      "a[b]",
      "a{b}",
      "a|b",
      "a^b",
      "a%b",
      """a"b""",
    ],
  )
  fun `should accept valid target names`(name: String) {
    LabelValidator.isTargetNameValid(name).shouldBeTrue()
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "my lib",
      "name:colon",
      "name`tick",
      "name\nfoo",
      "tab\there",
      ""
    ],
  )
  fun `should reject invalid target names`(name: String) {
    LabelValidator.isTargetNameValid(name).shouldBeFalse()
  }
}
