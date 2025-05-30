package org.jetbrains.bazel.ui.widgets

import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.ide.actions.searcheverywhere.SearchEverywherePreviewProvider
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor
import com.intellij.ide.util.EditorHelper
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.PsiElementNavigationItem
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.languages.starlark.references.resolveTargetPattern
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.target.targetUtils
import javax.swing.ListCellRenderer

class LabelSearchEverywhereContributor(private val project: Project) :
  WeightedSearchEverywhereContributor<LabelSearchEverywhereContributor.LabelWithPreview>,
  SearchEverywherePreviewProvider {
  private val listCellRenderer: ListCellRenderer<LabelWithPreview> =
    SimpleListCellRenderer.create { label, value, index ->
      label.text = value.displayName
      label.icon = BazelPluginIcons.bazel
    }

  override fun getSearchProviderId(): String = javaClass.simpleName

  override fun getGroupName(): String = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override fun getSortWeight(): Int = 0

  override fun showInFindResults(): Boolean = true

  override fun fetchWeightedElements(
    pattern: String,
    progressIndicator: ProgressIndicator,
    consumer: Processor<in FoundItemDescriptor<LabelWithPreview>>,
  ) {
    val matcher =
      NameUtil
        .buildMatcher("*$pattern")
        .withSeparators("@/:")
        .typoTolerant()
        .withCaseSensitivity(NameUtil.MatchingCaseSensitivity.NONE)
        .build()

    val foundItems =
      project.targetUtils
        .allTargets()
        .mapNotNull { label ->
          val fullString = label.toString()
          if (!matcher.matches(fullString)) return@mapNotNull null
        val targetElement = runReadAction { resolveTargetPattern(project, label) } ?: return@mapNotNull null
          val displayName = label.toShortString(project)
          FoundItemDescriptor(LabelWithPreview(displayName, targetElement), matcher.matchingDegree(fullString))
        }.toList()

    // Copied from AbstractGotoSEContributor
    if (ModalityState.defaultModalityState() == ModalityState.nonModal()) {
      @Suppress("UsagesOfObsoleteApi", "DEPRECATION")
      ProgressIndicatorUtils.yieldToPendingWriteActions()
    }
    @Suppress("UsagesOfObsoleteApi", "DEPRECATION")
    ProgressIndicatorUtils.runInReadActionWithWriteActionPriority({
      ContainerUtil.process(foundItems, consumer)
    }, progressIndicator)
  }

  override fun processSelectedItem(
    selected: LabelWithPreview,
    modifiers: Int,
    searchText: String,
  ): Boolean {
    BazelCoroutineService.getInstance(project).start {
      withContext(Dispatchers.EDT) {
        EditorHelper.openInEditor(selected.element, true, true)
      }
    }
    return true
  }

  override fun getElementsRenderer(): ListCellRenderer<in LabelWithPreview> = listCellRenderer

  class LabelWithPreview(val displayName: String, val element: PsiElement) : PsiElementNavigationItem {
    override fun getTargetElement(): PsiElement = element

    override fun getName(): String? = null

    override fun getPresentation(): ItemPresentation? = null
  }

  class Factory : SearchEverywhereContributorFactory<LabelWithPreview> {
    override fun createContributor(initEvent: AnActionEvent): SearchEverywhereContributor<LabelWithPreview> =
      LabelSearchEverywhereContributor(requireNotNull(initEvent.project))

    override fun isAvailable(project: Project): Boolean = project.isBazelProject
  }
}
