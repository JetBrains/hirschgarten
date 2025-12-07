package org.jetbrains.bazel.ui.widgets.tool.window.all.targets

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.util.coroutines.flow.throttle
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.BazelProjectProperties
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.sync_new.BazelSyncV2
import org.jetbrains.bazel.sync_new.flow.index.TargetTreeIndexService
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelTargetsPanel
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelTargetsPanelModel
import org.jetbrains.bazel.ui.widgets.tool.window.components.SyncV2TargetTreeCompat
import org.jetbrains.bazel.ui.widgets.tool.window.components.TargetUtilsTargetTreeCompat
import org.jetbrains.bazel.ui.widgets.tool.window.components.configureBazelToolWindowToolBar
import java.awt.BorderLayout

private class BazelAllTargetsWidgetFactory :
  ToolWindowFactory,
  DumbAware {
  private val contentRequested = Semaphore(permits = 1, acquiredPermits = 1)

  override suspend fun isApplicableAsync(project: Project): Boolean = project.serviceAsync<BazelProjectProperties>().isBazelProject

  override fun shouldBeAvailable(project: Project): Boolean = project.isBazelProject

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    contentRequested.release()

    val panel = JBLoadingPanel(BorderLayout(), toolWindow.disposable)
    panel.startLoading()
    toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
  }

  override suspend fun manage(toolWindow: ToolWindow, toolWindowManager: ToolWindowManager) {
    coroutineScope {
      launch(Dispatchers.EDT) {
        // double-check again
        val isBazelProject = toolWindow.project.serviceAsync<BazelProjectProperties>().isBazelProject
        toolWindow.setAvailable(isBazelProject)
        if (isBazelProject && !toolWindowManager.isStripeButtonShow(toolWindow)) {
          // the only way to force adding a stripe button is to show the tool window, due to a platform API limitation
          toolWindow.show()
        }
      }

      contentRequested.acquire()

      val project = toolWindow.project
      val updateRequests = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
      val model = BazelTargetsPanelModel(updateRequests)
      val actionManager = serviceAsync<ActionManager>()
      val targetPanel =
        async(Dispatchers.EDT) {
          val windowPanel = SimpleToolWindowPanel(true, true)
          configureBazelToolWindowToolBar(model, actionManager, windowPanel, project)
          val panel = BazelTargetsPanel(project, model)
          windowPanel.setContent(panel)

          val content = toolWindow.contentManager.getContent(0)!!
          val loadingPanel = content.component as JBLoadingPanel
          loadingPanel.stopLoading()
          content.component = windowPanel

          panel
        }

      val targetUtils = project.serviceAsync<TargetUtils>()
      // update immediately, avoid any delays
      updateVisibleTargets(targetUtils = targetUtils, project = project, model = model, targetPanel = targetPanel)
      merge(targetUtils.targetListUpdated, updateRequests)
        .throttle(300)
        .collectLatest {
          updateVisibleTargets(targetUtils = targetUtils, project = project, model = model, targetPanel = targetPanel)
        }
    }
  }
}

suspend fun showBspToolWindow(project: Project) {
  val toolWindow = project.serviceAsync<ToolWindowManager>().getToolWindow(BazelPluginConstants.BAZEL_TOOLWINDOW_ID) ?: return
  withContext(Dispatchers.EDT) {
    toolWindow.show()
  }
}

/**
 * Normally not needed, unless we link a Bazel project after [ToolWindowFactory.shouldBeAvailable] was called.
 */
suspend fun registerBazelToolWindow(project: Project) {
  val toolWindowManager = project.serviceAsync<ToolWindowManager>()
  val currentToolWindow = toolWindowManager.getToolWindow(bazelToolWindowId)
  if (currentToolWindow == null) {
    withContext(Dispatchers.EDT) {
      toolWindowManager
        .registerToolWindow(bazelToolWindowId) {
          this.icon = BazelPluginIcons.bazelToolWindow
          this.anchor = ToolWindowAnchor.RIGHT
          this.canCloseContent = false
          this.contentFactory = BazelAllTargetsWidgetFactory()
        }.show()
    }
  }
}

private val bazelToolWindowId: String
  get() = BazelPluginConstants.BAZEL_DISPLAY_NAME

private suspend fun updateVisibleTargets(
  targetUtils: TargetUtils,
  project: Project,
  model: BazelTargetsPanelModel,
  targetPanel: Deferred<BazelTargetsPanel>,
) {
  // First, apply the filter
  val targets = if (BazelSyncV2.useNewTargetTreeStorage) {
    project.serviceAsync<TargetTreeIndexService>()
      .getTargetTreeEntriesSequence()
      .map { SyncV2TargetTreeCompat(it) }
  } else {
    targetUtils.allBuildTargets()
      .map { TargetUtilsTargetTreeCompat(it) }
  }
  val filteredTargets = targets.filter { model.targetFilter.predicate(it) }
  val hasAnyTargets = targetUtils.getTotalTargetCount() > 0
  // Then, apply the search query
  var searchRegex: Regex?
  val searchResults =
    if (model.searchQuery.isEmpty()) {
      searchRegex = null
      filteredTargets
    } else {
      val options =
        buildSet {
          if (!model.matchCase) add(RegexOption.IGNORE_CASE)
          if (!model.regexMode) add(RegexOption.LITERAL)
        }

      searchRegex = model.searchQuery.toRegex(options)
      filteredTargets.filter { target ->
        searchRegex.containsMatchIn(target.name)
          || searchRegex.containsMatchIn(target.label.toShortString(project))
      }
    }

  // Finally, sort the results
  val visibleTargets = searchResults.sortedBy { it.label.toShortString(project) }
  val targetPanel = targetPanel.await()
  withContext(Dispatchers.EDT) {
    targetPanel.update(
      visibleTargets = visibleTargets.toList(),
      searchRegex = searchRegex,
      hasAnyTargets = hasAnyTargets,
      displayAsTree = model.displayAsTree,
    )
  }
}
