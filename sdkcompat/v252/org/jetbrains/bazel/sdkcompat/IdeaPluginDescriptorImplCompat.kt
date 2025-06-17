package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginMainDescriptor

object IdeaPluginDescriptorImplCompat {
  fun patchPluginVersion(newVersion: String, descriptor: IdeaPluginDescriptorImpl) {
    when (descriptor) {
      is PluginMainDescriptor -> {
        val versionField = PluginMainDescriptor::class.java.getDeclaredField("version")
        versionField.setAccessible(true)
        versionField.set(descriptor, newVersion)
        versionField.setAccessible(false)
      }
      else -> throw IllegalArgumentException("Unsupported descriptor type: ${descriptor.javaClass.name}")
    }
  }
}
