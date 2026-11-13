package org.jetbrains.bazel.flow.modify

import com.intellij.ide.actions.CreateJavaClassActionAvailabilityOverride
import com.intellij.psi.PsiDirectory
import com.intellij.util.ThreeState
import org.jetbrains.bazel.config.isBazelProject

/**
 * With single-file source roots, Java/Kotlin plugins assume that since the directory is not covered, no file can be created.
 * Hence, we just allow creating source files anywhere in the project via the respective extension points.
 */
internal class BazelCreateJavaClassActionAvailabilityOverride : CreateJavaClassActionAvailabilityOverride {
  override fun isActionAvailable(directories: Array<out PsiDirectory>): ThreeState =
    if (directories.firstOrNull()?.project?.isBazelProject == true) {
      ThreeState.YES
    }
    else {
      ThreeState.UNSURE
    }
}
