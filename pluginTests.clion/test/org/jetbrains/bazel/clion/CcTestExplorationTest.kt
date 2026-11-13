package org.jetbrains.bazel.clion

import com.intellij.clion.testFramework.nolang.junit5.core.LanguageEngine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.application.readAction
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.execution.testing.OCTestExecution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.EXECUTION_TIMEOUT
import org.jetbrains.bazel.fixtures.clionBazelProjectFixture
import org.jetbrains.bazel.fixtures.createBazelConfiguration
import org.jetbrains.bazel.fixtures.executeBazelConfiguration
import org.jetbrains.bazel.isWindows
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.BazelVersions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@CcTestApplication
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CcTestExplorationTest {
  private val project by clionBazelProjectFixture("clion/execution", buildProject = true, bazelVersion = BazelVersions.BAZEL_9) {
    if (isWindows()) {
      addBuildFlags("--cxxopt=/std:c++17")
    }
    else {
      addBuildFlags("--cxxopt=-std=c++17")
    }
  }

  @BeforeAll
  fun waitForTestIndex(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    LanguageEngine.INSTANCE.waitForTestIndex(project)
  }

  @Test
  fun testResultResolvesFromTestCache(): Unit = timeoutRunBlocking(EXECUTION_TIMEOUT) {
    val configuration = project.createBazelConfiguration(Label.parse("//main:gtest"))
    val result = project.executeBazelConfiguration(configuration, DefaultRunExecutor.getRunExecutorInstance())
    val test = result.leaves.single { it.name == "FilteredTest" }
    val url = checkNotNull(test.locationUrl)
    val locator = checkNotNull(test.locator)
    val file = withContext(Dispatchers.Default) {
      readAction {
        // The test tree resolves locations on the EDT, meaning OCTestExecution.canWaitInThisThread returns false.
        // In unit test mode, CLion waits for its backend on every thread, so we have to wrap it in this method.
        OCTestExecution.executeWithCachedValuesFastIgnoreUTMode {
          locator.getLocation(VirtualFileManager.extractProtocol(url)!!, VirtualFileManager.extractPath(url), project, GlobalSearchScope.allScope(project))
            .firstNotNullOfOrNull { it.virtualFile }
        }
      }
    }
    assertThat(file?.name).isEqualTo("gtest.cc")
  }
}
