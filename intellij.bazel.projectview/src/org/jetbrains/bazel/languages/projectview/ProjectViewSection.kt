package org.jetbrains.bazel.languages.projectview

import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.openapi.extensions.forEachExtensionSafeInline
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.sectionName

@ApiStatus.Internal
class ProjectViewSection<T>(
  val key: ProjectViewSectionKey<T>,
  val type: ProjectViewSectionType<T & Any>,
  @Nls val documentation: String,
  val deprecation: Deprecation? = null,
) {

  class Deprecation(
    val message: (sectionName: String) -> @InspectionMessage String,
    val level: Level = Level.Warning,
    val quickFixes: List<ProjectViewProblem.QuickFix> = emptyList()
  ) {

    enum class Level {
      Warning, Error
    }

    companion object {

      fun withMergeQuickFix(
        targetKey: ProjectViewSectionKey<*>,
        level: Level = Level.Warning,
      ): Deprecation = Deprecation(
        message = { sectionName ->
          BazelProjectViewBundle.message("annotator.deprecated.section.with.replacement.warning", sectionName, targetKey.name)
        },
        level = level,
        quickFixes = listOf(ProjectViewProblem.QuickFix.MergeIntoSection(targetKey))
      )
    }
  }

  companion object {

    private val byName: Map<String, ProjectViewSection<*>>
      get() = ProjectViewSectionProvider.EP_NAME.computeIfAbsent(ProjectViewSection::class.java) {
        buildMap {
          ProjectViewSectionProvider.EP_NAME.forEachExtensionSafeInline { provider ->
            provider.sections.forEach { putIfAbsent(it.key.name, it) }
          }
        }
      }

    val allRegistered: Sequence<ProjectViewSection<*>>
      get() = byName.values.asSequence()

    fun findByName(name: String): ProjectViewSection<*>? = byName[name]

    fun findByPsi(psi: PsiElement): ProjectViewSection<*>? {
      val section = psi.parentOfType<ProjectViewPsiSection>(withSelf = true) ?: return null
      return section.sectionName().let(::findByName)
    }
  }
}
