package org.jetbrains.bazel.kotlin.ui.actions

import com.intellij.psi.PsiDirectory
import com.intellij.util.ThreeState
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.kotlin.idea.actions.NewKotlinFileActionAvailabilityOverride

/**
 * @see org.jetbrains.bazel.flow.modify.BazelCreateJavaClassActionAvailabilityOverride
 */
internal class BazelNewKotlinFileActionAvailabilityOverride : NewKotlinFileActionAvailabilityOverride {
  override fun isActionAvailable(directories: Array<PsiDirectory>): ThreeState {
    return if (directories.firstOrNull()?.project?.isBazelProject == true) {
      ThreeState.YES
    }
    else {
      ThreeState.UNSURE
    }
  }
}
