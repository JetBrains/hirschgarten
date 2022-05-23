package org.jetbrains.plugins.bsp.protocol

import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.SourcesParams
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.jetbrains.magicmetamodel.ProjectDetails
import java.nio.file.Path

class VeryTemporaryBspResolver(private val projectBaseDir: Path) {

  private val server = VeryTemporaryBspConnection(projectBaseDir).bspServer

  fun collectModel(): ProjectDetails {
    println("buildInitialize")
    server.buildInitialize(createInitializeBuildParams()).get()

    println("onBuildInitialized")
    server.onBuildInitialized()

    println("workspaceBuildTargets")
    val workspaceBuildTargetsResult = server.workspaceBuildTargets().get()
    val allTargetsIds = workspaceBuildTargetsResult.targets.map(BuildTarget::getId)

    println("buildTargetSources")
    val sourcesResult = server.buildTargetSources(SourcesParams(allTargetsIds)).get()

    println("buildTargetResources")
    val resourcesResult = server.buildTargetResources(ResourcesParams(allTargetsIds)).get()

    println("buildTargetDependencySources")
    val dependencySourcesResult = server.buildTargetDependencySources(DependencySourcesParams(allTargetsIds)).get()

    println("done!")

    return ProjectDetails(
      targetsId = allTargetsIds,
      targets = workspaceBuildTargetsResult.targets.toSet(),
      sources = sourcesResult.items,
      resources = resourcesResult.items,
      dependenciesSources = dependencySourcesResult.items,
    )
  }

  private fun createInitializeBuildParams(): InitializeBuildParams {
    val params = InitializeBuildParams(
      "IntelliJ-BSP",
      "1.0.0",
      "2.0.0",
      projectBaseDir.toString(),
      BuildClientCapabilities(listOf("java"))
    )
    val dataJson = JsonObject()
    dataJson.addProperty("clientClassesRootDir", "$projectBaseDir/out")
    dataJson.add("supportedScalaVersions", JsonArray())
    params.data = dataJson

    return params
  }
}
