package org.jetbrains.bazel.sync.workspace.languages.sro

import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix.SourcePatternEval
import org.junit.jupiter.api.Test

class SourcePatternEvalTest {

  @Test
  fun `test general happy case`() {
    val items = listOf(
      "root/src/com/jetbrains/bazel/Class1.java",
      "root/src/com/jetbrains/bazel/Class2.java",
      "root/src/com/jetbrains/bazel/Class3.java",
      "root/excluded_from_pattern/Class4.java",
    )
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(Matchers.startsWith("root/src/com/jetbrains")),
      excludes = listOf(Matchers.startsWith("root/excluded_from_pattern")),
    )

    matched.shouldContainExactly(
      listOf(
        "root/src/com/jetbrains/bazel/Class1.java",
        "root/src/com/jetbrains/bazel/Class2.java",
        "root/src/com/jetbrains/bazel/Class3.java",
      ),
    )
    unmatched.shouldContainExactly(listOf("root/excluded_from_pattern/Class4.java"))
  }


  @Test
  fun `test empty items list`() {
    val items = emptyList<String>()
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(Matchers.startsWith("root/src")),
      excludes = listOf(Matchers.startsWith("root/excluded")),
    )

    matched.shouldContainExactly(emptyList())
    unmatched.shouldContainExactly(emptyList())
  }

  @Test
  fun `test empty includes list returns all items as unmatched`() {
    val items = listOf(
      "root/src/com/jetbrains/Class1.java",
      "root/excluded/Class2.java",
    )
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = emptyList(),
      excludes = listOf(Matchers.startsWith("root/excluded")),
    )

    matched.shouldContainExactly(emptyList())
    unmatched.shouldContainExactly(
      listOf(
        "root/src/com/jetbrains/Class1.java",
        "root/excluded/Class2.java",
      ),
    )
  }

  @Test
  fun `test empty excludes list with includes`() {
    val items = listOf(
      "root/src/com/jetbrains/Class1.java",
      "root/other/Class2.java",
    )
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(Matchers.startsWith("root/src")),
      excludes = emptyList(),
    )

    matched.shouldContainExactly(listOf("root/src/com/jetbrains/Class1.java"))
    unmatched.shouldContainExactly(listOf("root/other/Class2.java"))
  }

  @Test
  fun `test all items match includes`() {
    val items = listOf(
      "root/src/Class1.java",
      "root/src/Class2.java",
      "root/src/Class3.java",
    )
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(Matchers.startsWith("root/src")),
      excludes = emptyList(),
    )

    matched.shouldContainExactly(items)
    unmatched.shouldContainExactly(emptyList())
  }

  @Test
  fun `test all items match excludes`() {
    val items = listOf(
      "root/excluded/Class1.java",
      "root/excluded/Class2.java",
    )
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(Matchers.startsWith("root/excluded")),
      excludes = listOf(Matchers.startsWith("root/excluded")),
    )

    matched.shouldContainExactly(emptyList())
    unmatched.shouldContainExactly(items)
  }

  @Test
  fun `test no items match includes`() {
    val items = listOf(
      "root/other/Class1.java",
      "root/different/Class2.java",
    )
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(Matchers.startsWith("root/src")),
      excludes = emptyList(),
    )

    matched.shouldContainExactly(emptyList())
    unmatched.shouldContainExactly(items)
  }

  @Test
  fun `test excludes take precedence over includes`() {
    val items = listOf(
      "root/src/excluded/Class1.java",
      "root/src/included/Class2.java",
    )
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(Matchers.startsWith("root/src")),
      excludes = listOf(Matchers.startsWith("root/src/excluded")),
    )

    matched.shouldContainExactly(listOf("root/src/included/Class2.java"))
    unmatched.shouldContainExactly(listOf("root/src/excluded/Class1.java"))
  }

  @Test
  fun `test multiple include and exclude patterns`() {
    val items = listOf(
      "root/src/main/Class1.java",
      "root/src/test/Class2.java",
      "root/lib/dependency/Class3.java",
      "root/build/output/Class4.java",
    )
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(
        Matchers.startsWith("root/src"),
        Matchers.startsWith("root/lib"),
      ),
      excludes = listOf(
        Matchers.startsWith("root/src/test"),
        Matchers.startsWith("root/build"),
      ),
    )

    matched.shouldContainExactly(
      listOf(
        "root/src/main/Class1.java",
        "root/lib/dependency/Class3.java",
      ),
    )
    unmatched.shouldContainExactly(
      listOf(
        "root/src/test/Class2.java",
        "root/build/output/Class4.java",
      ),
    )
  }

  @Test
  fun `test single item matching include`() {
    val items = listOf("root/src/Class1.java")
    val (matched, unmatched) = SourcePatternEval.evalIdentity(
      items = items,
      includes = listOf(Matchers.startsWith("root/src")),
      excludes = emptyList(),
    )

    matched.shouldContainExactly(listOf("root/src/Class1.java"))
    unmatched.shouldContainExactly(emptyList())
  }



  private object Matchers {
    fun startsWith(str: String): (String) -> Boolean = { it.startsWith(str) }
  }
}
