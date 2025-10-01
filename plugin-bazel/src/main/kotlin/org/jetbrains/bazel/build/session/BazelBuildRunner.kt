package org.jetbrains.bazel.build.session

import com.intellij.build.BuildViewManager
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import java.nio.charset.StandardCharsets

/**
 * Starts a Bazel build as a local OS process and streams its output through the BazelOutputPipeline
 * into the Build tool window via BuildEvents. BSP is not used anywhere.
 */
class BazelBuildRunner(private val project: Project) {

  private val log = logger<BazelBuildRunner>()

  /**
   * Run `bazel build <targets>` and invoke [onFinished] with the process exit code.
   */
  fun build(targets: List<Label>, onFinished: (exitCode: Int) -> Unit) {
    val displayTargets = targets.joinToString(" ") { it.toString() }
    val title = "Bazel build $displayTargets"

    val buildView = project.getService(BuildViewManager::class.java)

    // Use negative timestamp to force newest builds at top (IntelliJ sorts by build ID/timestamp)
    val buildId = -System.currentTimeMillis()

    val descriptor = com.intellij.build.DefaultBuildDescriptor(
      buildId,
      BazelPluginConstants.SYSTEM_ID,
      title,
      project.rootDir.path,
      System.currentTimeMillis()
    )
    // Improve UX: navigate to the first error automatically and activate tool window
    descriptor.setNavigateToError(com.intellij.util.ThreeState.YES)
    descriptor.setActivateToolWindowWhenAdded(true)

    // Add restart action so users can re-run the build
    descriptor.withRestartAction(object : AnAction() {
      override fun actionPerformed(e: AnActionEvent) {
        build(targets, onFinished)
      }
    })

    val session = BazelBuildSession(buildView, descriptor)
    // Use context-aware parsers from Google plugin for better issue detection
    val workspaceRoot = project.rootDir.toNioPathOrNull()?.toFile()
      ?: error("Cannot resolve workspace root to file")
    val parsers = org.jetbrains.bazel.build.BazelOutputParserProvider()
      .getBuildOutputParsersWithContext(project, workspaceRoot)
    val pipeline = BazelOutputPipeline(session, parsers)

    // Optional BEP text file tailer for target grouping
    val useBep = com.intellij.openapi.util.registry.Registry.get("bazel.buildEvents.bep.enabled").asBoolean()
    val bepFile = if (useBep) kotlin.io.path.createTempFile("bazel-bep-", ".jsonl").toFile() else null
    val tailer = if (bepFile != null) org.jetbrains.bazel.build.bep.BazelBepTextTailer(bepFile, session) else null

    val cmd = buildCommand(targets, bepFile)

    val handler: OSProcessHandler = try {
      OSProcessHandler(cmd)
    } catch (t: Throwable) {
      // Start the build node so we can report failures like missing Bazel binary
      session.start()
      // Echo the exact command line for transparency
      session.acceptText(session.currentParentId(), cmd.commandLineString + "\n")
      // Process could not be created, likely due to missing Bazel binary
      log.warn("Failed to start Bazel process", t)
      val errorEvent = com.intellij.build.events.BuildEvents.getInstance().message()
        .withParentId(session.id())
        .withKind(com.intellij.build.events.MessageEvent.Kind.ERROR)
        .withMessage("Failed to start Bazel")
        .withDescription(buildString {
          appendLine("Could not start Bazel process.")
          appendLine(t.message ?: t.toString())
          appendLine()
          appendLine("Configure bazel_binary in your Project View (WorkspaceContext) or ensure 'bazel' is on PATH.")
        })
        .build()
      session.accept(errorEvent)
      session.finish(exitCode = 1)
      onFinished(1)
      return
    }

    // Attach process handler to the Build descriptor so the Build View can control/stop the process
    descriptor.withProcessHandler(BazelBuildProcessHandlerWrapper(handler), null)

    // Start the build node now that the descriptor is fully configured
    session.start()
    // Echo the exact command line for transparency
    session.acceptText(session.currentParentId(), cmd.commandLineString + "\n")

    val splitter = LineSplitter { line -> pipeline.onLine(line) }

    handler.addProcessListener(object : ProcessAdapter() {
      override fun startNotified(event: ProcessEvent) {
        // tailer now that process actually started
        tailer?.start()
      }

      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        splitter.append(event.text)
      }

      override fun processTerminated(event: ProcessEvent) {
        // Flush any remaining partial line
        splitter.flush()
        tailer?.stop()
        // best-effort delete temp file
        try { bepFile?.delete() } catch (_: Throwable) {}
        session.finish(event.exitCode)
        onFinished(event.exitCode)
      }
    })

    handler.setShouldDestroyProcessRecursively(true)
    handler.startNotify()
  }

  private fun buildCommand(targets: List<Label>, bepFile: java.io.File?): com.intellij.execution.configurations.GeneralCommandLine {
    val args = mutableListOf<String>()
    args += resolveBazelBinary()
    args += "build"
    args += listOf(
      "--color=yes",
      "--curses=no",
      "--isatty=0",
      "--ui_event_filters=-info,-debug",
      "--show_progress_rate_limit=0"
    )
    if (bepFile != null) {
      args += "--build_event_json_file=${bepFile.absolutePath}"
      args += "--build_event_publish_all_actions"
    }
    args += targets.map { it.toString() }

    return com.intellij.execution.configurations.GeneralCommandLine(args)
      .withCharset(StandardCharsets.UTF_8)
      .withWorkDirectory(project.rootDir.path)
  }

  private fun resolveBazelBinary(): String {
    return try {
      val provider = org.jetbrains.bazel.languages.projectview.ProjectViewWorkspaceContextProvider.getInstance(project)
      val ctx = provider.readWorkspaceContext()
      val path = ctx.bazelBinary?.toFile()
      if (path != null && path.exists()) path.absolutePath else "bazel"
    } catch (t: Throwable) {
      // In case project view is not available, fall back to PATH
      log.debug("Failed to resolve bazel binary from WorkspaceContext, falling back to PATH", t)
      "bazel"
    }
  }
}

/**
 * Simple line splitter that collects text chunks and invokes [onLine] for each complete line.
 */
private class LineSplitter(private val onLine: (String) -> Unit) {
  private val buf = StringBuilder()

  fun append(chunk: String) {
    buf.append(chunk)
    var idx: Int
    while (true) {
      idx = buf.indexOf("\n")
      if (idx < 0) break
      val line = buf.substring(0, idx)
      // Drop optional preceding \r
      val clean = if (line.endsWith("\r")) line.dropLast(1) else line
      onLine(clean)
      buf.delete(0, idx + 1)
    }
  }

  fun flush() {
    if (buf.isNotEmpty()) {
      onLine(buf.toString())
      buf.setLength(0)
    }
  }
}
