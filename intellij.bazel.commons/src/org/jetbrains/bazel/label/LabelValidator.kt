package org.jetbrains.bazel.label

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object LabelValidator {

  /**
   * Follows the Bazel doc - https://bazel.build/concepts/labels#target-names
   */
  fun isTargetNameValid(targetName: String): Boolean {
    return targetName.isNotEmpty() && VALID_TARGET_NAME_PATTERN.matches(targetName)
  }

  private val VALID_TARGET_NAME_PATTERN = Regex("""[a-zA-Z0-9!%\-@^_"#$&'()*+,;<=>?\[\]{}|~/.]+""")
}
