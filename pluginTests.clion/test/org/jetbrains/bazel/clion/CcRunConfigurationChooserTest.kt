package org.jetbrains.bazel.clion

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurations
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.cidr.lang.workspace.OCWorkspaceListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.workspace.CC_CLIENT_KEY
import org.jetbrains.bazel.clion.workspace.CcResolveConfiguration
import org.jetbrains.bazel.clion.workspace.encode
import org.jetbrains.bazel.fixtures.ccProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@TestApplication
class CcRunConfigurationChooserTest {
  private val directoryFixture = tempPathFixture()
  private val directory by directoryFixture
  private val project by projectFixture(pathFixture = directoryFixture)

  private val snapshot = ccProject {
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
    initializeBazelProject(project, directory)
    project.service<WorkspaceSnapshotService>().update { snapshot to Unit }

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
  fun testSelectsConfigurationOfTarget(): Unit = timeoutRunBlocking {
    select("//app:arm")
    assertThat(choose("x86", "arm")).isEqualTo("arm")

    select("//app:x86")
    assertThat(choose("arm", "x86")).isEqualTo("x86")
  }

  @Test
  fun testSelectsFirstMatchingConfiguration(): Unit = timeoutRunBlocking {
    select("//lib:shared")
    assertThat(choose("unrelated", "x86", "arm")).isEqualTo("x86")
    assertThat(choose("unrelated", "arm", "x86")).isEqualTo("arm")
  }

  @Test
  fun testDoesNotSelectDependencyConfiguration(): Unit = timeoutRunBlocking {
    val updated = ccProject {
      val arm = ccLibrary {
        label("//lib:shared")
        configurationId("arm")
        srcs("lib/shared.cc")
      }
      ccBinary {
        label("//app:x86")
        configurationId("x86")
        deps(arm)
      }
    }
    project.service<WorkspaceSnapshotService>().update { updated to Unit }

    select("//app:x86")
    assertThat(choose("arm", "x86")).isEqualTo("x86")
    assertThat(choose("arm", "unrelated")).isNull()
  }

  @Test
  fun testDoesNotSelectOutsideSlice(): Unit = timeoutRunBlocking {
    select("//app:arm")
    assertThat(choose("x86", "unrelated")).isNull()
  }

  @Test
  fun testUnknownTarget(): Unit = timeoutRunBlocking {
    select("//missing:target")
    assertThat(choose("arm", "x86")).isNull()
  }

  @Test
  fun testClearingSelection(): Unit = timeoutRunBlocking {
    select("//app:arm")
    assertThat(choose("arm", "x86")).isEqualTo("arm")

    withContext(Dispatchers.EDT) { RunManager.getInstanceAsync(project).selectedConfiguration = null }
    assertThat(choose("arm", "x86")).isNull()
  }

  @Test
  fun testEditingSelectedTargets(): Unit = timeoutRunBlocking {
    val settings = select("//app:arm")
    assertThat(choose("arm", "x86")).isEqualTo("arm")

    val changed = CompletableDeferred<Unit>()
    project.messageBus.connect(project).subscribe(OCWorkspaceListener.TOPIC, object : OCWorkspaceListener {
      override fun selectedResolveConfigurationChanged() {
        changed.complete(Unit)
      }
    })
    withContext(Dispatchers.EDT) {
      (settings.configuration as BazelRunConfiguration).updateTargets(listOf(Label.parse("//app:x86")))
      project.messageBus.syncPublisher(RunManagerListener.TOPIC).runConfigurationChanged(settings)
    }
    changed.await()
    assertThat(choose("arm", "x86")).isEqualTo("x86")
  }

  @Test
  fun testNotifiesResolutionEngine(): Unit = timeoutRunBlocking {
    val changed = CompletableDeferred<Unit>()
    project.messageBus.connect(project).subscribe(OCWorkspaceListener.TOPIC, object : OCWorkspaceListener {
      override fun selectedResolveConfigurationChanged() {
        changed.complete(Unit)
      }
    })

    select("//app:arm")
    changed.await()
    assertThat(choose("x86", "arm")).isEqualTo("arm")
  }

  @Test
  fun testNewSnapshotInvalidatesSelection(): Unit = timeoutRunBlocking {
    select("//app:arm")
    assertThat(choose("arm", "x86")).isEqualTo("arm")

    val updated = ccProject {
      ccBinary { label("//app:arm") }
    }
    project.service<WorkspaceSnapshotService>().update { updated to Unit }
    assertThat(choose("arm", "x86")).isNull()
  }

  @Test
  fun testMultipleSelectedTargets(): Unit = timeoutRunBlocking {
    select("//app:arm", "//app:x86")
    assertThat(choose("unrelated", "arm")).isEqualTo("arm")
    assertThat(choose("unrelated", "x86")).isEqualTo("x86")
  }

  @Test
  fun testEmptyTargets(): Unit = timeoutRunBlocking {
    select()
    assertThat(choose("arm", "x86")).isNull()
  }

  @Test
  fun testIgnoresConfigurationsWithoutBazelIdentifier(): Unit = timeoutRunBlocking {
    select("//app:arm")
    assertThat(choose("plain")).isNull()
    assertThat(choose("plain", "arm")).isEqualTo("arm")
  }

  private fun configurationId(id: String): String =
    CcResolveConfiguration.Identifier(0, WorkspaceConfigurationId.of(id)).encode()

  private suspend fun select(vararg labels: String): RunnerAndConfigurationSettings = withContext(Dispatchers.EDT) {
    val manager = RunManager.getInstanceAsync(project)
    manager.createConfiguration(labels.joinToString(), BazelRunConfigurationType()).also { settings ->
      (settings.configuration as BazelRunConfiguration).updateTargets(labels.map { Label.parse(it) })
      manager.addConfiguration(settings)
      manager.selectedConfiguration = settings
    }
  }

  private suspend fun choose(vararg ids: String): String? = readAction {
    val workspace = OCWorkspace.getInstance(project)
    val configurations = ids.map { requireNotNull(workspace.getConfigurationById(if (it == "plain") it else configurationId(it))) }
    val selected = OCResolveConfigurations.findPreselectedConfiguration(project, configurations, null)
    ids.getOrNull(configurations.indexOf(selected))
  }
}
