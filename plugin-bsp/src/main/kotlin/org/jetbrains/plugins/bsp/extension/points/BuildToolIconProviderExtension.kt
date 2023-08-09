package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.BuildToolIconProvider
import javax.swing.Icon

public interface BuildToolIconProviderExtension : BuildToolIconProvider

private val ep =
  ExtensionPointName.create<BuildToolIconProviderExtension>("com.intellij.buildToolIconProviderExtension")

public fun buildToolIconProviderExtensions(): List<BuildToolIconProviderExtension> =
  ep.extensionList

public class BazelBuildToolIconProvider : BuildToolIconProviderExtension {
  override fun name(): String = "bazelbsp"

  override fun icon(): Icon = BspPluginIcons.bazel
}
