package org.jetbrains.bazel.clion

import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurations
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.OCWorkspaceListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.workspace.CC_CLIENT_KEY
import org.jetbrains.bazel.clion.workspace.CcDebugSessionConfigurations
import org.jetbrains.bazel.clion.workspace.CcResolveConfiguration
import org.jetbrains.bazel.clion.workspace.encode
import org.jetbrains.bazel.fixtures.CcProjectBuilder
import org.jetbrains.bazel.fixtures.ccTargets
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class CcDebugSessionChooserTest {
  private val testBazel by testBazelProjectFixture(openAfterCreation = false)
  private val project get() = testBazel.project

  private val targets: CcProjectBuilder.() -> Unit = {
    val arm = ccLibrary {
      label("//lib:shared")
      configurationId("arm")
      srcs("lib/shared.cc")
    }
    val x86 = ccLibrary {
      label("//lib:shared")
      configurationId("x86")
      srcs("lib/shared.cc")
    }
    ccBinary {
      label("//app:arm")
      configurationId("arm")
      deps(arm)
    }
    ccBinary {
      label("//app:x86")
      configurationId("x86")
      deps(x86)
    }
  }

  @BeforeEach
  fun setUp(): Unit = timeoutRunBlocking {
    testBazel.sync { ccTargets(targets) }

    val workspace = OCWorkspace.getInstance(project)
    val model = workspace.getModifiableModel(CC_CLIENT_KEY, true)
    try {
      for (id in listOf("arm", "x86", "unrelated")) {
        model.addConfiguration(configurationId(id), id)
      }
      model.addConfiguration("plain", "Plain")
      model.preCommit()
      model.commitAndContribute()
    }
    finally {
      Disposer.dispose(model)
    }
  }

  @Test
  fun testNoDebugSession(): Unit = timeoutRunBlocking {
    assertThat(choose("arm", "x86")).isNull()
  }

  @Test
  fun testSelectsConfigurationOfDebugSession(): Unit = timeoutRunBlocking {
    debug("arm")
    assertThat(choose("x86", "arm")).isEqualTo("arm")

    debug("x86")
    assertThat(choose("arm", "x86")).isEqualTo("x86")
  }

  @Test
  fun testPrefersConfigurationOfDebuggedTarget(): Unit = timeoutRunBlocking {
    debug("x86", "arm")
    assertThat(choose("arm", "x86")).isEqualTo("x86")
    assertThat(choose("unrelated", "arm")).isEqualTo("arm")
  }

  @Test
  fun testDoesNotSelectOutsideSlice(): Unit = timeoutRunBlocking {
    debug("arm")
    assertThat(choose("x86", "unrelated")).isNull()
  }

  @Test
  fun testUnknownConfiguration(): Unit = timeoutRunBlocking {
    debug("missing")
    assertThat(choose("arm", "x86")).isNull()
  }

  @Test
  fun testNotifiesResolutionEngine(): Unit = timeoutRunBlocking {
    val changed = CompletableDeferred<Unit>()
    project.messageBus.connect(project).subscribe(OCWorkspaceListener.TOPIC, object : OCWorkspaceListener {
      override fun selectedResolveConfigurationChanged() {
        changed.complete(Unit)
      }
    })

    debug("arm")
    changed.await()
    assertThat(choose("x86", "arm")).isEqualTo("arm")
  }

  @Test
  fun testIgnoresConfigurationsWithoutBazelIdentifier(): Unit = timeoutRunBlocking {
    debug("arm")
    assertThat(choose("plain")).isNull()
    assertThat(choose("plain", "arm")).isEqualTo("arm")
  }

  @Test
  fun testCanceledUpdateNotifiesOnRetry(): Unit = timeoutRunBlocking {
    debug("arm")
    val tracker = OCWorkspace.getInstance(project).modificationTrackers.selectedResolveConfigurationTracker
    val before = tracker.modificationCount

    coroutineScope {
      launch {
        currentCoroutineContext().cancel()
        debug("x86")
      }.join()
    }
    assertThat(tracker.modificationCount).isEqualTo(before)
    assertThat(choose("arm", "x86")).isEqualTo("arm")

    debug("x86")
    assertThat(tracker.modificationCount).isEqualTo(before + 1)
    assertThat(choose("arm", "x86")).isEqualTo("x86")
  }

  private fun configurationId(id: String): String =
    CcResolveConfiguration.Identifier(0, WorkspaceConfigurationId.of(id)).encode()

  /** Simulates the start of a debug session with a build in the given configurations. */
  private suspend fun debug(vararg ids: String) {
    CcDebugSessionConfigurations.getInstance(project).update(ids.map { WorkspaceConfigurationId.of(it) })
  }

  private suspend fun choose(vararg ids: String): String? = readAction {
    val workspace = OCWorkspace.getInstance(project)
    val configurations = ids.map { requireNotNull(workspace.getConfigurationById(if (it == "plain") it else configurationId(it))) }
    val selected = OCResolveConfigurations.findPreselectedConfiguration(project, configurations, null)
    ids.getOrNull(configurations.indexOf(selected))
  }
}
