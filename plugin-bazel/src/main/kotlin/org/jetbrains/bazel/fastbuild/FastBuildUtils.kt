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
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeIntentReadAction
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import com.intellij.util.io.delete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.flow.sync.BazelBinPathService
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.io.File
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import kotlin.io.path.reader
import kotlin.io.path.relativeTo
import kotlin.io.path.writer

object FastBuildUtils {
  private val logger = Logger.getInstance(FastBuildUtils::class.java)

  fun fastBuildFilesPromise(project: Project, files: List<VirtualFile>): Promise<ProjectTaskRunner.Result> {
    val promise = AsyncPromise<ProjectTaskRunner.Result>()
    BazelCoroutineService.getInstance(project).start {
      try {
        fastBuildFiles(project, files)
        promise.setResult(TaskRunnerResults.SUCCESS)
      } catch (e: Exception) {
        promise.setResult(TaskRunnerResults.FAILURE)

        NotificationGroupManager
          .getInstance()
          .getNotificationGroup("HotSwap Messages")
          .createNotification(
            BazelPluginBundle.message("widget.fastbuild.error.title"),
            e.localizedMessage,
            NotificationType.ERROR,
          ).setImportant(true)
          .notify(project)
      }
    }
    return promise
  }

  suspend fun fastBuildFiles(project: Project, files: List<VirtualFile>) {
    val workspaceRoot = project.rootDir.toNioPath()
    val targetUtils = project.targetUtils
    val virtualFileManager = VirtualFileManager.getInstance()
    val buildInfos =
      files
        .mapNotNull { file ->
          val targetForFile = targetUtils.getTargetsForFile(file).firstOrNull() ?: return@mapNotNull null
          targetUtils.getBuildTargetForLabel(targetForFile)?.let { file to it }
        }.toMap()

    if (buildInfos.isEmpty()) {
      throw ExecutionException(BazelPluginBundle.message("widget.fastbuild.error.no.targets"))
    }

    for (entry in buildInfos) {
      val inputFile = entry.key
      val canonicalFilePath = inputFile.canonicalPath
      val canonicalWorkspaceRootPath = workspaceRoot.toCanonicalPath()
      if (!(canonicalFilePath?.startsWith(canonicalWorkspaceRootPath) ?: false)) {
        throw ExecutionException(
          BazelPluginBundle.message(
            "widget.fastbuild.error.wrong.canonical.path",
            canonicalFilePath.orEmpty(),
            canonicalWorkspaceRootPath,
          ),
        )
      }
      val relativePath =
        entry.value.baseDirectory.let {
          virtualFileManager.findFileByNioPath(it)?.toNioPath()?.relativeTo(workspaceRoot)
        } ?: continue
      val isLib = targetUtils.isLibrary(entry.value.id)
      val bazelBin =
        BazelBinPathService.getInstance(project).bazelBinPath
          ?: throw ExecutionException(BazelPluginBundle.message("widget.fastbuild.error.missing.path"))
      val targetJar = Path.of("$bazelBin/$relativePath/${if (isLib) "lib" else ""}${entry.value.id.targetName}.jar")

      val originalParamsFile = targetJar.parent.resolve(targetJar.fileName.name + "-0.params")
      val originalParamsFile1 = targetJar.parent.resolve(targetJar.fileName.name + "-1.params")

      if (originalParamsFile.notExists()) {
        BazelCoroutineService.getInstance(project).start {
          runBuildTargetTask(buildInfos.values.map { it.id }, project)
        }
        throw ExecutionException(BazelPluginBundle.message("widget.fastbuild.error.params.not.found"))
      }

      withContext(Dispatchers.EDT) {
        writeIntentReadAction {
          FileDocumentManager.getInstance().saveAllDocuments()
        }
      }

      val compileTask = CompilerTask(project, BazelPluginBundle.message("widget.fastbuild.recompile"), false, false, true, false)
      val compileScope = OneProjectItemCompileScope(project, inputFile)
      val compileContext = CompileContextImpl(project, compileTask, compileScope, true, false)
      compileTask.start({
        try {
          val indicator = ProgressManager.getInstance().progressIndicator

          indicator.checkCanceled()

          val tempDir = Files.createTempDirectory("cdb-fast-compile")
          logger.debug("Using '$tempDir' for fast compile")
          val outputJar = tempDir.resolve("output.jar")

          val paramsFile =
            updateAndWriteCompileParams(
              originalParamsFile,
              tempDir,
              outputJar,
              Path.of(inputFile.canonicalPath),
              Path.of(workspaceRoot.toCanonicalPath()),
              targetJar,
            )

          indicator.checkCanceled()

          val toolchainInfo =
            ToolchainInfoSyncHook.JvmToolchainInfoService.getInstance(project).jvmToolchainInfo
              ?: throw ExecutionException(BazelPluginBundle.message("widget.fastbuild.error.null.jvm.toolchain"))

          val arguments =
            buildList {
              add(toolchainInfo.java_home + "/bin/java")
              toolchainInfo.jvm_opts.forEach { add(it) }
              add("-jar")
              add(toolchainInfo.toolchain_path)
              add("@${paramsFile.pathString}")
              add("@${originalParamsFile1.pathString}")
            }

          val command =
            GeneralCommandLine(arguments).apply {
              workDirectory =
                File(
                  BazelBinPathService.getInstance(project).bazelExecPath
                    ?: throw ExecutionException(BazelPluginBundle.message("widget.fastbuild.error.null.bazel.exec.path")),
                )
            }
          val handler = OSProcessHandler(command)
          handler.addProcessListener(
            object : ProcessListener {
              // TODO: adapt EclipseOutputParser and JavacOutputParser and group parse errors properly
              val JAVAC_WARNING = "warning:"
              val JAVAC_ERROR = "error:"
              val ECLIPSE_PROBLEM_SEPARATOR = "----------"
              val ECLIPSE_WARNING = ". WARNING in "
              val ECLIPSE_ERROR = ". ERROR in "

              var isWarning = false

              override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (outputType == ProcessOutputType.STDERR) {
                  if (ECLIPSE_PROBLEM_SEPARATOR in event.text) return
                  if (JAVAC_WARNING in event.text || ECLIPSE_WARNING in event.text) {
                    isWarning = true
                  } else if (JAVAC_ERROR in event.text || ECLIPSE_ERROR in event.text) {
                    isWarning = false
                  }
                  val compilerCategory = if (isWarning) CompilerMessageCategory.WARNING else CompilerMessageCategory.ERROR

                  val parts = event.text.split(':')
                  val line = parts.firstOrNull()?.trim()?.toIntOrNull()
                  if (parts.size >= 4 && line != null) {
                    val error = parts[3]
                    compileContext.addMessage(
                      CompilerMessageImpl(
                        project,
                        compilerCategory,
                        error,
                        inputFile,
                        line,
                        0,
                        null,
                      ),
                    )
                  } else {
                    compileContext.addMessage(
                      CompilerMessageImpl(
                        project,
                        compilerCategory,
                        event.text,
                      ),
                    )
                  }
                } else if (outputType == ProcessOutputType.STDOUT) {
                  compileContext.addMessage(
                    CompilerMessageImpl(
                      project,
                      CompilerMessageCategory.INFORMATION,
                      event.text,
                    ),
                  )
                }
              }
            },
          )
          handler.startNotify()
          if (!handler.waitFor() || handler.exitCode != 0) {
            compileTask.setEndCompilationStamp(ExitStatus.ERRORS, System.currentTimeMillis())
            tempDir.delete()
            return@start
          }
          compileTask.setEndCompilationStamp(ExitStatus.SUCCESS, System.currentTimeMillis())
          processAndHotswapOutput(tempDir, outputJar, project)
        } catch (e: ExecutionException) {
          compileContext.addMessage(
            CompilerMessageImpl(
              project,
              CompilerMessageCategory.ERROR,
              e.message,
            ),
          )
          compileTask.setEndCompilationStamp(ExitStatus.ERRORS, System.currentTimeMillis())
          return@start
        }
      }, null)
    }
  }

  /**
   * Copies the original params file updating certain fields to reduce compile time
   * and point to a different output
   */
  fun updateAndWriteCompileParams(
    originalParams: Path,
    tempDir: Path,
    outputJar: Path,
    inputFile: Path,
    workspaceRoot: Path,
    targetJar: Path,
  ): Path {
    val params = tempDir.resolve("compile.params")
    originalParams.reader().buffered().use { ips ->
      params.writer().use { os ->
        var line = ips.readLine()
        val sourceFile = inputFile.relativeTo(workspaceRoot)

        fun OutputStreamWriter.writeLn(line: String) = write("$line\n")
        while (line != null) {
          when (line) {
            "--output" -> {
              os.writeLn(line)
              os.writeLn(outputJar.pathString)
              ips.readLine()
            }

            "--native_header_output" -> {
              os.writeLn(line)
              os.writeLn(tempDir.resolve("output-native-header.jar").pathString)
              ips.readLine()
            }

            "--generated_sources_output" -> {
              os.writeLn(line)
              os.writeLn(tempDir.resolve("output-generated-sources.jar").pathString)
              ips.readLine()
            }

            "--output_manifest_proto" -> {
              os.writeLn(line)
              os.writeLn(tempDir.resolve("output.jar_manifest_proto").pathString)
              ips.readLine()
            }

            "--output_deps_proto" -> {
              os.writeLn(line)
              os.writeLn(tempDir.resolve("output.jdeps").pathString)
              ips.readLine()
            }

            "--sources" -> {
              os.writeLn(line)
              line = ips.readLine()
              os.writeLn(sourceFile.pathString)
              while (line?.startsWith("--") == false) {
                line = ips.readLine()
              }
              continue
            }

            "--source_jars" -> {
              line = ips.readLine()
              while (line?.startsWith("--") == false) {
                line = ips.readLine()
              }
              continue
            }

            "--strict_java_deps" -> {
              line = ips.readLine()
              while (line?.startsWith("--") == false) {
                line = ips.readLine()
              }
              continue
            }

            else -> {
              os.writeLn(line)
            }
          }
          line = ips.readLine()
        }

        os.writeLn("--classpath")
        // Add the modules jar to the dependencies
        if (targetJar.notExists()) {
          logger.error("Bazel module jar not found: $targetJar")
        }
        os.writeLn(targetJar.pathString)
      }
    }
    return params
  }

  /**
   * Unzips the jar and uses the files within to hotswap
   */
  fun processAndHotswapOutput(
    tempDir: Path,
    outputJar: Path,
    project: Project,
  ) {
    ProgressManager.getInstance().run(
      object : Task.Backgroundable(project, BazelPluginBundle.message("widget.fastbuild.hotswaping"), true) {
        override fun run(indicator: ProgressIndicator) {
          val unzip = tempDir.resolve("unzip").also { it.toFile().mkdir() }
          val files =
            buildList<String> {
              ZipFile(outputJar.toFile()).use { zipFile ->
                zipFile.entries().iterator().forEach {
                  if (it.isDirectory || !it.name.endsWith(".class")) {
                    return@forEach
                  }
                  val output = unzip.resolve(it.name)
                  output.parent.toFile().mkdirs()
                  zipFile.getInputStream(it).copyTo(output.outputStream())
                  add(it.name)
                }
              }
            }.associate {
              it.replace(File.separatorChar, '.').let {
                if (it.endsWith(".class")) {
                  return@let it.substringBeforeLast('.')
                }
                it
              } to HotSwapFile(unzip.resolve(it).toFile())
            }

          indicator.checkCanceled()

          hotswapFile(project, files, tempDir.toFile())
        }
      },
    )
  }

  fun hotswapFile(
    project: Project,
    hotswapMap: Map<String, HotSwapFile>,
    tempDir: File,
  ) {
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
          tempDir.delete()
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
        NotificationGroupManager
          .getInstance()
          .getNotificationGroup("HotSwap Messages")
          .createNotification(
            BazelPluginBundle.message("widget.fastbuild.completed.title"),
            BazelPluginBundle.message("widget.fastbuild.completed.description"),
            NotificationType.INFORMATION,
          ).setImportant(false)
          .setIcon(AllIcons.Status.Success)
          .notify(project)
      }
    } catch (e: Exception) {
      logger.warn("Failed to trigger hotswap completed notification", e)
    }
  }

  private fun getSessionsToHotswap(project: Project): Collection<DebuggerSession> {
    val sessions = getDebugSessions(project)
    if (sessions.isEmpty()) return sessions
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

  private fun getDebugSessions(project: Project) =
    DebuggerManagerEx
      .getInstanceEx(project)
      .sessions
      .stream()
      .filter { debuggerSession: DebuggerSession? -> HotSwapUIImpl.canHotSwap(debuggerSession!!) }
      .toList()
}
