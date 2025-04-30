package org.jetbrains.bazel.fastbuild

import com.intellij.compiler.CompilerMessageImpl
import com.intellij.compiler.impl.CompileContextImpl
import com.intellij.compiler.impl.ExitStatus
import com.intellij.compiler.impl.OneProjectItemCompileScope
import com.intellij.compiler.progress.CompilerTask
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.impl.HotSwapFile
import com.intellij.debugger.impl.HotSwapManager
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.debugger.ui.HotSwapProgressImpl
import com.intellij.debugger.ui.HotSwapUIImpl
import com.intellij.debugger.ui.RunHotswapDialog
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import org.jetbrains.bazel.flow.sync.BazelBinPathService
import org.jetbrains.bsp.protocol.FastBuildCommand
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream

object FastBuildUtils {

  private val logger = Logger.getInstance(FastBuildUtils::class.java)

  const val fastBuildEnabledKey = "bsp.enable.jvm.fastbuild"

  fun fastBuildFiles(project: Project, fastBuildCommand: FastBuildCommand, file: VirtualFile, tmpFastBuild: Path): Promise<ProjectTaskRunner.Result> {
    val promise = AsyncPromise<ProjectTaskRunner.Result>()
      val compileTask = CompilerTask(project, "Recompile", false, false, true, false)
      val compileScope = OneProjectItemCompileScope(project, file)
      val compileContext = CompileContextImpl(project, compileTask, compileScope, true, false)
      compileTask.start({
        val indicator = ProgressManager.getInstance().progressIndicator

        indicator.checkCanceled()


        val arguments = listOf(
          fastBuildCommand.builderScript,
          *fastBuildCommand.builderArgs.toTypedArray()
        )
        val command = GeneralCommandLine(arguments).apply {
          workDirectory = File(BazelBinPathService.getInstance(project).bazelExecPath ?: TODO())
        }

        try {
          val handler = OSProcessHandler(command)
          handler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
              if (outputType == ProcessOutputType.STDERR) {
                val parts = event.text.split(':')
                val line = parts.firstOrNull()?.trim()?.toIntOrNull()
                if (parts.size >= 4 && line != null) {
                  val error = parts[3]
                  compileContext.addMessage(
                    CompilerMessageImpl(
                      project,
                      CompilerMessageCategory.ERROR,
                      error,
                      file,
                      line,
                      0,
                      null
                    )
                  )
                } else {
                  compileContext.addMessage(
                    CompilerMessageImpl(
                      project,
                      CompilerMessageCategory.ERROR,
                      event.text
                    )
                  )
                }
              } else if (outputType == ProcessOutputType.STDOUT) {
                compileContext.addMessage(
                  CompilerMessageImpl(
                    project,
                    CompilerMessageCategory.INFORMATION,
                    event.text
                  )
                )
              }
            }
          })
          handler.startNotify()
          if (!handler.waitFor() || handler.exitCode != 0) {
            compileTask.setEndCompilationStamp(ExitStatus.ERRORS, System.currentTimeMillis())
            return@start
          }
        } catch (e: ExecutionException) {

          compileContext.addMessage(
            CompilerMessageImpl(
              project,
              CompilerMessageCategory.ERROR,
              e.message
            )
          )
          compileTask.setEndCompilationStamp(ExitStatus.ERRORS, System.currentTimeMillis())
          return@start
        }
        compileTask.setEndCompilationStamp(ExitStatus.SUCCESS, System.currentTimeMillis())

        processAndHotswapOutput(fastBuildCommand.outputFile, tmpFastBuild, project)
        promise.setResult(TaskRunnerResults.SUCCESS)
      }, null)
    return promise
  }

  /**
   * Unzips the jar and uses the files within to hotswap
   */
  fun processAndHotswapOutput(outputJar: Path, tmpFastBuildDir: Path, project: Project) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Hotswapping", true) {
      override fun run(indicator: ProgressIndicator) {
        val files = buildList {
          ZipFile(outputJar.toFile()).use { zipFile ->
            zipFile.entries().iterator().forEach {
              if (it.isDirectory || !it.name.endsWith(".class")) {
                return@forEach
              }
              val output = tmpFastBuildDir.resolve(it.name)
              output.parent.toFile().mkdirs()

              zipFile.getInputStream(it).use { it.copyTo(output.outputStream())}
              add(it.name)
            }
          }
        }

        val hotswapMap = files.associate {
          it.replace(File.separatorChar, '.').let {
            if (it.endsWith(".class")) {
              return@let it.substringBeforeLast('.')
            }
            it
          } to HotSwapFile(tmpFastBuildDir.resolve(it).toFile())
        }

        indicator.checkCanceled()
//        span.end()
        //updateRuntimeJar(originalOutputJar, outputJar.parent, unzip, files)
        hotswapFile(project, hotswapMap)
      }
    })

  }

  fun hotswapFile(project: Project, hotswapMap: Map<String, HotSwapFile>) {

    ApplicationManager.getApplication().invokeLater {
      val sessionsToHotswap = getSessionsToHotswap(project)
      val progress = HotSwapProgressImpl(project)
      ApplicationManager.getApplication().executeOnPooledThread {
        try {
          val sessionMap = sessionsToHotswap.associateWith { hotswapMap }
          ProgressManager.getInstance().runProcess({
            HotSwapManager.reloadModifiedClasses(sessionMap, progress)
          }, progress.progressIndicator)
        } finally {
          triggerHotswapSuccessNotification(project, progress)
          progress.finished()
//          hotswapSpan.end()
        }
      }
    }
  }

  private fun triggerHotswapSuccessNotification(project: Project, progress: HotSwapProgressImpl) {
    try {
      val hasErrorsMethod = HotSwapProgressImpl::class.java.getDeclaredMethod("hasErrors")
      hasErrorsMethod.setAccessible(true)
      val hasErrorsResult: Boolean = hasErrorsMethod.invoke(progress) as Boolean
      // Send notifications for hotswap completed without errors
      if (!hasErrorsResult) {
        NotificationGroupManager.getInstance().getNotificationGroup("HotSwap Messages").createNotification(
          "Hotswap successful",
          "Hotswap completed without errors",
          NotificationType.INFORMATION
        ).setImportant(false).notify(project)
      }
    } catch (e: Exception) {
//      span.recordException(e)
      logger.warn("Failed to trigger hotswap completed notification", e)
    }
  }

  private fun getSessionsToHotswap(project: Project): Collection<DebuggerSession> {
    val sessions = getDebugSessions(project)
    if (DebuggerSettings.getInstance().RUN_HOTSWAP_AFTER_COMPILE == DebuggerSettings.RUN_HOTSWAP_ASK) {
      val runHotswapDialog = RunHotswapDialog(project, sessions, false)
      if (!runHotswapDialog.showAndGet()) {
        return emptyList()
      }

      return runHotswapDialog.sessionsToReload
    } else {
      return sessions
    }
  }

  fun getDebugSessions(project: Project) = DebuggerManagerEx.getInstanceEx(project).sessions.stream()
    .filter { debuggerSession: DebuggerSession? -> HotSwapUIImpl.canHotSwap(debuggerSession!!) }
    .toList()
}
