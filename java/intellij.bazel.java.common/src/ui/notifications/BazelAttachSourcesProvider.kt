package org.jetbrains.bazel.ui.notifications

import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.LibrarySourceRootDetectorUtil
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.ThrowableRunnable
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject

/**
 * See https://github.com/bazelbuild/bazel/issues/10692
 */
internal class BazelAttachSourcesProvider() : AttachSourcesProvider {
  private class BazelAttachSourcesAction() : AttachSourcesProvider.AttachSourcesAction {
    override fun getName(): String = BazelPluginBundle.message("sources.attach.action.text")

    override fun getBusyText(): String = BazelPluginBundle.message("sources.pending.text")

    override fun perform(orderEntries: List<LibraryOrderEntry>): ActionCallback =
      ActionCallback().apply {
        ApplicationManager.getApplication().executeOnPooledThread {
          try {
            val libraries = orderEntries.mapNotNull { it.library }.distinct()
            val (libsWithEmptySources, libsWithSources) = libraries.partition { library ->
              library.getFiles(OrderRootType.SOURCES).isEmpty()
            }
            if (libsWithEmptySources.isNotEmpty()) {
              val missingSources = libsWithEmptySources.joinToString(",\n") { it.name.orEmpty() }
              showError(missingSources)
            }
            val modelsToCommit = mutableListOf<Pair<Library.ModifiableModel, Array<out VirtualFile>>>()
            libsWithSources.forEach { library ->
              val availableSources = library.getFiles(OrderRootType.SOURCES)
              library.obtainModelWithAddedSources(availableSources)?.let {
                modelsToCommit.add(it)
              }
            }
            if (modelsToCommit.isNotEmpty()) {
              WriteAction.run(
                ThrowableRunnable {
                  modelsToCommit.forEach { (model, roots) ->
                    roots.forEach {
                      model.addRoot(it.url, OrderRootType.SOURCES)
                    }
                    model.commit()
                  }
                  setDone()
                },
              )
            } else {
              setDone()
            }
          } catch (e: Exception) {
            reject(e.message)
          }
        }
      }


    private fun showError(target: String) {
      BazelBalloonNotifier.error(
        BazelPluginBundle.message("sources.files.not.resolved"),
        BazelPluginBundle.message("error.message.failed.to.resolve.sources.0", target),
      )
    }

    private fun Library.obtainModelWithAddedSources(availableSources: Array<VirtualFile>): Pair<Library.ModifiableModel, Array<out VirtualFile>>? {
      val sourceRoots =
        invokeAndWaitIfNeeded {
          LibrarySourceRootDetectorUtil.scanAndSelectDetectedJavaSourceRoots(null, availableSources)
        }
      if (sourceRoots.isEmpty()) return null
      return Pair(modifiableModel, sourceRoots)
    }
  }

  override fun getActions(
    orderEntries: MutableList<out LibraryOrderEntry>,
    psiFile: PsiFile,
  ): List<AttachSourcesProvider.AttachSourcesAction> {
    val project = orderEntries.firstNotNullOf { it.ownerModule.project }
    return if (project.isBazelProject && containsBazelSourcesForEntries(orderEntries)) {
      listOf(BazelAttachSourcesAction())
    } else {
      emptyList()
    }
  }

  private fun containsBazelSourcesForEntries(orderEntries: List<LibraryOrderEntry>): Boolean =
    orderEntries.any {
      it.library?.getFiles(OrderRootType.SOURCES)?.isNotEmpty() == true
    }
}
