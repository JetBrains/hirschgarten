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
import com.intellij.debugger.ui.HotSwapProgressImpl
import com.intellij.debugger.ui.HotSwapStatusListener
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.xdebugger.impl.hotswap.HotSwapStatusNotificationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

/** Manages hotswapping for bazel java_binary run configurations.  */
@Service(Service.Level.PROJECT)
class BazelHotSwapManager(private val project: Project, private val coroutineScope: CoroutineScope) {
  @OptIn(ExperimentalPathApi::class)
  suspend fun hotswapImpl(
    oldManifest: JarFileManifest,
    newManifest: JarFileManifest,
    listener: HotSwapStatusListener?,
    sessions: List<DebuggerSession>,
    isAutoRun: Boolean,
  ) {
    if (sessions.isEmpty()) return

    val tempRoot = Path.of(PathManager.getTempPath())
    val tempDir = createTempDirectory(tempRoot, "class_files_")
    val diff = JarFileManifest.diffJarManifests(oldManifest = oldManifest, newManifest = newManifest)
    val localClassFiles = copyClassFilesLocally(tempDir, diff)
    val hotSwapClassFiles = localClassFiles.mapValues { entry -> HotSwapFile(entry.value.toFile()) }

    LOG.info(
      "hotswap: found ${diff.perJarModifiedFiles.size()} jar files with changed entries after compilation\n" +
      diff.perJarModifiedFiles.entrySet()
        .map { "  ${it.key}: ${it.value.sorted().joinToString(" ")}" }
        .sorted()
        .joinToString("\n"),
    )
    if (hotSwapClassFiles.isNotEmpty()) {
      val progress = withContext(Dispatchers.EDT) {
        HotSwapProgressImpl(project)
      }

      try {
        ProgressManager.getInstance().runProcess(
          {
            LOG.info("hotswap: redefining ${hotSwapClassFiles.size} classes: " + hotSwapClassFiles.keys.joinToString(" "))
            HotSwapManager.reloadModifiedClasses(
              sessions.associateWith { hotSwapClassFiles },
              progress,
            )

            runBlockingCancellable {
              HotSwapHook.EP_NAME.extensionList.forEach { hotSwapHook ->
                hotSwapHook.onHotSwap(sessions)
              }
            }

            listener?.onSuccess(sessions)
          },
          progress.progressIndicator,
        )
      }
      catch (e: CancellationException) {
        listener?.onCancel(sessions)
        throw e
      }
      catch (e: Exception) {
        listener?.onFailure(sessions)
        LOG.error("Unable to hotswap", e)
      }
      finally {
        progress.finished()
        withContext(Dispatchers.EDT) {
          Disposer.dispose(progress.progressIndicator as Disposable)
        }
        tempDir.deleteRecursively()
      }
    }
    else {
      if (!isAutoRun) {
        val message = JavaDebuggerBundle.message("status.hotswap.uptodate")
        val notification = NotificationGroupManager.getInstance().getNotificationGroup("HotSwap Messages")
          .createNotification(message, NotificationType.INFORMATION)
        HotSwapStatusNotificationManager.getInstance(project).trackNotification(notification)
        notification.notify(project)
      }
      listener?.onNothingToReload(sessions)
    }
  }

  fun getCurrentDebugSessions(): List<DebuggerSession> {
    return DebuggerManagerEx.getInstanceEx(project)
      .sessions
      .filter { it.isAttached && it.process.canRedefineClasses() }
  }

  /**
   * Given a per-jar map of .class files, extracts them locally and returns a map from qualified
   * class name to File, suitable for [HotSwapManager].
   */
  private fun copyClassFilesLocally(tempDir: Path, manifestDiff: JarFileManifest.Diff): Map<String, Path> {
    val map = HashMap<String, Path>()
    for (jar in manifestDiff.perJarModifiedFiles.keySet()) {
      val classes = manifestDiff.perJarModifiedFiles.get(jar)
        .filter { it.endsWith(".class") }
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
    JarFile(jar.toFile()).use { jarFile ->
      for (path in classes) {
        val entry = jarFile.getJarEntry(path)
                    ?: error("")
        val f = destination.resolve(path.replace('/', '-'))
        jarFile.getInputStream(entry).use { inputStream ->
          Files.copy(inputStream, f, StandardCopyOption.REPLACE_EXISTING)
        }
        map[deriveQualifiedClassName(path)] = f
      }
    }
    return map
  }

  /** Derive the fully qualified class name from the path inside the jar.  */
  private fun deriveQualifiedClassName(path: String): String =
    path.removeSuffix(".class").replace('/', '.')

  companion object {
    fun getInstance(project: Project): BazelHotSwapManager = project.service<BazelHotSwapManager>()
  }
}

private val LOG = logger<BazelHotSwapManager>()
