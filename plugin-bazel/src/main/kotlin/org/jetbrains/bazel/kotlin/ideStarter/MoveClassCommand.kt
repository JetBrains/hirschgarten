package org.jetbrains.bazel.kotlin.ideStarter

import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.refactoring.move.MoveHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.performanceImpl.resolveFromRelativeOrRoot
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import java.util.concurrent.CountDownLatch
import kotlin.collections.component1
import kotlin.collections.component2

internal class MoveClassCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "moveClass"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val (sourceFilePath, destinationDirectoryPath) =
      try {
        extractCommandArgument(PREFIX).split(" ")
      } catch (_: Exception) {
        throw IllegalArgumentException("Usage: $PREFIX sourceFile destinationDirectory")
      }
    val project = context.project
    val rootDir = project.rootDir
    val sourceFile = checkNotNull(rootDir.resolveFromRelativeOrRoot(sourceFilePath))
    val destinationDirectory = checkNotNull(rootDir.resolveFromRelativeOrRoot(destinationDirectoryPath))

    val targetDirectory =
      readAction {
        checkNotNull(PsiManager.getInstance(project).findDirectory(destinationDirectory))
      }
    val sourceClass =
      readAction {
        val sourcePsiFile = PsiManager.getInstance(project).findFile(sourceFile) as PsiClassOwner
        var sourceClass: PsiElement = sourcePsiFile.classes.single()
        if (sourceClass is KtLightClass) {
          sourceClass = checkNotNull(sourceClass.kotlinOrigin)
        }
        sourceClass
      }

    withContext(Dispatchers.EDT) {
      MoveHandler.doMove(
        project,
        arrayOf(sourceClass),
        targetDirectory,
        SimpleDataContext.getProjectContext(project),
        null,
      )
    }
  }
}
