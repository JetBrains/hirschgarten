package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
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

  private val bazelLabelRegex = """@?(?<repository>.*)//(?<package>.*):(?<target>.*)""".toRegex()

  override fun getBuildTargetPath(buildTargetIdentifier: BuildTargetId): List<String> {
    return bazelLabelRegex.find(buildTargetIdentifier)?.groups
      ?.get("package")
      ?.value
      ?.split("/")
      ?.filter { it.isNotEmpty() }
      .orEmpty()
  }

  override fun getBuildTargetName(buildTargetIdentifier: BuildTargetId): String =
    bazelLabelRegex.find(buildTargetIdentifier)?.groups?.get("target")?.value ?: buildTargetIdentifier
}
