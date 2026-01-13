package org.jetbrains.bazel.test.framework

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightingSessionImpl
import com.intellij.codeInsight.multiverse.anyContext
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.jobToIndicator
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.util.ProperTextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.diagnostic.telemetry.NoopTelemetryManager
import com.intellij.platform.diagnostic.telemetry.Scope
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestUtil
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.performance.BSP_SCOPE
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.startup.GenericCommandLineProcessSpawner
import org.jetbrains.bazel.startup.IntellijBidirectionalMap
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolute
import kotlin.io.path.copyToRecursively

/**
 * Base class for tests that require a sync.
 *
 * Primarily created for testing redcode issues.
 *
 * This is a *heavy* test because:
 * - Sync operations require real filesystem access.
 * - Reproducing redcode issues often involves multiple modules, which are only allowed in heavy tests.
 */
abstract class BazelSyncingTestCase : HeavyPlatformTestCase() {

  protected val projectRoot by lazy { createTestProjectStructure() }
  protected val projectRootPath by lazy { projectRoot.toNioPath() }

  override fun tearDown() {
    try {
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

  @OptIn(ExperimentalPathApi::class)
  protected fun copyToProjectRootFromTestData(path: String) {
    BazelPathManager
      .getTestFixturePath(path)
      .copyToRecursively(target = projectRootPath, followLinks = false, overwrite = true)
  }

  protected fun performSync() {
    project.bazelProjectProperties.rootDir = projectRoot
    BidirectionalMap.provideBidirectionalMapFactory { IntellijBidirectionalMap<Any, Any>() }
    TelemetryManager.provideNoopTelemetryManager()
    ProcessSpawner.provideProcessSpawner(GenericCommandLineProcessSpawner)
    VfsRootAccess.allowRootAccess(project, "/private/var/tmp/")
    runWithModalProgressBlocking(project, "Syncing project...") {
      ProjectSyncTask(project).sync(SecondPhaseSync, true)
    }
  }

  protected fun doHighlightingOn(file: Path): List<HighlightInfo> {
    val virtualFile = file.toVirtualFile()
    val psiFile = virtualFile.toPsi()
    val editor = CodeInsightTestUtil.openEditorFor(psiFile)
    return runBlocking(Dispatchers.Default) {
      doHighlighting(psiFile, editor.document)
    }
  }

  protected fun assertNoErrorsHighlighted(file: Path) {
    val errors = doHighlightingOn(file).filter { it.severity == HighlightSeverity.ERROR }
    if (errors.isEmpty()) return
    val absoluteFilePath = projectRootPath.absolute()
      .relativize(file)
      .absolute()
    val document = CodeInsightTestUtil
      .openEditorFor(file.toVirtualFile().toPsi())
      .document
    val renderedErrors = errors.joinToString("\n") {
      val line = document.getLineNumber(it.startOffset)
      val lineStart = document.getLineStartOffset(line)
      val column = it.startOffset - lineStart
      "${absoluteFilePath}:${line + 1}:$column: ${it.description}"
    }
    throw AssertionError("Expected no errors but found: \n${renderedErrors}")
  }

  private suspend fun doHighlighting(psi: PsiFile, document: Document): List<HighlightInfo> {
    val indicator = DaemonProgressIndicator()
    return jobToIndicator(currentCoroutineContext().job, indicator) {
      runInsideHighlightingSession(psi, document) {
        DaemonCodeAnalyzerEx
          .getInstanceEx(project)
          .runMainPasses(psi, document, indicator)
      }
    }
  }

  private fun Path.toVirtualFile() = project
    .service<WorkspaceModel>()
    .getVirtualFileUrlManager()
    .let(this::toVirtualFileUrl)
    .virtualFile
    .let(::checkNotNull)

  private fun VirtualFile.toPsi() = checkNotNull(PsiManager.getInstance(project).findFile(this))
}

private inline fun <T> runInsideHighlightingSession(psi: PsiFile, document: Document, crossinline block: () -> T): T {
  var result: T? = null
  HighlightingSessionImpl.runInsideHighlightingSession(
    /* psiFile = */ psi,
    /* codeInsightContext = */ anyContext(),
    /* editorColorsScheme = */ null,
    /* visibleRange = */ ProperTextRange(0, document.textLength),
    /* canChangeFileSilently = */ false,
  ) {
    result = block()
  }
  return checkNotNull(result)
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
