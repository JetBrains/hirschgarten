package org.jetbrains.bazel.sync.workspace.languages.scala

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.java.JvmLanguagePluginMixin
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.toWorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.reflect.KClass

@ApiStatus.Internal
class ScalaLanguagePlugin : JvmLanguagePluginMixin {
  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(ScalaBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.SCALA)
  override fun createProjectMapper(project: Project, server: BazelServerFacade): Mapper = Mapper(server)

  class Mapper(private val server: BazelServerFacade) : JvmLanguagePluginMixin.Mapper {

    private var scalaSdks: Map<WorkspaceTargetKey, ScalaSdk> = emptyMap()
    private var scalaTestJars: Map<WorkspaceTargetKey, Set<Path>> = emptyMap()

    override suspend fun prepareSync(
      graph: DependencyGraph,
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ) {
      val localRepositories = repoMapping.getLocalRepositories()
      scalaSdks =
        graph.idToTargetInfo.values
          .associateBy(
            { it.key.toWorkspaceTargetKey() },
            { ScalaSdkResolver(server.bazelPathsResolver).resolveSdk(it, localRepositories) },
          ).filterValuesNotNull()

      scalaTestJars =
        graph.idToTargetInfo.values
          .filter { it.hasScalaTargetInfo() }
          .associateBy(
            { it.key.toWorkspaceTargetKey() },
            { target ->
              target.scalaTargetInfo.scalatestClasspathTargetsList.flatMap {
                graph.idToTargetInfo[target.key.toWorkspaceTargetKey().copy(label = Label.parse(it))]?.javaProvider?.fullCompileJarsList
                ?: emptyList()
              }
                .map { server.bazelPathsResolver.resolve(it, localRepositories) }
                .toSet()
            },
          )
    }

    private fun <K, V> Map<K, V?>.filterValuesNotNull(): Map<K, V> = filterValues { it != null }.mapValues { it.value!! }

    override suspend fun createBuildTargetData(
      target: TargetIdeInfo,
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ): List<BuildTargetData> {
      if (!target.hasScalaTargetInfo()) {
        return emptyList()
      }
      val sdk = scalaSdks[target.key.toWorkspaceTargetKey()] ?: return emptyList()
      return listOf(
        ScalaBuildTarget(
          scalaVersion = sdk.version,
          sdkJars = sdk.compilerJars,
          scalacOptions = target.scalaTargetInfo.scalacOptsList,
        ),
      )
    }

    override suspend fun toolchainLibraries(
      targetsToImport: Map<WorkspaceTargetKey, TargetIdeInfo>,
      repoMapping: RepoMapping,
    ): Map<WorkspaceTargetKey, List<LibraryItem>> {
      val projectLevelScalaSdkLibraries = calculateProjectLevelScalaSdkLibraries()
      val projectLevelScalaTestLibraries = calculateProjectLevelScalaTestLibraries()
      val scalaTargets = targetsToImport.filter { it.value.hasScalaTargetInfo() }.map { it.key }
      return scalaTargets.associateWith {
        val sdkLibraries =
          scalaSdks[it]
            ?.compilerJars
            ?.mapNotNull {
              projectLevelScalaSdkLibraries[it]
            }.orEmpty()
        val testLibraries =
          scalaTestJars[it]
            ?.mapNotNull {
              projectLevelScalaTestLibraries[it]
            }.orEmpty()

        (sdkLibraries + testLibraries).distinct()
      }
    }

    private suspend fun calculateProjectLevelScalaSdkLibraries(): Map<Path, LibraryItem> =
      getProjectLevelScalaSdkLibrariesJars().associateWith {
        createLibrary(key = WorkspaceTargetKey(label = Label.synthetic(it.name)), jar = it)
      }

    private suspend fun calculateProjectLevelScalaTestLibraries(): Map<Path, LibraryItem> {
      return scalaTestJars.values
        .flatten()
        .toSet()
        .associateWith {
          createLibrary(key = WorkspaceTargetKey(label = Label.synthetic(it.name)), jar = it)
        }
    }

    private fun getProjectLevelScalaSdkLibrariesJars(): Set<Path> {
      return scalaSdks.values
        .toSet()
        .flatMap {
          it.compilerJars
        }.toSet()
    }

    private suspend fun createLibrary(
      key: WorkspaceTargetKey,
      jar: Path,
    ): LibraryItem {
      return LibraryItem(
        key = key,
        ijars = emptyList(),
        jars = server.outFileHardLinks.createOutputFileHardLinks(listOf(jar)),
        sourceJars = emptyList(),
        mavenCoordinates = null,
        containsInternalJars = false,
      )
    }
  }
}
