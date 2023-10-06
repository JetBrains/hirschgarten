package org.jetbrains.plugins.bsp.ui.notifications

import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.ActionCallback
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library as MMMLibrary

internal class BazelAttachSourcesProvider : AttachSourcesProvider {
  private class BazelAttachSourcesAction : AttachSourcesProvider.AttachSourcesAction {
    override fun getName(): String = BspPluginBundle.message("sources.attach.action.text")

    override fun getBusyText(): String = BspPluginBundle.message("sources.pending.text")

    override fun perform(orderEntries: List<LibraryOrderEntry>): ActionCallback {
      val mmmLibraries = getAllMMMLibraries(orderEntries)
      val libraries = orderEntries.mapNotNull { it.library }
      val modelsToCommit = libraries.mapNotNull { library ->
        val bazelLibrarySources = library.pickSourcesFromBazel(mmmLibraries).orEmpty()
        library.obtainModelWithAddedSources(bazelLibrarySources)
      }
      if (modelsToCommit.isNotEmpty()) WriteAction.run<Exception> {
        modelsToCommit.forEach {
          it.commit()
        }
      }
      return ActionCallback.DONE
    }

    private fun Library.obtainModelWithAddedSources(sources: List<String>): Library.ModifiableModel? {
      return if (sources.isEmpty()) null
      else modifiableModel.apply {
        sources.forEach {
          addRoot(MMMLibrary.formatJarString(it), OrderRootType.SOURCES)
        }
      }
    }
  }

  override fun getActions(
    orderEntries: MutableList<out LibraryOrderEntry>,
    psiFile: PsiFile,
  ): List<AttachSourcesProvider.AttachSourcesAction> =
    if (isApplicableForEntries(orderEntries)) listOf(BazelAttachSourcesAction())
    else emptyList()

  private fun isApplicableForEntries(orderEntries: List<LibraryOrderEntry>): Boolean {
    val mmmLibraries = getAllMMMLibraries(orderEntries)
    return orderEntries.any { it.library?.pickSourcesFromBazel(mmmLibraries)?.isNotEmpty() ?: false }
  }

  private companion object {
    fun getAllMMMLibraries(orderEntries: List<LibraryOrderEntry>): List<MMMLibrary> {
      val project = orderEntries.firstNotNullOf { it.ownerModule.project }
      val magicMetaModel = MagicMetaModelService.getInstance(project).value
      return magicMetaModel.getLibraries()
    }

    fun Library.pickSourcesFromBazel(mmmLibraries: List<MMMLibrary>) =
      mmmLibraries.firstOrNull { it.displayName == name }?.sourceJars
  }
}
