package org.jetbrains.bazel.ui.notifications

import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.LibrarySourceRootDetectorUtil
import com.intellij.openapi.util.ActionCallback
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.buildToolIdOrNull
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import java.net.URI
import kotlin.io.path.toPath
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Library as MMMLibrary

internal class BazelAttachSourcesProvider : AttachSourcesProvider {
  private class BazelAttachSourcesAction(private val project: Project) : AttachSourcesProvider.AttachSourcesAction {
    override fun getName(): String = BazelPluginBundle.message("sources.attach.action.text")

    override fun getBusyText(): String = BazelPluginBundle.message("sources.pending.text")

    override fun perform(orderEntries: List<LibraryOrderEntry>): ActionCallback {
      val mmmLibraries = getAllMMMLibraries(project)
      val fileManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
      val libraries = orderEntries.mapNotNull { it.library }.distinct()
      val modelsToCommit = libraries.mapNotNull { library ->
        val bazelLibrarySources = library.pickSourcesFromBazel(mmmLibraries)
        val availableSources = filterOutUnavailableSources(bazelLibrarySources)
        if (availableSources.isEmpty()) {
          showError(project, library.name.orEmpty())
          null
        } else {
          library.obtainModelWithAddedSources(availableSources, fileManager)
        }
      }
      if (modelsToCommit.isNotEmpty()) WriteAction.run<Exception> {
        modelsToCommit.forEach {
          it.commit()
        }
      }
      return ActionCallback.DONE
    }

    private fun filterOutUnavailableSources(sources: List<String>) =
      sources.filter { URI(it).toPath().toFile().exists() }

    private fun showError(project: Project, target: String) {
      Notification(
        RESOLVING_BAZEL_SOURCES_GROUP_ID,
        BazelPluginBundle.message("sources.files.not.resolved"),
        BazelPluginBundle.message("error.message.failed.to.resolve.sources.0", target),
        NotificationType.ERROR
      ).notify(project)
    }

    private fun Library.obtainModelWithAddedSources(
      sources: List<String>,
      fileManager: VirtualFileUrlManager
    ): Library.ModifiableModel? {
      return if (sources.isEmpty()) null
      else modifiableModel.apply {
        sources.forEach {
          addSource(it, fileManager)
        }
      }
    }

    private fun Library.ModifiableModel.addSource(sourceUri: String, fileManager: VirtualFileUrlManager) {
      val path = MMMLibrary.formatJarString(sourceUri)
      val candidate = fileManager.getOrCreateFromUri(path).virtualFile
      val sourceRoots = LibrarySourceRootDetectorUtil.scanAndSelectDetectedJavaSourceRoots(null, arrayOf(candidate))
      sourceRoots.forEach { source ->
        addRoot(source.url, OrderRootType.SOURCES)
      }
    }
  }

  override fun getActions(
    orderEntries: MutableList<out LibraryOrderEntry>,
    psiFile: PsiFile,
  ): List<AttachSourcesProvider.AttachSourcesAction> {
    val project = orderEntries.firstNotNullOf { it.ownerModule.project }
    return if (project.isBazelProject() && project.containsBazelSourcesForEntries(orderEntries))
      listOf(BazelAttachSourcesAction(project))
    else emptyList()
  }

  private fun Project.isBazelProject() =
    buildToolIdOrNull == BazelPluginConstants.bazelBspBuildToolId

  private fun Project.containsBazelSourcesForEntries(orderEntries: List<LibraryOrderEntry>): Boolean {
    val mmmLibraries = getAllMMMLibraries(this)
    return orderEntries.any { it.library?.pickSourcesFromBazel(mmmLibraries)?.isNotEmpty() ?: false }
  }

  private companion object {
    const val RESOLVING_BAZEL_SOURCES_GROUP_ID = "Resolving Bazel Sources"
  }
}

private fun getAllMMMLibraries(project: Project): List<MMMLibrary> =
  MagicMetaModelService.getInstance(project).value.getLibraries()

private fun Library.pickSourcesFromBazel(mmmLibraries: List<MMMLibrary>) =
  mmmLibraries.filter { it.displayName == name }
    .flatMap { it.sourceJars }
