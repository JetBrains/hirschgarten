package org.jetbrains.bazel.util

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelRuleSetProvider {
  fun ruleSets() : Set<String>

  companion object {
    val ep =
      ExtensionPointName.create<BazelRuleSetProvider>("org.jetbrains.bazel.bazelRuleSetProvider")
  }
}

internal fun relevantRuleSets() : Set<String> {
  val result = mutableSetOf<String>()
  BazelRuleSetProvider.ep.forEachExtensionSafe { provider ->
     result.addAll(provider.ruleSets())
  }
  return result
}
