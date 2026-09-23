package org.jetbrains.bazel.bazelrunner

import com.intellij.openapi.util.SystemInfo
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.sync.JvmToolchainQuery
import org.jetbrains.bazel.sync.BuildfilesQuery
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.io.path.Path

class BazelQueryExpressionTest {
  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `query quotes a label with spaces with either manual target setting`(allowManualTargetsSync: Boolean) {
    val command = BazelCommand.Query("bazel", allowManualTargetsSync).apply {
      targets.add(Label.parse("//path with spaces:target"))
    }

    val expected = when {
      allowManualTargetsSync -> "\"@//path with spaces:target\""
      SystemInfo.isWindows -> "attr('tags', '^((?!manual).)*$', \"@//path with spaces:target\")"
      else -> "attr(\"tags\", \"^((?!manual).)*$\", \"@//path with spaces:target\")"
    }
    command.buildExecutionDescriptor().command.last() shouldBe expected
  }

  @ParameterizedTest
  @CsvSource("query, true", "query, false", "cquery, true", "aquery, true")
  fun `queries quote each included and excluded label in one expression`(kind: String, allowManualTargetsSync: Boolean) {
    val command = createQuery(kind, allowManualTargetsSync)
    (command as HasMultipleTargets).apply {
      targets.add(Label.parse("//path with spaces/..."))
      targets.add(Label.parse("@repo//another path:target"))
      excludedTargets.add(Label.parse("//path with spaces:excluded"))
      excludedTargets.add(Label.parse("@repo//another path:all"))
    }

    val targets = "\"@//path with spaces/...:all\" + \"@repo//another path:target\" - " +
                  "\"@//path with spaces:excluded\" - \"@repo//another path:all\""
    val expected = when {
      allowManualTargetsSync -> targets
      SystemInfo.isWindows -> "attr('tags', '^((?!manual).)*$', $targets)"
      else -> "attr(\"tags\", \"^((?!manual).)*$\", $targets)"
    }
    val arguments = command.buildExecutionDescriptor().command
    arguments[1] shouldBe kind
    arguments.last() shouldBe expected
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `query without included targets stays empty`(allowManualTargetsSync: Boolean) {
    val command = BazelCommand.Query("bazel", allowManualTargetsSync).apply {
      excludedTargets.add(Label.parse("//path with spaces:excluded"))
    }

    command.buildExecutionDescriptor().command.last() shouldBe ""
  }

  @ParameterizedTest
  @ValueSource(strings = ["cquery", "aquery"])
  fun `queries preserve an expression supplied through options`(kind: String) {
    val command = createQuery(kind).apply {
      options.add("deps(\"@//path with spaces:target\")")
    }

    command.buildExecutionDescriptor().command.takeLast(2) shouldBe
      listOf("deps(\"@//path with spaces:target\")", "--")
  }

  @Test
  fun `file query quotes each path with spaces`() {
    val command = BazelRunner.CommandBuilder("bazel").fileQuery(
      listOf(Path("path with spaces/BUILD.bazel"), Path("another path/BUILD")),
    )

    command.buildExecutionDescriptor().command.last() shouldBe
      "set(\"path with spaces/BUILD.bazel\" \"another path/BUILD\")"
  }

  @Test
  fun `file query preserves an empty set`() {
    val command = BazelRunner.CommandBuilder("bazel").fileQuery(emptyList())

    command.buildExecutionDescriptor().command.last() shouldBe "set()"
  }

  @Test
  fun `reverse buildfiles query quotes each path with spaces`() {
    BuildfilesQuery.expressionOf(setOf(Path("path with spaces/rules.bzl"), Path("another path/BUILD.bazel"))) shouldBe
      "rbuildfiles(\"path with spaces/rules.bzl\",\"another path/BUILD.bazel\")"
  }

  @Test
  fun `toolchain query quotes a label with spaces`() {
    JvmToolchainQuery.javacMnemonic(Label.parse("//path with spaces:target")) shouldBe
      "mnemonic(\"Javac\", \"@//path with spaces:target\")"
  }

  @Test
  fun `test command preserves labels as separate arguments without query quotes`() {
    val command = BazelCommand.Test("bazel").apply {
      targets.add(Label.parse("//path with spaces:target"))
      excludedTargets.add(Label.parse("//path with spaces:excluded"))
    }

    command.buildExecutionDescriptor().command.takeLast(3) shouldBe
      listOf("--", "@//path with spaces:target", "-@//path with spaces:excluded")
  }

  private fun createQuery(kind: String, allowManualTargetsSync: Boolean = true): BazelCommand = when (kind) {
    "query" -> BazelCommand.Query("bazel", allowManualTargetsSync)
    "cquery" -> BazelCommand.CQuery("bazel")
    "aquery" -> BazelCommand.AQuery("bazel")
    else -> error("Unknown query kind: $kind")
  }
}
