package org.jetbrains.bazel.redcodes

import com.intellij.mock.MockDocument
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.ExpectedHighlightingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.inspections.JavaStrictDependenciesInspection
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

class JavaStrictDependenciesTest {

  @Nested
  @BazelTestApplication
  inner class SourceTargets {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/strict_dependencies/java_strict_deps",
      configure = { it.enableInspections(JavaStrictDependenciesInspection()) },
    )

    @Test
    fun testStrictDepsWithSourceTargets() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("src/main/com/example/a/A.java")
      }
    }
  }

  @Nested
  @BazelTestApplication
  inner class CustomExportingRule {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/strict_dependencies/java_strict_deps_custom_rule_export",
      configure = { it.enableInspections(JavaStrictDependenciesInspection()) },
    )

    @Test
    fun testStrictDepsWithCustomExportingRule() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("src/main/com/example/a/A.java")
      }
    }
  }

  @Nested
  @BazelTestApplication
  inner class JavaExport {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/strict_dependencies/java_export",
      configure = { it.enableInspections(JavaStrictDependenciesInspection()) },
    )

    @Test
    fun testStrictDepsWithJavaExport() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("app/App.java")
      }
    }
  }

  @Nested
  @BazelTestApplication
  inner class KotlinSources {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/strict_dependencies/java_strict_deps_kotlin",
      configure = { it.enableInspections(JavaStrictDependenciesInspection()) },
    )

    @Test
    fun testStrictDepsWithKotlinSources() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("A1.java")
      }
    }
  }

  @Nested
  @BazelTestApplication
  inner class ProtobufReference {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/strict_dependencies/java_strict_deps_proto",
      configure = { it.enableInspections(JavaStrictDependenciesInspection()) },
    )

    @Test
    @DisabledOnOs(OS.WINDOWS) //cpp toolchain
    // https://youtrack.jetbrains.com/issue/BAZEL-1423
    fun testStrictDepsWithProtobufReference() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("src/WorkRequestHandler.java")
      }
    }
  }

  @Nested
  @BazelTestApplication
  inner class Multiverse {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/strict_dependencies/java_strict_deps_multiverse",
      configure = { it.enableInspections(JavaStrictDependenciesInspection()) },
    )

    @Test
    fun testStrictDepsWithMultiverse1() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting(
          "Main.java", "main1",
          expected = ExpectedHighlightingData(
            MockDocument().apply {
              replaceText(
                """
                class Main {
                  public void foo() {
                    <error descr="Using type B from an indirect dependency @//:lib_b1">B</error> b = new A().bar();
                  }
                }
               """.trimIndent(),
                1,
              )
            },
          ).also { it.init() },
        )
      }
    }

    @Test
    fun testStrictDepsWithMultiverse2() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {

        fixture.checkHighlighting(
          "Main.java", "main2",
          expected = ExpectedHighlightingData(
            MockDocument().apply {
              replaceText(
                """
                class Main {
                  public void foo() {
                    <error descr="Using type B from an indirect dependency @//:lib_b2">B</error> b = new A().bar();
                  }
                }
               """.trimIndent(),
                1,
              )
            },
          ).also { it.init() },
        )
      }
    }
  }

  @Nested
  @BazelTestApplication
  inner class DeepMultiverse {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/strict_dependencies/java_strict_deps_deep_multiverse",
      configure = { it.enableInspections(JavaStrictDependenciesInspection()) },
    )

    @Test
    fun testStrictDepsWithDeepMultiverse() = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {

        fixture.checkHighlighting(
          "Main.java", "main2",
          expected = ExpectedHighlightingData(
            MockDocument().apply {
              replaceText(
                """
                class Main {
                  public void foo() {
                    <error descr="Using type B from an indirect dependency @//:lib_b2">B</error> b = new A().bar();
                  }
                }
               """.trimIndent(),
                1,
              )
            },
          ).also { it.init() },
        )
      }
    }
  }
}
