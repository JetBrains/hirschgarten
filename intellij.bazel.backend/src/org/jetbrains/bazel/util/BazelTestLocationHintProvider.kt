package org.jetbrains.bazel.util

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.testing.BazelTestDetails

@ApiStatus.Internal
interface BazelTestLocationHintProvider {
  fun isApplicable(targetKind: TargetKind): Boolean

  fun getLocationHint(testDetails: BazelTestDetails): String?

  companion object {
    val ep =
      ExtensionPointName.create<BazelTestLocationHintProvider>("org.jetbrains.bazel.testLocationHintProvider")

    fun getLocationHint(testDetails: BazelTestDetails, targetKind: TargetKind?): String? {
      getTestLocationHintByFileLocation(testDetails)?.let { return it }
      if (targetKind == null) return null
      return ep.computeSafeIfAny { provider ->
        provider.takeIf { it.isApplicable(targetKind) }?.getLocationHint(testDetails)
      }
    }

    private fun getTestLocationHintByFileLocation(testDetails: BazelTestDetails): String? =
      testDetails.run {
        if (oneBasedLine != null) {
          val forwardSlashFilePath = file?.replace('\\', '/') ?: return null
          "file://$forwardSlashFilePath:$oneBasedLine"
        } else {
          null
        }
    }
  }
}
