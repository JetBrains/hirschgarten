package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkInvalidDictKeyInspectionTest : BasePlatformTestCase() {
  private val descriptionNotHashable = StarlarkBundle.message("inspection.description.dict.key.not.hashable")
  private val descriptionDuplicate = StarlarkBundle.message("inspection.description.dict.key.duplicate")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkInvalidDictKeyInspection())
  }

  @Test
  fun `list literal key should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        <error descr="$descriptionNotHashable">[1, 2]</error>: "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `dict literal key should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        <error descr="$descriptionNotHashable">{"a": 1}</error>: "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `tuple containing list literal key should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        <error descr="$descriptionNotHashable">([1],)</error>: "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `nested tuple containing list literal key should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        <error descr="$descriptionNotHashable">(([],),)</error>: "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `static hashable literal keys should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "a": 1,
        1: "number",
        1.5: "float",
        True: "yes",
        None: "none",
        ("a", 1, True): "tuple",
        ((1, 2),): "nested tuple",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate string literal keys should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "a": 1,
        "b": 2,
        <error descr="$descriptionDuplicate">"a"</error>: 3,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate string literal keys with different quotes should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "a": 1,
        <error descr="$descriptionDuplicate">'a'</error>: 2,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate numeric literal keys should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        1: "integer",
        <error descr="$descriptionDuplicate">1.0</error>: "float",
        
        2.5: "first float",
        <error descr="$descriptionDuplicate">2.50</error>: "second float",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate numeric literal keys with different representations should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        16: "decimal",
        <error descr="$descriptionDuplicate">0x10</error>: "hex",
        <error descr="$descriptionDuplicate">0o20</error>: "octal",
        <error descr="$descriptionDuplicate">16.00</error>: "float",
        
        1000: "decimal exponent",
        <error descr="$descriptionDuplicate">1e3</error>: "exponent",
        <error descr="$descriptionDuplicate">1.0e3</error>: "float exponent",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate signed numeric literal keys should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        1: "plain",
        <error descr="$descriptionDuplicate">+1</error>: "positive",
        
        -1: "negative integer",
        <error descr="$descriptionDuplicate">-1.0</error>: "negative float",
        
        0: "zero",
        <error descr="$descriptionDuplicate">-0.0</error>: "negative zero",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate boolean literal keys should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        True: "yes",
        False: "no",
        <error descr="$descriptionDuplicate">True</error>: "again",
        <error descr="$descriptionDuplicate">False</error>: "again",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate None literal keys should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        None: "first",
        <error descr="$descriptionDuplicate">None</error>: "second",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate tuple literal keys should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        ("a", 16, -1, True, None): "first",
        <error descr="$descriptionDuplicate">("a", 0x10, -1.0, True, None)</error>: "second",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate nested tuple literal keys should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        (("a", 1), (False, None)): "first",
        <error descr="$descriptionDuplicate">(("a", 1.0), (False, None))</error>: "second",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `all duplicate occurrences after the first one should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "a": 1,
        <error descr="$descriptionDuplicate">"a"</error>: 2,
        <error descr="$descriptionDuplicate">"a"</error>: 3,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `different non numeric literal key types should not be highlighted as duplicates`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "1": "string",
        1: "number",
        True: "boolean",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `dynamic keys should not be highlighted as duplicates`() {
    myFixture.configureByText(
      "test.bzl",
      """
      key = "a"

      d = {
        "a": 1,
        key: 2,
        foo(): 3,
        foo(): 4,
        "a" + "": 5,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `tuple keys with dynamic elements should not be highlighted as duplicates`() {
    myFixture.configureByText(
      "test.bzl",
      """
      key = "a"

      d = {
        ("a", key): 1,
        ("a", key): 2,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate keys in nested dict literals should be highlighted independently`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "outer": {
          "inner": 1,
          <error descr="$descriptionDuplicate">"inner"</error>: 2,
        },
        <error descr="$descriptionDuplicate">"outer"</error>: {
          "inner": 3,
        },
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `escaped string duplicate should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "a": 1,
        <error descr="$descriptionDuplicate">"\141"</error>: 2,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `triple quoted string duplicate should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "a": 1,
        <error descr="$descriptionDuplicate">${"\"\"\""}a${"\"\"\""}</error>: 2,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `different string values should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        ${"\"\"\""}a${"\"\"\""}: 1,
        '""a""': 2,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `double negative duplicate should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        1: "first",
        <error descr="$descriptionDuplicate">-(-1)</error>: "second",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `rounded float duplicate should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        1.0: "first",
        <error descr="$descriptionDuplicate">1.0000000000000001</error>: "second",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `different integer and float values should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        9007199254740993: "integer",
        9007199254740993.0: "float",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `raw string duplicate should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "a": 1,
        <error descr="$descriptionDuplicate"><error descr="Missing closing quote [r]">r"a"</error></error>: 2,
      }
      """.trimIndent(),
    )
    // TODO: Remove nested parser error marker after BAZEL-3528 is fixed.

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `exact large integer and float duplicate should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      same = {
        1152921504606846976: 1, 
        <error descr="$descriptionDuplicate">1152921504606846976.0</error>: 2
      }      
      different = {
        1152921504606847000: 1, 
        1152921504606846976.0: 2
      }
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `starlark string escape duplicate keys should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "\a": 1,
        <error descr="$descriptionDuplicate">"\007"</error>: 2,

        "\v": 3,
        <error descr="$descriptionDuplicate">"\013"</error>: 4,

        "\b": 5,
        <error descr="$descriptionDuplicate">"\010"</error>: 6,

        "\f": 7,
        <error descr="$descriptionDuplicate">"\014"</error>: 8,

        "\n": 9,
        <error descr="$descriptionDuplicate">"\012"</error>: 10,

        "\r": 11,
        <error descr="$descriptionDuplicate">"\015"</error>: 12,

        "\t": 13,
        <error descr="$descriptionDuplicate">"\011"</error>: 14,

        "\\": 15,
        <error descr="$descriptionDuplicate">"\134"</error>: 16,

        "\"": 17,
        <error descr="$descriptionDuplicate">"\042"</error>: 18,

        "'": 19,
        <error descr="$descriptionDuplicate">"\047"</error>: 20,

        "\a-letter": 21,
        "a-letter": 22,

        "\v-letter": 23,
        "v-letter": 24,
        
        "é": 25,
        <error descr="$descriptionDuplicate">"\303\251"</error>: 26,
        "\351": 27,
        
        "😀": 28,
        <error descr="$descriptionDuplicate">"\360\237\230\200"</error>: 29,
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }
}
