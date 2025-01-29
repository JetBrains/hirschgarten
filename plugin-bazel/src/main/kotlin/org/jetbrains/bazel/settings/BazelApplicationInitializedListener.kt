package org.jetbrains.bazel.settings

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.ide.plugins.PluginStateManager

class BazelApplicationInitializedListener : ApplicationInitializedListener {
  override suspend fun execute() {
    PluginStateManager.addStateListener(
      object : PluginStateListener {
        override fun install(descriptor: IdeaPluginDescriptor) {
          BazelPluginUpdater.verifyPluginVersionChannel(descriptor)
        }
      },
    )
  }
}
