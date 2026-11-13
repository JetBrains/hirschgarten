package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.readAction
import com.intellij.platform.testFramework.assertion.treeAssertion.SimpleTreeAssertion
import com.intellij.platform.testFramework.assertion.treeAssertion.buildTree
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.common.timeoutRunBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.EXECUTION_TIMEOUT
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.fixtures.createBazelConfiguration
import org.jetbrains.bazel.fixtures.executeBazelConfiguration
import org.jetbrains.bazel.isWindows
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelTestRerunExtension
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcTestTreeTest {
  private val project by clionBazelProjectFixture("clion/test_tree", buildProject = true, bazelVersion = BazelVersions.BAZEL_9) {
    if (isWindows()) {
      addBuildFlags("--cxxopt=/std:c++17")
    }
    else {
      addBuildFlags("--cxxopt=-std=c++17")
    }
  }

  // Catch2 names the test suite after the test binary
  private val catchSuite = if (isWindows()) "catch.exe" else "catch"

  @BeforeAll
  fun waitForTestIndex(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    LanguageEngine.INSTANCE.waitForTestIndex(project)
  }

  @Test
  fun googleTestTree(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertTestTree("//main:gtest") {
      test("TestInHeader", null) {
        test("Probe", "TestInHeader.h:1")
      }
      test("Test1", null) {
        test("InImplementation", "ExampleTestSuite.cpp:5")
      }
      test("Test2", null) {
        test("gtest_test", "ExampleTestSuite.cpp:9")
      }
      test("Test_Name", null) {
        test("With_Underscores", "ExampleTestSuite.cpp:13")
      }
      test("TestFixture", null) {
        test("success", "ExampleTestSuite.cpp:19")
      }
      test("TypedTest", null) {
        test("0 - char", null) {
          test("success", "ExampleTestSuite.cpp:40")
          test("failure", "ExampleTestSuite.cpp:42")
        }
        test("1 - int", null) {
          test("success", "ExampleTestSuite.cpp:40")
          test("failure", "ExampleTestSuite.cpp:42")
        }
        test("2 - unsigned int", null) {
          test("success", "ExampleTestSuite.cpp:40")
          test("failure", "ExampleTestSuite.cpp:42")
        }
      }
      test("Instantiation/TypeParameterizedTest", null) {
        test("0 - char", null) {
          test("success", "ExampleTestSuite.cpp:50")
          test("failure", "ExampleTestSuite.cpp:52")
        }
        test("1 - int", null) {
          test("success", "ExampleTestSuite.cpp:50")
          test("failure", "ExampleTestSuite.cpp:52")
        }
        test("2 - unsigned int", null) {
          test("success", "ExampleTestSuite.cpp:50")
          test("failure", "ExampleTestSuite.cpp:52")
        }
      }
      test("Instantiation1to3/TestWithParam", null) {
        test("Fails1", null) {
          test("0 - 1", "ExampleTestSuite.cpp:25")
          test("1 - 2", "ExampleTestSuite.cpp:25")
          test("2 - 3", "ExampleTestSuite.cpp:25")
        }
        test("Fails5", null) {
          test("0 - 1", "ExampleTestSuite.cpp:28")
          test("1 - 2", "ExampleTestSuite.cpp:28")
          test("2 - 3", "ExampleTestSuite.cpp:28")
        }
      }
      test("Instantiation4to6/TestWithParam", null) {
        test("Fails1", null) {
          test("0 - 4", "ExampleTestSuite.cpp:25")
          test("1 - 5", "ExampleTestSuite.cpp:25")
          test("2 - 6", "ExampleTestSuite.cpp:25")
        }
        test("Fails5", null) {
          test("0 - 4", "ExampleTestSuite.cpp:28")
          test("1 - 5", "ExampleTestSuite.cpp:28")
          test("2 - 6", "ExampleTestSuite.cpp:28")
        }
      }
      test("TestWithParam", null) {
        test("Fails1", null) {
          test("0 - 1", "ExampleTestSuite.cpp:25")
          test("1 - 5", "ExampleTestSuite.cpp:25")
        }
        test("Fails5", null) {
          test("0 - 1", "ExampleTestSuite.cpp:28")
          test("1 - 5", "ExampleTestSuite.cpp:28")
        }
      }
    }
  }

  @Test
  fun catchTestTree(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    assertTestTree("//main:catch") {
      test(catchSuite, null) {
        test("Scenario: tree test", "TestBase.cpp:3") {
          test("Common", "TestBase.cpp:3") {
            test("1, comma", "TestBase.cpp:3")
            test("2", "TestBase.cpp:3")
          }
        }
        test("SUCCEED counts as a test pass", "TestBase.cpp:14")
        test("Output from all sections is reported", null) {
          test("one", "TestBase.cpp:19")
          test("two", "TestBase.cpp:19")
        }
        test("checkedIf, failing", "TestBase.cpp:32")
      }
    }
  }

  @Test
  fun googleTestRerunKeepsInstantiationAndParam(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val run = runTests("//main:gtest")
    assertThat(run.rerunConfiguration("Instantiation1to3/TestWithParam", "Fails1", "1 - 2").testFilter)
      .isEqualTo("Instantiation1to3/TestWithParam.Fails1/1:Instantiation1to3/TestWithParam/1.Fails1")
    assertThat(run.rerunConfiguration("TypedTest", "1 - int", "success").testFilter)
      .isEqualTo("TypedTest.success:TypedTest/1.success:TypedTest.success/1:*/TypedTest.success/*:*/TypedTest/*.success")
  }

  @Test
  fun catchRerunSelectsWholeTestCase(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val run = runTests("//main:catch")
    assertThat(run.rerunConfiguration(catchSuite, "SUCCEED counts as a test pass").testFilter).isEqualTo("SUCCEED counts as a test pass")
    assertThat(run.rerunConfiguration(catchSuite, "Scenario: tree test", "Common", "2").testFilter).isEqualTo("Scenario: tree test")

    // A test filter can't select sections, so the rerun of a section runs all sections of its test case
    val sectionConfiguration = run.rerunConfiguration(catchSuite, "Output from all sections is reported", "one")
    assertThat(sectionConfiguration.testFilter).isEqualTo("Output from all sections is reported")
    val sectionRun = project.executeBazelConfiguration(sectionConfiguration, DefaultRunExecutor.getRunExecutorInstance())
    assertThat(sectionRun.testNames).containsExactly("Output from all sections is reported.one", "Output from all sections is reported.two")
  }

  private class TestRun(val configuration: BazelRunConfiguration, val testsRoot: SMTestProxy)

  private suspend fun runTests(label: String): TestRun {
    val configuration = project.createBazelConfiguration(Label.parse(label))
    val testsRoot = project.executeBazelConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance()).testsRoot.assertNotNull()
    return TestRun(configuration, testsRoot)
  }

  /** The rerun of the node at [path]. Right-click on the node must select the same tests. */
  private suspend fun TestRun.rerunConfiguration(vararg path: String): BazelRunConfiguration {
    val test = path.fold(testsRoot) { node, name -> node.children.single { it.name == name } }
    val rerunConfiguration = configuration.clone() as BazelRunConfiguration
    readAction { BazelTestRerunExtension.getInstance(rerunConfiguration).assertNotNull().setTestsToRerun(rerunConfiguration, listOf(test)) }
    val contextConfigurations = readAction {
      val location = test.getLocation(project, GlobalSearchScope.allScope(project)).assertNotNull()
      ConfigurationContext.createEmptyContextForLocation(location).configurationsFromContext.orEmpty().map { it.configuration }
    }
    assertThat(contextConfigurations.map { (it as BazelRunConfiguration).testFilter }).containsExactly(rerunConfiguration.testFilter)
    return rerunConfiguration
  }

  private val BazelRunConfiguration.testFilter: String?
    get() = (handler?.state as HasTestFilter).testFilter

  private suspend fun assertTestTree(label: String, assert: SimpleTreeAssertion<SMTestProxy>.() -> Unit) {
    val configuration = project.createBazelConfiguration(Label.parse(label))
    val testsRoot = project.executeBazelConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance()).testsRoot.assertNotNull()
    SimpleTreeAssertion.assertTree(buildTree(testsRoot.children, { name }, { children }), assert)
  }

  /** Asserts a node and where its location points to, as `file:line`, or null without a location. */
  private fun SimpleTreeAssertion<SMTestProxy>.test(
    name: String,
    declaration: String?,
    assertChildren: SimpleTreeAssertion.Node<SMTestProxy>.() -> Unit = {},
  ) {
    assertNode(name) {
      assertValue { assertThat(declaration(it)).isEqualTo(declaration) }
      assertChildren()
    }
  }

  private fun declaration(test: SMTestProxy): String? =
    ReadAction.nonBlocking<String?> {
      val element = test.getLocation(project, GlobalSearchScope.allScope(project))?.psiElement ?: return@nonBlocking null
      val file = element.containingFile
      val line = PsiDocumentManager.getInstance(project).getDocument(file)!!.getLineNumber(element.textRange.startOffset)
      "${file.name}:${line + 1}"
    }.executeSynchronously()
}
