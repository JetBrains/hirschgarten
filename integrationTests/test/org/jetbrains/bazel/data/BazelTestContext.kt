package org.jetbrains.bazel.data

import com.intellij.ide.starter.models.IdeInfo
import com.intellij.tools.ide.starter.product.goland.GoLand
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import com.intellij.tools.ide.starter.product.pycharm.PyCharm

enum class BazelTestContext {
  IDEA,
  GOLAND,
  PYCHARM,
  IDEA_GO_PLUGIN,
  ;

  fun getIdeInfo(): IdeInfo = when(this) {
    IDEA -> IdeInfo.IdeaUltimate
    GOLAND -> IdeInfo.GoLand
    PYCHARM -> IdeInfo.PyCharm
    IDEA_GO_PLUGIN -> IdeInfo.IdeaUltimate.copy(additionalModules = IdeInfo.IdeaUltimate.additionalModules + listOf("intellij.go.plugin"))
  }
}
