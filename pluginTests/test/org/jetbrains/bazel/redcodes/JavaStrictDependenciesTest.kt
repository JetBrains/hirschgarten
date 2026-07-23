package org.jetbrains.bazel.redcodes

import com.intellij.mock.MockDocument
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.inspections.JavaStrictDependenciesInspection
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test

@BazelTestApplication
class JavaStrictDependenciesTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  fun testStrictDepsWithSourceTargets() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/strict_dependencies/java_strict_deps")
    fixture.enableInspections(JavaStrictDependenciesInspection())
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("src/main/com/example/a/A.java")
    }
  }

  @Test
  fun testStrictDepsWithCustomExportingRule() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/strict_dependencies/java_strict_deps_custom_rule_export")
    fixture.enableInspections(JavaStrictDependenciesInspection())
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("src/main/com/example/a/A.java")
    }
  }

  @Test
  fun testStrictDepsWithJavaExport() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/strict_dependencies/java_export")
    fixture.enableInspections(JavaStrictDependenciesInspection())
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("app/App.java")
    }
  }

  @Test
  fun testStrictDepsWithKotlinSources() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/strict_dependencies/java_strict_deps_kotlin")
    fixture.enableInspections(JavaStrictDependenciesInspection())
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("A1.java")
    }
  }

  @Test
  // https://youtrack.jetbrains.com/issue/BAZEL-1423
  fun testStrictDepsWithProtobufReference() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/strict_dependencies/java_strict_deps_proto")
    fixture.enableInspections(JavaStrictDependenciesInspection())
    fixture.performBazelSync()
    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("src/WorkRequestHandler.java")
    }
  }

  @Test
  fun testStrictDepsWithMultiverse1() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/strict_dependencies/java_strict_deps_multiverse")
    fixture.enableInspections(JavaStrictDependenciesInspection())
    fixture.performBazelSync()
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
    fixture.copyBazelTestProject("redcodes/strict_dependencies/java_strict_deps_multiverse")
    fixture.enableInspections(JavaStrictDependenciesInspection())
    fixture.performBazelSync()
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

  @Test
  fun testStrictDepsWithDeepMultiverse() = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/strict_dependencies/java_strict_deps_deep_multiverse")
    fixture.enableInspections(JavaStrictDependenciesInspection())
    fixture.performBazelSync()
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
