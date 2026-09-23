package org.jetbrains.bazel.sync.workspace.languages.java

import com.intellij.aspect.lib.Rules
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.util.BazelRuleSet
import org.jetbrains.bazel.util.BazelRuleSetProvider

@ApiStatus.Internal
val JavaRuleSet: BazelRuleSet = BazelRuleSet(
  aspectLanguage = Rules.JAVA,
  rulesetNames = listOf("rules_java"),
  isBundled = true,
  autoloadHints = listOf("JavaInfo", "java_common", "JavaPluginInfo", "java_binary", "java_library"),
  hostLocations = listOf("https://github.com/bazelbuild/rules_java/"),
)

internal class JavaBazelRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): List<BazelRuleSet> = listOf(JavaRuleSet)
}
