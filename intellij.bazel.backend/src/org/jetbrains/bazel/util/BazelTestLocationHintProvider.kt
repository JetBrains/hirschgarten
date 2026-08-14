package org.jetbrains.bazel.util

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.testing.BazelTestDetails
import kotlin.run
import kotlin.text.replace

@ApiStatus.Internal
interface BazelTestLocationHintProvider {
  fun getLocationHint(testDetails: BazelTestDetails): String?

  companion object {
    val ep =
      ExtensionPointName.create<BazelTestLocationHintProvider>("org.jetbrains.bazel.testLocationHintProvider")

    fun getLocationHint(testDetails: BazelTestDetails): String =
      getTestLocationHintByFileLocation(testDetails) ?: ep.computeSafeIfAny { it.getLocationHint(testDetails) } ?: ""

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
