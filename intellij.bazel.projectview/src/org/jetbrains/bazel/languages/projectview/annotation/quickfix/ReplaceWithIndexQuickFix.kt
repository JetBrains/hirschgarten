package org.jetbrains.bazel.languages.projectview.annotation.quickfix

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.openapi.project.DumbAware
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.languages.projectview.INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.INDEX_ALL_FILES_IN_DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.INDEX_KEY
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementFactory
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.read

/**
 * Implementation of [org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem.QuickFix.ReplaceWithIndex] quick fix.
 *
 * Replaces `index_all_files_in_directories` and `index_additional_files_in_directories` with one `index` section.
 * `index_all_files_in_directories: true` becomes the single line `index: *`.
 * The fix keeps the patterns that [org.jetbrains.bazel.languages.projectview.index] derives from the deprecated sections.
 * An existing `index` section with items has priority, so the fix only removes the deprecated sections in that case.
 * An empty `index` section has no effect, so the fix replaces it.
 */
internal class ReplaceWithIndexQuickFix(
  section: ProjectViewPsiSection,
) : PsiUpdateModCommandAction<ProjectViewPsiSection>(section), DumbAware {

  private val targetSectionName = INDEX_KEY.name

  override fun invoke(
    context: ActionContext,
    element: ProjectViewPsiSection,
    updater: ModPsiUpdater,
  ) {
    val file = element.containingFile as? ProjectViewPsiFile ?: return
    val indexAllFiles = file.getSection(INDEX_ALL_FILES_IN_DIRECTORIES_KEY.name)
    val indexAdditionalFiles = file.getSection(INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY.name)
    val target = file.getSection(targetSectionName)
    val emptyTarget = target?.takeIf { section -> section.getItems().none { it.text.isNotBlank() } }
    val factory = ProjectViewElementFactory(context.project)
    val replacement = when {
      target != null && emptyTarget == null -> null
      indexAllFiles?.read(type = ProjectViewSectionType.boolean) == true -> factory.createSingleLineSection(targetSectionName, "*")
      else -> indexAdditionalFiles
        ?.contentLines()
        ?.map { it.text }
        ?.takeIf { it.isNotEmpty() }
        ?.let { factory.createSection(targetSectionName, it) }
    }
    if (replacement != null) {
      element.parent.addBefore(replacement, element)
    }
    listOfNotNull(element, indexAllFiles, indexAdditionalFiles, emptyTarget)
      .distinct()
      .forEach { it.deleteContent() }
  }

  override fun getPresentation(
    context: ActionContext,
    element: ProjectViewPsiSection,
  ): Presentation = Presentation.of(BazelProjectViewBundle.message("quickfix.deprecated.section.replace.presentation", targetSectionName))

  override fun getFamilyName(): @IntentionFamilyName String =
    BazelProjectViewBundle.message("quickfix.deprecated.section.replace.description")
}
