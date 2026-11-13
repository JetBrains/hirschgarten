package org.jetbrains.bazel.run.testResults

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
      val providerHint =
        targetKind?.let { kind ->
          ep.computeSafeIfAny { provider -> provider.takeIf { it.isApplicable(kind) }?.getLocationHint(testDetails) }
        }
      return providerHint ?: getTestLocationHintByFileLocation(testDetails)
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
