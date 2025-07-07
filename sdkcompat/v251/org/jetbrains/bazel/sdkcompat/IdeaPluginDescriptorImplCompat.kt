package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl

object IdeaPluginDescriptorImplCompat {
  fun patchPluginVersion(newVersion: String, descriptor: IdeaPluginDescriptorImpl) {
    val versionField = IdeaPluginDescriptorImpl::class.java.getDeclaredField("version")
    versionField.setAccessible(true)
    versionField.set(descriptor, newVersion)
    versionField.setAccessible(false)
  }
}
