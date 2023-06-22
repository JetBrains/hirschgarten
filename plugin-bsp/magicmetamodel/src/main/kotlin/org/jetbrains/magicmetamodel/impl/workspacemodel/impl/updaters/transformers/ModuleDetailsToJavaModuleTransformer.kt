package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.JvmBuildTarget
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JvmJdkInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Module
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.toPath

internal class ModuleDetailsToJavaModuleTransformer(
  moduleNameProvider: ModuleNameProvider,
  private val projectBasePath: Path,
): WorkspaceModelEntityTransformer<ModuleDetails, JavaModule> {

  private val bspModuleDetailsToModuleTransformer = BspModuleDetailsToModuleTransformer(moduleNameProvider)
  private val sourcesItemToJavaSourceRootTransformer = SourcesItemToJavaSourceRootTransformer(projectBasePath)
  private val resourcesItemToJavaResourceRootTransformer = ResourcesItemToJavaResourceRootTransformer(projectBasePath)

  override fun transform(inputEntity: ModuleDetails): JavaModule =
    JavaModule(
      module = toModule(inputEntity),
      baseDirContentRoot = toBaseDirContentRoot(inputEntity),
      sourceRoots = sourcesItemToJavaSourceRootTransformer.transform(inputEntity.sources.map {
        BuildTargetAndSourceItem(
          inputEntity.target,
          it,
        )
      }),
      resourceRoots = resourcesItemToJavaResourceRootTransformer.transform(inputEntity.resources),
      libraries = DependencySourcesItemToLibraryTransformer.transform(inputEntity.dependenciesSources.map {
        DependencySourcesAndJavacOptions(
          it,
          inputEntity.javacOptions
        )
      }),
      compilerOutput = toCompilerOutput(inputEntity),
      jvmJdkInfo = toJdkInfo(inputEntity)
    )

  private fun toModule(inputEntity: ModuleDetails): Module {
    val bspModuleDetails = BspModuleDetails(
      target = inputEntity.target,
      allTargetsIds = inputEntity.allTargetsIds,
      dependencySources = inputEntity.dependenciesSources,
      type = type,
      javacOptions = inputEntity.javacOptions,
    )

    return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
  }

  private fun toBaseDirContentRoot(inputEntity: ModuleDetails): ContentRoot =
    ContentRoot(
      // TODO what if null?
      url = URI.create(inputEntity.target.baseDirectory ?: "file:///todo").toPath(),
      excludedPaths = inputEntity.outputPathUris.map { URI.create(it).toPath() },
    )

  private fun toCompilerOutput(inputEntity: ModuleDetails): Path? =
    inputEntity.javacOptions?.classDirectory?.let { URI(it).toPath() }

  private fun toJdkInfo(inputEntity: ModuleDetails): JvmJdkInfo? {
    val jvmBuildTarget = extractJvmBuildTarget(inputEntity.target)
    return if (jvmBuildTarget != null)
      JvmJdkInfo(
        name = jvmBuildTarget.javaVersion.javaVersionToJdkName(projectBasePath.name),
        javaHome = jvmBuildTarget.javaHome
      )
    else null
  }

  companion object {
    private const val type = "JAVA_MODULE"
  }

}

// TODO ugly, but we need to change it anyway
public fun extractJvmBuildTarget(target: BuildTarget): JvmBuildTarget? =
  if (target.dataKind == BuildTargetDataKind.JVM) {
    when (target.data) {
      is JvmBuildTarget -> target.data as JvmBuildTarget
      else -> Gson().fromJson(target.data as JsonObject, JvmBuildTarget::class.java)
    }
  }
  else null

public fun String.javaVersionToJdkName(projectName: String): String = "$projectName-$this"
