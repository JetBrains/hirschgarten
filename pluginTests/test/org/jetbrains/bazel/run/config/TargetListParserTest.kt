package org.jetbrains.bazel.run.config

import org.jetbrains.bazel.label.Label
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class TargetListParserTest {
  @ParameterizedTest
  @ValueSource(strings = ["", " ", "   "])
  fun `empty input has no targets`(text: String) {
    assertEquals(emptyList(), TargetListParser.parseString(text))
  }

  @Test
  fun `create an empty string when there are no targets`() {
    assertEquals("", TargetListParser.createString(emptyList()))
  }

  @ParameterizedTest
  @ValueSource(strings = [":target_name", "//pkg:target", "@//pkg:target", "@@//pkg:target", "@repo//pkg:target", "@@repo+version//pkg:target"])
  fun `parse a single target`(text: String) {
    assertEquals(listOf(Label.parse(text)), TargetListParser.parseString(text))
  }

  @ParameterizedTest
  @ValueSource(strings = [":target_name", "//pkg:target", "@//pkg:target", "@@//pkg:target", "@repo//pkg:target", "@@repo+version//pkg:target"])
  fun `create a string for a single target`(text: String) {
    val target = Label.parse(text)

    assertEquals(target.toString(), TargetListParser.createString(listOf(target)))
  }

  @ParameterizedTest
  @ValueSource(ints = [1, 2, 4])
  fun `split targets at spaces before label starts`(spaceCount: Int) {
    val targets = listOf(
      "//pkg:zero",
      ":first",
      "@//pkg:second",
      "@@//pkg:third",
      "@repo//pkg:fourth",
      "@@repo+version//pkg:fifth",
      "//pkg:sixth",
    )
    val text = "  " + targets.joinToString(" ".repeat(spaceCount)) + "   "

    assertEquals(targets.map { Label.parse(it) }, TargetListParser.parseString(text))
  }

  @Test
  fun `separate targets with spaces when creating a string`() {
    val targets = listOf("//pkg:first", "@//pkg:second", "@@repo+version//pkg:third").map { Label.parse(it) }

    assertEquals("@//pkg:first @//pkg:second @@repo+version//pkg:third", TargetListParser.createString(targets))
  }

  @Test
  fun `preserve spaces inside labels`() {
    val targets = listOf("//some  package:first", "@repo//another package:second", "@@repo+version//last   package:third")

    assertEquals(targets.map { Label.parse(it) }, TargetListParser.parseString(targets.joinToString("   ")))
  }

  @Test
  fun `preserve spaces inside labels when creating a string`() {
    val targets = listOf("//some  package:first", "@repo//another package:second", "@@repo+version//last   package:third").map { Label.parse(it) }

    assertEquals("@//some  package:first @repo//another package:second @@repo+version//last   package:third", TargetListParser.createString(targets))
  }

  @Test
  fun `preserve the target order and duplicates`() {
    val targets = listOf("//pkg:second", "//pkg:first", "//pkg:second").map { Label.parse(it) }

    assertEquals(targets, TargetListParser.parseString(TargetListParser.createString(targets)))
  }
}
