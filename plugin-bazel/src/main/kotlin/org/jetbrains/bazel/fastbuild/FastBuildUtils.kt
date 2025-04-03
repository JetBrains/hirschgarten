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
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.compiler.CompilerMessageCategory
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.toCanonicalPath
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.task.TaskRunnerResults
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.target.TargetUtils
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

  suspend fun fastBuildFiles(project: Project, files: Array<VirtualFile>) {
    val workspaceRoot = project.rootDir.toNioPath()

    val targetUtils = project.service<TargetUtils>()
    val virtualFileManager = VirtualFileManager.getInstance()
    val buildInfos = files
      .mapNotNull { file ->
        targetUtils.getBuildTargetInfoForLabel(
          targetUtils.getTargetsForFile(file).first()
        )?.let {
          file to it
        }
      }.toMap()

    if (buildInfos.isEmpty()) {
      logger.info("Unable to determine build targets for files, falling back to native compile")
      TODO()
    }
    for (entry in buildInfos) {
      val inputFile = entry.key
      val filePath = inputFile.path
      val canonicalFilePath = inputFile.canonicalPath
//      span.setAttribute("file", filePath)
//      span.setAttribute("file.canonical", canonicalFilePath + "")

      val workspacePath = workspaceRoot.pathString
      val canonicalWorkspaceRootPath = workspaceRoot.toCanonicalPath()
//      span.setAttribute("workspaceRoot", workspacePath)
//      span.setAttribute("workspaceRoot.canonical", canonicalWorkspaceRootPath)
      if (!(canonicalFilePath?.startsWith(canonicalWorkspaceRootPath) ?: false)) {
        logger.warn(String.format("Path %s is not under workspace root %s or bazel folder is null",
          filePath, workspacePath
        ))
        NotificationGroupManager.getInstance().getNotificationGroup("HotSwap Messages").createNotification(
          "Hotswap failed",
          String.format("Hotswap failed with errors: %s is not under workspace root %s.", canonicalFilePath, canonicalWorkspaceRootPath),
          NotificationType.ERROR
        ).setImportant(true).notify(project)

//        span.addEvent("path is not under workspace root or bazel folder is null")
//          .setStatus(StatusCode.ERROR)
//          .end()
        continue
      }
      val relativePath = entry.value.baseDirectory?.let {
        virtualFileManager.findFileByUrl(it)?.toNioPath()?.relativeTo(workspaceRoot)
      } ?: continue
      val isLib = targetUtils.isLibrary(entry.value.id)
      val targetJar = workspaceRoot.resolve("bazel-bin/${relativePath}/${if (isLib) "lib" else ""}${entry.value.id.targetName}.jar")

      val originalParamsFile = targetJar.parent.resolve(targetJar.fileName.name + "-0.params")
      val originalParamsFile1 = targetJar.parent.resolve(targetJar.fileName.name + "-1.params")

      if (originalParamsFile.notExists()) {
        logger.info("Params file not found for fast compile. Delegating to bazel build")
//        span.setAttribute("fast-build", false)
        //Without the params file, we have to fall back to the original hotswap mechanism via bazel build
        //This will generate the params file for later use
        //TODO multiple files
//        bazelServer.nativeBuildAndHotswap(project, context, span, *tasks).onProcessed { promise.setResult(it) }
        TODO()
      }
//      span.setAttribute("fast-build", true)

      runInEdt {
        FileDocumentManager.getInstance().saveAllDocuments()
      }


      val compileTask = CompilerTask(project, "Recompile", false, false, true, false)
      val compileScope = OneProjectItemCompileScope(project, inputFile)
      val compileContext = CompileContextImpl(project, compileTask, compileScope, true, false)
      compileTask.start({
        val indicator = ProgressManager.getInstance().progressIndicator

        indicator.checkCanceled()

        val tempDir = Files.createTempDirectory("cdb-fast-compile")
        logger.debug("Using '$tempDir' for fast compile")
        val outputJar = tempDir.resolve("output.jar")

        val paramsFile = updateAndWriteCompileParams(
          originalParamsFile,
          tempDir,
          outputJar,
          Path.of(inputFile.canonicalPath),
          Path.of(workspaceRoot.toCanonicalPath()),
          targetJar
        )

        indicator.checkCanceled()

        val toolchainInfo = ToolchainInfoSyncHook.JvmToolchainInfoService.getInstance(project).jvmToolchainInfo ?: return@start

        val arguments = buildList {
          add(toolchainInfo.java_home + "/bin/java")
          toolchainInfo.jvm_opts.forEach { add(it) }
          add("-jar")
          add(toolchainInfo.toolchain_path)
          add("@${paramsFile.pathString}")
          add("@${originalParamsFile1.pathString}")
        }
        val command = GeneralCommandLine(arguments).apply {
          workDirectory = info.executionRoot.toFile() //TODO
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
                      inputFile,
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
          span.addEvent("fast compile")
          handler.startNotify()
          if (!handler.waitFor() || handler.exitCode != 0) {
            compileTask.setEndCompilationStamp(ExitStatus.ERRORS, System.currentTimeMillis())
            if (Registry.`is`(CompileUtils.cleanupKey)) {
              tempDir
            }
            span.addEvent("fast compile failed")
            span.end()
            return@start
          }
        } catch (e: ExecutionException) {
          span.recordException(e)
          span.setStatus(StatusCode.ERROR)
          span.end()

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

        processAndHotswapOutput(tempDir, outputJar, project, span)
        promise.setResult(TaskRunnerResults.SUCCESS)
      }, null)
    }
  }

  /**
   * Copies the original params file updating certain fields to reduce compile time
   * and point to a different output
   */
  fun updateAndWriteCompileParams(
    originalParams: Path, tempDir: Path, outputJar: Path, inputFile: Path,
    workspaceRoot: Path, targetJar: Path
  ): Path {
    val params = tempDir.resolve("compile.params")
    originalParams.reader().buffered().use { ips ->
      params.writer().use { os ->
        var line = ips.readLine()
        val sourceFile = inputFile.relativeTo(workspaceRoot)
        while (line != null) {
          fun OutputStreamWriter.writeLn(line: String) = write("$line\n")
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
              os.writeLn(tempDir.resolve("output.jdpes").pathString)
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

            "--direct_dependencies" -> {
              os.writeLn(line)
              line = ips.readLine()
              while (line?.startsWith("--") == false) {
                os.write("$line\n")
                line = ips.readLine()
              }
              //Add the modules jar to the dependencies
              if (targetJar.notExists()) {
                logger.error("Bazel module jar not found: $targetJar")
              }
              os.writeLn(targetJar.pathString)
              continue
            }

            else -> {
              os.writeLn(line)
            }
          }
          line = ips.readLine()
        }
      }
    }
    return params
  }

  /**
   * Unzips the jar and uses the files within to hotswap
   */
  fun processAndHotswapOutput(tempDir: Path, outputJar: Path, project: Project, span: Span) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Hotswapping", true) {
      override fun run(indicator: ProgressIndicator) {
        span.addEvent("unzipping jar")
        val unzip = tempDir.resolve("unzip").also { it.toFile().mkdir() }
        val files = buildList<String> {
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
        span.end()

        hotswapFile(project, files, tempDir.toFile(), span)
      }
    })

  }

  fun hotswapFile(project: Project, hotswapMap: Map<String, HotSwapFile>, tempDir: File, span: Span) {
    val hotswapSpan = Tracing.getSpanBuilder("hotswapping", project)
      .setParent(Context.current().with(span))
      .startSpan()
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
          triggerHotswapSuccessNotification(project, progress, hotswapSpan)
          progress.finished()
          if (Registry.`is`(cleanupKey)) {
            tempDir.delete()
          }
          hotswapSpan.end()
        }
      }
    }
  }

  private fun triggerHotswapSuccessNotification(project: Project, progress: HotSwapProgressImpl, span: Span) {
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
      span.recordException(e)
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
