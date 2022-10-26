package org.jetbrains.plugins.bsp.extension.points

import ch.epfl.scala.bsp4j.BuildTarget
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

public class TemporaryTestTargetClassifier : BspBuildTargetClassifierExtension {
  override fun name(): String = "bazelbsp"

  override fun separator(): String = "/"

  override fun getBuildTargetPath(buildTarget: BuildTarget): List<String> {
    val uri = buildTarget.id.uri
    return if (uri.startsWith("//") && uri.contains(':')) {
      uri
        .dropWhile { it == '/' }
        .takeWhile { it != ':' }
        .split('/')
        .filter { it.isNotEmpty() }
    } else emptyList()
  }

  override fun getBuildTargetName(buildTarget: BuildTarget): String {
    val uri = buildTarget.id.uri
    return if (uri.startsWith("//") && uri.contains(':')) {
      ":" + uri
        .takeLastWhile { it != ':' }
    } else buildTarget.displayName
  }
}
