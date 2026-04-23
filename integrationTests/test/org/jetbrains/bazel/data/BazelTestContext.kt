package org.jetbrains.bazel.data

import com.intellij.ide.starter.models.IdeInfo
import com.intellij.tools.ide.starter.product.goland.GoLand
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import com.intellij.tools.ide.starter.product.pycharm.PyCharm

enum class BazelTestContext {
  IDEA,
  GOLAND,
  PYCHARM;

  fun getIdeInfo(): IdeInfo = when(this) {
    IDEA -> IdeInfo.IdeaUltimate
    GOLAND -> IdeInfo.GoLand
    PYCHARM -> IdeInfo.PyCharm
  }
}
