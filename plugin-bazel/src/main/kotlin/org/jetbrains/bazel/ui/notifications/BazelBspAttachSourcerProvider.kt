package org.jetbrains.bazel.ui.notifications

import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.LibrarySourceRootDetectorUtil
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier

/**
 * See https://github.com/bazelbuild/bazel/issues/10692
 */
internal class BazelAttachSourcesProvider : AttachSourcesProvider {
  private class BazelAttachSourcesAction : AttachSourcesProvider.AttachSourcesAction {
    override fun getName(): String = BazelPluginBundle.message("sources.attach.action.text")

    override fun getBusyText(): String = BazelPluginBundle.message("sources.pending.text")

    override fun perform(orderEntries: List<LibraryOrderEntry>): ActionCallback {
      val libraries = orderEntries.mapNotNull { it.library }.distinct()
      val modelsToCommit =
        libraries.mapNotNull { library ->
          val availableSources = library.getFiles(OrderRootType.SOURCES)
          if (availableSources.isEmpty()) {
            showError(library.name.orEmpty())
            null
          } else {
            library.obtainModelWithAddedSources(availableSources)
          }
        }
      if (modelsToCommit.isNotEmpty()) {
        WriteAction.run<Exception> {
          modelsToCommit.forEach {
            it.commit()
          }
        }
      }
      return ActionCallback.DONE
    }

    private fun showError(target: String) {
      BspBalloonNotifier.error(
        BazelPluginBundle.message("sources.files.not.resolved"),
        BazelPluginBundle.message("error.message.failed.to.resolve.sources.0", target),
      )
    }

    private fun Library.obtainModelWithAddedSources(availableSources: Array<VirtualFile>): Library.ModifiableModel? {
      val sourceRoots = LibrarySourceRootDetectorUtil.scanAndSelectDetectedJavaSourceRoots(null, availableSources)
      if (sourceRoots.isEmpty()) return null
      val modifiableModel = this.modifiableModel
      sourceRoots.forEach { source ->
        modifiableModel.addRoot(source.url, OrderRootType.SOURCES)
      }
      return modifiableModel
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
