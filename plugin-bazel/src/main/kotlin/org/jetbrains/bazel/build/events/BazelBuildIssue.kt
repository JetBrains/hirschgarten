package org.jetbrains.bazel.build.events

import com.intellij.build.issue.BuildIssue
import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.pom.NavigatableAdapter
import org.jetbrains.bazel.label.Label

/**
 * Custom BuildIssue that supports navigation to Bazel labels.
 *
 * Ported from Google's Bazel plugin to provide better integration with Build View.
 * When a build error references a Bazel target (e.g., "//foo/bar:baz"), clicking the
 * error in the Build View will navigate to the target definition in BUILD files.
 */
data class BazelBuildIssue(
  override val title: String,
  override val description: String,
  override val quickFixes: List<BuildIssueQuickFix> = emptyList(),
  val label: Label? = null,
) : BuildIssue {

  override fun getNavigatable(project: Project): Navigatable? {
    if (label == null) return null
    return LabelNavigatable(project, label)
  }
}

/**
 * Navigatable implementation that resolves Bazel labels to their BUILD file definitions.
 */
private class LabelNavigatable(
  private val project: Project,
  private val label: Label
) : NavigatableAdapter() {

  override fun navigate(requestFocus: Boolean) {
    // TODO: Implement label-to-file resolution
    // This should:
    // 1. Find the BUILD file for the label's package
    // 2. Find the target definition within that file
    // 3. Navigate to it
    //
    // Potential approaches:
    // - Use `bazel query "label" --output=location` for accuracy
    // - Use PSI to find the BUILD file and search for the target name
    // - Cache label -> file mappings from previous builds
  }
}
