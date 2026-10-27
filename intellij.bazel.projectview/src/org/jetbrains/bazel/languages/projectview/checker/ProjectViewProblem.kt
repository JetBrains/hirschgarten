package org.jetbrains.bazel.languages.projectview.checker

import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionKey
import java.nio.file.Path

@ApiStatus.Internal
data class ProjectViewProblem(
  val id: Id,
  @InspectionMessage val message: String,
  val severity: Severity,
  val quickFixes: List<QuickFix> = emptyList(),
  val element: PsiElement? = null,
  val sourcePath: Path? = null,
) {

  /**
   * The entries go from the lowest to the highest severity.
   */
  enum class Severity {
    WeakWarning, Warning, Error
  }

  /**
   * [value] names the problem in a suppression comment, so it has only letters, digits and underscores.
   * Do not change a value, because the comments in project view files use it.
   */
  data class Id(val value: String) {

    init {
      require(value.matches(regex)) { "A problem ID has only letters, digits and underscores: '$value'" }
    }

    companion object {
      /** The pattern of [value]. The suppression comments build their patterns from it. */
      internal const val PATTERN: String = """\w+"""
      private val regex = Regex(PATTERN)

      val UnsupportedSection: Id = Id("unsupported_section")
      val InvalidValue: Id = Id("invalid_value")
      val MissingPath: Id = Id("missing_path")
      val UnknownFlag: Id = Id("unknown_flag")
      val FlagNotAllowed: Id = Id("flag_not_allowed")
      val UnresolvedImport: Id = Id("unresolved_import")
      val Deprecation: Id = Id("deprecation")
    }
  }

  /**
   * Quick fix descriptor which will be turned into appropriate action.
   */
  sealed interface QuickFix {

    /**
     * Merge the deprecated section values to the section with [targetSectionKey]. If target section does not exist, it will be created.
     *
     * Can only be applied to section PSI element.
     */
    data class MergeIntoSection(val targetSectionKey: ProjectViewSectionKey<*>) : QuickFix

    /**
     * Special quick fix to replace deprecated `index_...` sections with `index`.
     */
    data object ReplaceWithIndex : QuickFix
  }
}
