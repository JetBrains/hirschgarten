package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.platform.diagnostic.telemetry.NoopTelemetryManager
import com.intellij.platform.diagnostic.telemetry.Scope
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.performance.BSP_SCOPE
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.startup.GenericCommandLineProcessSpawner
import org.jetbrains.bazel.startup.IntellijBidirectionalMap
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import kotlin.io.path.pathString

interface BazelSyncCodeInsightTestFixture : CodeInsightTestFixture {

  fun performBazelSync()
}

class BazelSyncCodeInsightTestFixtureImpl(
  projectFixture: IdeaProjectTestFixture,
  tempDirTestFixture: TempDirTestFixture,
) : CodeInsightTestFixtureImpl(projectFixture, tempDirTestFixture), BazelSyncCodeInsightTestFixture {

  init {
    testDataPath = BazelPathManager.testProjectsRoot.pathString
  }

  override fun performBazelSync() {
    runWithModalProgressBlocking(project, "Syncing project...") {
      ProjectSyncTask(project).sync(SecondPhaseSync, true)
    }
  }

  override fun setUp() {
    super.setUp()
    project.bazelProjectProperties.rootDir = virtualFileOf(tempDirPath)
    BidirectionalMap.provideBidirectionalMapFactory { IntellijBidirectionalMap<Any, Any>() }
    TelemetryManager.provideNoopTelemetryManager()
    ProcessSpawner.provideProcessSpawner(GenericCommandLineProcessSpawner)
    VfsRootAccess.allowRootAccess(project, "/private/var/tmp/")
  }

  override fun tearDown() {
    try {
      project.bazelProjectProperties.rootDir = null
      WriteAction.runAndWait<Throwable> {
        ProjectJdkTable.getInstance().apply {
          allJdks.forEach(this::removeJdk)
        }
      }
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}

private fun TelemetryManager.Companion.provideNoopTelemetryManager() {
  TelemetryManager.provideTelemetryManager(
    object : TelemetryManager {
      private val noop = NoopTelemetryManager()

      override fun getTracer(): Tracer = noop.getTracer(Scope(BSP_SCOPE))

      override fun getMeter(): Meter = noop.getMeter(Scope(BSP_SCOPE))
    },
  )
}

