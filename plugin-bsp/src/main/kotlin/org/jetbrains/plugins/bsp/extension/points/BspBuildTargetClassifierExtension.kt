package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.BspBuildTargetClassifier

public interface BspBuildTargetClassifierExtension : BspBuildTargetClassifier {
  public companion object {
    private val ep =
      ExtensionPointName.create<BspBuildTargetClassifierExtension>("com.intellij.bspBuildTargetClassifierExtension")

    public fun extensions(): List<BspBuildTargetClassifierExtension> =
      ep.extensionList
  }
}
