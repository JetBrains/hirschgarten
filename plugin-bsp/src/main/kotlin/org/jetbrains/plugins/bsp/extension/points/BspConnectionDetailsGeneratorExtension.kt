package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.javaapi.CollectionConverters.asJava
import scala.jdk.javaapi.CollectionConverters.asScala
import java.io.File
import java.nio.file.Paths
import kotlin.collections.List
import kotlin.collections.map
import coursier.core.Dependency as CoursierDependency
import coursier.core.Module as CoursierModule
import scala.collection.immutable.List as ScalaImmutableList
import scala.collection.immutable.Map as ScalaImmutableMap

public interface BspConnectionDetailsGeneratorExtension : BspConnectionDetailsGenerator {
  public companion object {
    private val ep =
      ExtensionPointName.create<BspConnectionDetailsGeneratorExtension>(
        "com.intellij.bspConnectionDetailsGeneratorExtension")

    public fun extensions(): List<BspConnectionDetailsGeneratorExtension> =
      ep.extensionList
  }
}

public object ExternalCommandUtils {
  public fun calculateJavaExecPath(): String {
    val javaHome = System.getProperty("java.home")
    if (javaHome == null) {
      error("Java needs to be set up before running the plugin")
    } else {
      return "${Paths.get(javaHome, "bin", "java")}"
    }
  }

  @Suppress("UNCHECKED_CAST")
  public fun calculateNeededJars(org: String, name: String, version: String): List<String> {
    val attributes = ScalaImmutableMap.from(asScala(mapOf<String, String>()))
    val dependencies = listOf<CoursierDependency>(
      CoursierDependency.apply(
        CoursierModule.apply(org, name, attributes),
        version,
      ),
    )
    val fetchTask = coursier
      .Fetch
      .apply()
      .addDependencies(asScala(dependencies).toSeq())
    val executionContext = fetchTask.cache().ec()
    val future = fetchTask.io().future(executionContext)
    val futureResult = Await.result(future, Duration.Inf())
    return asJava(futureResult as ScalaImmutableList<File>).map { it.canonicalPath }
  }
}
