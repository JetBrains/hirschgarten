package org.jetbrains.plugins.bsp.extension.points

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
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

  private val bazelLabelRegex = """@?(?<repository>.*)//(?<package>.*):(?<target>.*)""".toRegex()

  override fun getBuildTargetPath(buildTargetIdentifier: BuildTargetIdentifier): List<String> {
    val uri = buildTargetIdentifier.uri
    return bazelLabelRegex.find(uri)?.groups
      ?.get("package")
      ?.value
      ?.split("/")
      ?.filter { it.isNotEmpty() }
      .orEmpty()
  }

  override fun getBuildTargetName(buildTargetIdentifier: BuildTargetIdentifier): String {
    val uri = buildTargetIdentifier.uri
    return bazelLabelRegex.find(uri)?.groups?.get("target")?.value ?: uri
  }
}
