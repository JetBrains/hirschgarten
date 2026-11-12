package org.jetbrains.bazel.ui.gutters

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.util.ThreeState
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTargetInfo

/**
 * Tells which targets from a given set run the code at a PSI element.
 *
 * The entry point of a target is the class that the target starts.
 * For a test, it is the test class. For a binary, it is the main class.
 */
@ApiStatus.Internal
interface BazelEntryPointMatcher {
  /**
   * Returns one result for each target in [targets]:
   * - [ThreeState.YES] when the entry point contains [element], or [element] contains the entry point.
   *   For example, [element] is a method of the test class, or the file of the test class.
   * - [ThreeState.NO] when the entry point is a different class.
   * - [ThreeState.UNSURE] when this matcher does not know the entry point.
   *
   * [ThreeState.UNSURE] from a matcher does not change the result of another matcher.
   * When one matcher gives [ThreeState.YES] and another gives [ThreeState.NO], the result is [ThreeState.UNSURE].
   * A target that no matcher knows gets [ThreeState.UNSURE].
   *
   * The result depends on [element], not only on its file.
   * A Kotlin file can hold more than one class, and a Java class can hold nested classes.
   * Each of these classes can be the entry point of a different target.
   */
  fun match(element: PsiElement, targets: List<BuildTargetInfo>): Map<Label, ThreeState>

  companion object {
    val ep: ExtensionPointName<BazelEntryPointMatcher> = ExtensionPointName("org.jetbrains.bazel.entryPointMatcher")

    /**
     * Returns one result for each target in [targets] from all matchers.
     * The results combine as [BazelEntryPointMatcher.match] describes.
     */
    fun matchAll(element: PsiElement, targets: List<BuildTargetInfo>): Map<Label, ThreeState> {
      val matches = targets.associateTo(mutableMapOf()) { it.id to ThreeState.UNSURE }
      ep.forEachExtensionSafe { matcher ->
        for ((key, value) in matcher.match(element, targets)) {
          matches.merge(key, value, ThreeState::combine)
        }
      }
      return matches
    }
  }
}

/**
 * Combines the results of two matchers.
 * [ThreeState.UNSURE] changes nothing, and [ThreeState.YES] with [ThreeState.NO] gives [ThreeState.UNSURE].
 */
private fun ThreeState.combine(other: ThreeState): ThreeState = when (this) {
  ThreeState.UNSURE -> other
  ThreeState.YES, ThreeState.NO -> if (other == ThreeState.UNSURE) this else merge(other)
}
