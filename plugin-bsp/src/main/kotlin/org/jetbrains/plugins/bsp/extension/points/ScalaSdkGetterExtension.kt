package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.plugins.bsp.server.tasks.ScalaSdk
import org.jetbrains.plugins.scala.project.ScalaLanguageLevel
import org.jetbrains.plugins.scala.project.external.ScalaSdkUtils
import scala.Option
import scala.Some
import scala.jdk.javaapi.CollectionConverters.asScala
import java.io.File
import java.net.URI
import java.nio.file.Paths
import scala.collection.immutable.Seq as ScalaImmutableSeq

public interface ScalaSdkGetterExtension {
  public fun addScalaSdk(
    scalaSdk: ScalaSdk,
    modelsProvider: IdeModifiableModelsProvider,
  )
}

private val ep =
  ExtensionPointName.create<ScalaSdkGetterExtension>(
    "com.intellij.scalaSdkGetterExtension",
  )

public fun scalaSdkGetterExtension(): ScalaSdkGetterExtension? =
  ep.extensionList.firstOrNull()

public fun scalaSdkGetterExtensionExists(): Boolean =
  ep.extensionList.isNotEmpty()

public class ScalaSdkGetter : ScalaSdkGetterExtension {
  override fun addScalaSdk(
    scalaSdk: ScalaSdk,
    modelsProvider: IdeModifiableModelsProvider,
  ) {
    if (ScalaLanguageLevel.findByVersion(scalaSdk.scalaVersion).isEmpty) return

    val scalaSdkName = scalaSdk.scalaVersion.scalaVersionToScalaSdkName()
    val projectLibrariesModel = modelsProvider.modifiableProjectLibrariesModel
    val existingScalaLibrary = projectLibrariesModel
      .libraries
      .find { it.name?.equals(scalaSdkName) == true }

    val sdkJars = scalaSdk.sdkJars.map { Paths.get(URI(it)).toFile() }

    val scalaLibrary = existingScalaLibrary
      ?: projectLibrariesModel.createLibrary(scalaSdkName)

    sdkJars.filter { jar ->
      projectLibrariesModel.libraries.find { it.name?.equals(jar.name) == true } == null
    }.filter {
      VfsUtil.findFileByIoFile(it, true) != null
    }.forEach {
      val jarLib = projectLibrariesModel.createLibrary(it.name)
      val model = jarLib.modifiableModel
      model.addRoot(VfsUtil.getUrlForLibraryRoot(it), OrderRootType.CLASSES)
      model.commit()
    }

    ScalaSdkUtils.ensureScalaLibraryIsConvertedToScalaSdk(
      modelsProvider,
      scalaLibrary,
      Some(scalaSdk.scalaVersion),
      ScalaImmutableSeq.from(asScala(sdkJars)),
      ScalaImmutableSeq.from(asScala(emptyList<File>())),
      Option.empty(),
    )
  }
}
