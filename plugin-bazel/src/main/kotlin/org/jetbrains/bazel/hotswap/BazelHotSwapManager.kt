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
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.io.delete
import com.intellij.util.ui.MessageCategory
import com.intellij.xdebugger.impl.XDebugSessionImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BazelHotSwapBundle
import org.jetbrains.bazel.config.BazelPluginBundle
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import kotlin.io.path.createTempDirectory

/** Manages hotswapping for bazel java_binary run configurations.  */
object BazelHotSwapManager {
  private val logger = Logger.getInstance(BazelHotSwapManager::class.java)

  fun reloadChangedClasses(project: Project) {
    val session = createHotSwappableDebugSession(project)
    if (session == null) {
      return
    }
    ApplicationManager
      .getApplication()
      .executeOnPooledThread {
        /**
         * [HotSwapProgressImpl] requires EDT for initializing
         */
        val progress =
          runBlocking {
            withContext(Dispatchers.EDT) { HotSwapProgressImpl(project) }
          }
        progress.setSessionForActions(session.session)
        ProgressManager
          .getInstance()
          .runProcess(
            {
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
      progress.setText(BazelHotSwapBundle.message("hotswap.text.manifest.build.in.progress"))
      val manifestDiff: ClassFileManifest.Diff? =
        ClassFileManifestBuilder.buildManifest(session, progress)
      if (manifestDiff == null) {
        progress.addMessage(
          session.session,
          MessageCategory.ERROR,
          BazelHotSwapBundle.message("hotswap.message.manifest.build.error"),
        )
        return
      }
      val localFiles = copyClassFilesLocally(manifestDiff)
      val files =
        localFiles.mapValues { entry -> HotSwapFile(entry.value.toFile()) }

      if (!files.isEmpty()) {
        progress.setText(BazelHotSwapBundle.message("hotswap.text.reload.in.progress", files.size))
        try {
          HotSwapManager.reloadModifiedClasses(
            mapOf(session.session to files),
            progress,
          )
          progress.addMessage(
            session.session,
            MessageCategory.INFORMATION,
            BazelHotSwapBundle.message("hotswap.message.reload.success", files.size),
          )
        } finally {
          localFiles.values.forEach { obj -> obj.delete() }
        }
      } else {
        progress.addMessage(
          session.session,
          MessageCategory.INFORMATION,
          BazelHotSwapBundle.message("hotswap.message.reload.no.changes"),
        )
      }
    } catch (e: Throwable) {
      processException(e, session.session, progress)
    } finally {
      progress.finished()
    }
  }

  /**
   * Given a per-jar map of .class files, extracts them locally and returns a map from qualified
   * class name to File, suitable for [HotSwapManager].
   *
   */
  private fun copyClassFilesLocally(manifestDiff: ClassFileManifest.Diff): Map<String, Path> {
    val tempDir = createTempDirectory("class_files_")
    tempDir.toFile().deleteOnExit()

    val map = HashMap<String, Path>()
    for (jar in manifestDiff.perJarModifiedClasses.keySet()) {
      val classes = manifestDiff.perJarModifiedClasses.get(jar)
      map.putAll(
        copyClassFilesLocally(
          tempDir,
          jar,
          classes,
        ),
      )
    }
    return map
  }

  private fun copyClassFilesLocally(
    destination: Path,
    jar: Path,
    classes: Collection<String>,
  ): Map<String, Path> {
    val map = mutableMapOf<String, Path>()
    try {
      val jarFile = JarFile(jar.toFile())
      for (path in classes) {
        val entry = jarFile.getJarEntry(path)
        if (entry == null) {
          throw ExecutionException(BazelPluginBundle.message("hotswap.missing.file.inside.jar", path, jar))
        }
        val f = destination.resolve(path.replace('/', '-'))
        f.toFile().deleteOnExit()
        jarFile.getInputStream(entry).use { inputStream ->
          Files.copy(inputStream, f, StandardCopyOption.REPLACE_EXISTING)
        }
        map.put(deriveQualifiedClassName(path), f)
      }
    } catch (e: IOException) {
      throw ExecutionException(BazelPluginBundle.message("hotswap.error.reading.jars"), e)
    } catch (e: IllegalStateException) {
      throw ExecutionException(BazelPluginBundle.message("hotswap.error.reading.jars"), e)
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
    if (!HotSwapUtils.canHotSwap(env, project)) return null

    return HotSwappableDebugSession(
      session,
      env,
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
      progress.addMessage(session, MessageCategory.ERROR, BazelHotSwapBundle.message("hotswap.message.reload.error"))
    }
  }

  data class HotSwappableDebugSession(val session: DebuggerSession, val env: ExecutionEnvironment)
}
