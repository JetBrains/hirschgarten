package org.jetbrains.bazel.startup

import org.jetbrains.bazel.commons.EnvironmentProvider
import com.intellij.util.EnvironmentUtil as IntellijEnvironmentUtil

object IntellijEnvironmentProvider : EnvironmentProvider {
  override fun getValue(name: String): String? = IntellijEnvironmentUtil.getValue(name)
}
