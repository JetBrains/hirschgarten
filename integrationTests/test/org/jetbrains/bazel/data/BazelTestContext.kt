package org.jetbrains.bazel.data

import com.intellij.ide.starter.models.IdeInfo
import com.intellij.tools.ide.starter.build.server.goland.GoLand
import com.intellij.tools.ide.starter.build.server.idea.ultimate.IdeaUltimate
import com.intellij.tools.ide.starter.build.server.pycharm.PyCharm

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
