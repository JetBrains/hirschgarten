package org.jetbrains.bazel.project

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.TestFixtureImpl
import com.intellij.testFramework.junit5.fixture.testFixture
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.bazelrunner.BazelCommandExecutionDescriptor
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncher
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.jetbrains.bazel.test.framework.writeProjectView
import org.jetbrains.bsp.protocol.TaskId
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.test.assertFailsWith

@BazelTestApplication
@Timeout(30)
internal class BazelProjectFixtureCleanupTest {
  private val context by testFixture { initialized(it) {} }

  @ParameterizedTest
  @EnumSource(FailureStage::class)
  fun `failed setup releases resources`(stage: FailureStage, @TestDisposable disposable: Disposable): Unit = timeoutRunBlocking {
    lateinit var project: Project
    lateinit var console: Disposable
    lateinit var root: Path
    val commands = mutableListOf<List<String>>()
    val launcherProvider = object : BazelProcessLauncherProvider {
      override fun createBazelProcessLauncher(workspaceRoot: Path, parentEnvironment: Map<String, String>): BazelProcessLauncher {
        root = workspaceRoot
        return object : BazelProcessLauncher {
          override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process {
            assertThat(root).exists()
            assertThat(project.isDisposed).isFalse()
            assertThat(Disposer.isDisposed(console)).isFalse()
            commands.add(executionDescriptor.command)
            return mock(Process::class.java).also {
              `when`(it.inputStream).thenReturn(InputStream.nullInputStream())
              `when`(it.errorStream).thenReturn(InputStream.nullInputStream())
              `when`(it.waitFor(anyLong(), any(TimeUnit::class.java))).thenReturn(true)
            }
          }
        }
      }
    }
    ExtensionTestUtil.maskExtensions(BazelProcessLauncherProvider.ep, listOf(launcherProvider), disposable)

    val failure = IllegalStateException("Expected fixture setup failure")
    val fixture = bazelProjectFixture("base", jvmToolchains = false) {
      project = it
      console = project.service<ConsoleService>() as Disposable
      writeProjectView(project, "directories:\n  .\nbazel_binary:\n  bazel\n")
      if (stage == FailureStage.CONFIGURE) throw failure
      project.replaceService(ProjectSyncService::class.java, object : ProjectSyncService {
        override val lastSyncTaskId: TaskId? = null

        override suspend fun sync(scope: ProjectSyncScope) {
          throw failure
        }
      }, project)
    }
    val fixtureJob = SupervisorJob(coroutineContext.job)
    try {
      val thrown = assertFailsWith<IllegalStateException> {
        (fixture as TestFixtureImpl<Project>).init(CoroutineScope(coroutineContext + fixtureJob), context).await()
      }
      assertThat(thrown).hasMessage(failure.message)
    }
    finally {
      fixtureJob.cancelAndJoin()
    }

    assertThat(commands).hasSize(1)
    assertThat(commands.single()).contains("shutdown")
    assertThat(root.exists()).isFalse()
    assertThat(project.isDisposed).isTrue()
    assertThat(Disposer.isDisposed(console)).isTrue()
  }

  enum class FailureStage { CONFIGURE, SYNC }
}
