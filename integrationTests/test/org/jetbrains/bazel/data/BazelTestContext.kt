package org.jetbrains.bazel.data

import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.IdeInfo

enum class BazelTestContext {
  IDEA,
  GOLAND,
  PYCHARM;

  fun getIdeInfo(): IdeInfo = when(this) {
    IDEA -> IdeProductProvider.IU
    GOLAND -> IdeProductProvider.GO
    PYCHARM -> IdeProductProvider.PY
  }
}
