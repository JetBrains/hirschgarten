/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.hotswap

import com.google.common.collect.ImmutableMap
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.JavaDebuggerBundle
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.impl.HotSwapFile
import com.intellij.debugger.impl.HotSwapManager
import com.intellij.debugger.impl.HotSwapProgress
import com.intellij.debugger.ui.HotSwapProgressImpl
import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.MessageCategory
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.plugins.bsp.runnerAction.BspJvmApplicationConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.function.Consumer
import java.util.jar.JarFile

/** Manages hotswapping for blaze java_binary run configurations.  */
object BazelHotSwapManager {
  private val logger = Logger.getInstance(BazelHotSwapManager::class.java)

  fun reloadChangedClasses(project: Project) {
    val session = createHotSwappableDebugSession(project)
    if (session == null) {
      return
    }
    val progress = HotSwapProgressImpl(project)
    ApplicationManager
      .getApplication()
      .executeOnPooledThread {
        progress.setSessionForActions(session.session)
        ProgressManager
          .getInstance()
          .runProcess(
            Runnable {
              doReloadClasses(
                session,
                progress,
              )
            },
            progress.progressIndicator,
          )
      }
  }

  private fun doReloadClasses(session: HotSwappableDebugSession, progress: HotSwapProgress) {
    try {
      progress.setDebuggerSession(session.session)
      progress.setText("Building .class file manifest")
      val manifestDiff: ClassFileManifest.Diff? =
        ClassFileManifestBuilder.buildManifest(session.env, progress)
      if (manifestDiff == null) {
        progress.addMessage(
          session.session,
          MessageCategory.ERROR,
          "Modified classes could not be determined.",
        )
        return
      }
      val localFiles = copyClassFilesLocally(manifestDiff)
      val files =
        localFiles.mapValues { entry -> HotSwapFile(entry.value) }

      if (!files.isEmpty()) {
        progress.setText("Reloading ${files.size} \".class\" file(s)")
      }
      try {
        HotSwapManager.reloadModifiedClasses(
          mapOf<DebuggerSession, Map<String, HotSwapFile>>(session.session to files),
          progress,
        )
        progress.addMessage(
          session.session,
          MessageCategory.INFORMATION,
          "Reloaded ${files.size} \".class\" file(s)",
        )
      } finally {
        localFiles.values.forEach(Consumer { obj: File? -> obj?.delete() })
      }
    } catch (e: Throwable) {
      processException(e, session.session, progress)
      if (e.message != null) {
        progress.addMessage(session.session, MessageCategory.ERROR, e.message)
      }
    } finally {
      progress.finished()
    }
  }

  /**
   * Given a per-jar map of .class files, extracts them locally and returns a map from qualified
   * class name to File, suitable for [HotSwapManager].
   *
   */
  private fun copyClassFilesLocally(manifestDiff: ClassFileManifest.Diff): Map<String, File> {
    val tempDir = File(System.getProperty("java.io.tmpdir"))
    val suffix: String = UUID.randomUUID().toString().substring(0, 8)
    val localDir = File(tempDir, "class_files_" + suffix)
    if (!localDir.mkdir()) {
      throw ExecutionException(
        String.format("Cannot create temp output directory '%s'", localDir.getPath()),
      )
    }
    localDir.deleteOnExit()

    val map = HashMap<String, File>()
    for (jar in manifestDiff.perJarModifiedClasses.keySet()) {
      val classes: MutableCollection<String> = manifestDiff.perJarModifiedClasses.get(jar)
      map.putAll(
        copyClassFilesLocally(
          localDir,
          jar,
          classes,
        ),
      )
    }
    return ImmutableMap.copyOf<String?, File?>(map)
  }

  private fun copyClassFilesLocally(
    destination: File,
    jar: File,
    classes: Collection<String>,
  ): Map<String, File> {
    val map = mutableMapOf<String, File>()
    try {
      val jarFile = JarFile(jar)
      for (path in classes) {
        val entry = jarFile.getJarEntry(path)
        if (entry == null) {
          throw ExecutionException(
            String.format("Couldn't find class file %s inside jar %s.", path, jar),
          )
        }
        val f = File(destination, path.replace('/', '-'))
        f.deleteOnExit()
        jarFile.getInputStream(entry).use { inputStream ->
          Files.copy(inputStream, f.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        map.put(deriveQualifiedClassName(path), f)
      }
    } catch (e: IOException) {
      throw ExecutionException("Error reading runtime jars", e)
    } catch (e: IllegalStateException) {
      throw ExecutionException("Error reading runtime jars", e)
    }
    return map
  }

  /** Derive the fully-qualified class name from the path inside the jar.  */
  private fun deriveQualifiedClassName(path: String): String = path.substring(0, path.length - ".class".length).replace('/', '.')

  fun createHotSwappableDebugSession(project: Project): HotSwappableDebugSession? {
    val debuggerManager = DebuggerManagerEx.getInstanceEx(project)
    val session = debuggerManager.context.debuggerSession
    if (session == null || !session.isAttached) {
      return null
    }
    val process = session.process.xdebugProcess
    if (process == null) {
      return null
    }
    val env = (process.session as? XDebugSessionImpl)?.executionEnvironment ?: return null
    if (!HotSwapUtils.canHotSwap(env)) return null
    val runProfile = env.runProfile as? BspJvmApplicationConfiguration ?: return null
    // build manifest if not exists
    return HotSwappableDebugSession(
      session,
      env,
      runProfile,
    )
  }

  private fun processException(
    e: Throwable,
    session: DebuggerSession?,
    progress: HotSwapProgress,
  ) {
    if (e.message != null) {
      progress.addMessage(session, MessageCategory.ERROR, e.message)
    }
    if (e is ProcessCanceledException) {
      progress.addMessage(
        session,
        MessageCategory.INFORMATION,
        JavaDebuggerBundle.message("error.operation.canceled"),
      )
    } else {
      logger.warn(e)
      progress.addMessage(session, MessageCategory.ERROR, "Error reloading classes")
    }
  }

  data class HotSwappableDebugSession(
    val session: DebuggerSession,
    val env: ExecutionEnvironment,
    val runConfig: BspJvmApplicationConfiguration,
  )
}
