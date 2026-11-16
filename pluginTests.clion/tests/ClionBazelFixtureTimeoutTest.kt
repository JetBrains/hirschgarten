package org.jetbrains.bazel.fixtures

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Checks that a hang in the setup of [clionBazelProjectFixture] fails the test. Without a timeout, a hang in the sync or
 * in the backend blocks the test target until the TeamCity limit, and gives no diagnosis.
 */
class ClionBazelFixtureTimeoutTest {
  // The guard against a missing timeout: a missing timeout must fail this test, and not hang it.
  private val guardTimeout = 30.seconds

  @Test
  fun testHangInOpenProjectFailsTheSetup() {
    val failure = runSetup(openProject = { awaitCancellation() }, waitForSymbols = {})
    assertThat(failure).hasMessageContaining("Timeout after")
  }

  @Test
  fun testHangInWaitForSymbolsFailsTheSetup() {
    val failure = runSetup(openProject = { "project" }, waitForSymbols = { awaitCancellation() })
    assertThat(failure).hasMessageContaining("Timeout after")
  }

  @Test
  fun testSetupReturnsTheProject() {
    val waited = mutableListOf<String>()
    val project = runBlocking {
      setUpClionBazelProject(timeout = 1.seconds, openProject = { "project" }, waitForSymbols = { waited += it })
    }
    assertThat(project).isEqualTo("project")
    assertThat(waited).containsExactly("project")
  }

  private fun runSetup(openProject: suspend () -> String, waitForSymbols: suspend (String) -> Unit): Throwable = runBlocking {
    val failure = withTimeoutOrNull(guardTimeout) {
      runCatching { setUpClionBazelProject(timeout = 100.milliseconds, openProject, waitForSymbols) }.exceptionOrNull()
    }
    checkNotNull(failure) { "The setup did not fail within $guardTimeout. The fixture setup has no timeout." }
  }
}
