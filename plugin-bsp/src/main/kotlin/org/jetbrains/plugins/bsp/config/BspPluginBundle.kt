package org.jetbrains.plugins.bsp.config

import com.intellij.DynamicBundle
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.BspPluginBundle"

internal object BspPluginBundle : DynamicBundle(BUNDLE) {

  @JvmStatic
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
    getMessage(key, *params)
}

internal object BazelBspConstants {
  const val ID = "bazelbsp"
  val SYSTEM_ID = ProjectSystemId(ID, "Bazel BSP")
  val BUILD_FILE_NAMES = listOf("WORKSPACE", "WORKSPACE.bazel")
}
