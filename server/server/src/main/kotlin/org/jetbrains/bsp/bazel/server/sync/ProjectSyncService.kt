package org.jetbrains.bsp.bazel.server.sync

import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target;

import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CppOptionsParams
import ch.epfl.scala.bsp4j.CppOptionsResult
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildResult
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JavacOptionsResult
import ch.epfl.scala.bsp4j.JvmCompileClasspathParams
import ch.epfl.scala.bsp4j.JvmCompileClasspathResult
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.PythonOptionsResult
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.RustWorkspaceParams
import ch.epfl.scala.bsp4j.RustWorkspaceResult
import ch.epfl.scala.bsp4j.ScalaMainClassesParams
import ch.epfl.scala.bsp4j.ScalaMainClassesResult
import ch.epfl.scala.bsp4j.ScalaTestClassesParams
import ch.epfl.scala.bsp4j.ScalaTestClassesResult
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsResult
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.server.bep.BepServer
import org.jetbrains.bsp.bazel.server.bsp.managers.BepReader
import org.jetbrains.bsp.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.bazel.server.model.Language
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.languages.JVMLanguagePluginParser
import org.jetbrains.bsp.protocol.EnhancedSourceItem
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import java.nio.file.Path
import kotlin.collections.firstOrNull
import kotlin.io.path.Path
import kotlin.io.path.exists

/** A facade for all project sync related methods  */
class ProjectSyncService(
  private val bspMapper: BspProjectMapper,
  private val projectProvider: ProjectProvider,
  private val clientCapabilities: BuildClientCapabilities,
  private val bazelRunner: BazelRunner,
  private val client: JoinedBuildClient,
  private val bazelPathsResolver: BazelPathsResolver,
  private val workspaceRoot: Path,
) {
  fun initialize(): InitializeBuildResult = bspMapper.initializeServer(Language.all())

  // TODO https://youtrack.jetbrains.com/issue/BAZEL-639
  // We might consider doing the actual project reload in this endpoint
  // i.e. just run projectProvider.refreshAndGet() and in workspaceBuildTargets
  // just run projectProvider.get() although current approach seems to work
  // correctly, so I am not changing anything.
  fun workspaceReload(cancelChecker: CancelChecker): Any = Any()

  fun workspaceBuildTargets(cancelChecker: CancelChecker, build: Boolean): WorkspaceBuildTargetsResult {
    if (build) {
      val project = projectProvider.refreshAndGet(cancelChecker, build = build)
      return bspMapper.workspaceTargets(project)
    }

    val project = projectProvider.qqsync(cancelChecker, bazelRunner, workspaceRoot, bazelPathsResolver, client)


    val a = project.qqsync.map {
      BuildTarget(
        BuildTargetIdentifier(it.rule.name),
        emptyList(),
        listOf("java"),
        it.rule.attributeList.firstOrNull { it.name == "deps" }?.stringListValueList?.map { BuildTargetIdentifier(it) } ?: emptyList(),
        BuildTargetCapabilities(),
      )
    }

    return WorkspaceBuildTargetsResult(a)
  }

  fun workspaceBuildTargetsPartial(cancelChecker: CancelChecker, targetsToSync: List<BuildTargetIdentifier>): WorkspaceBuildTargetsResult {
    val project =
      projectProvider.updateAndGet(
        cancelChecker = cancelChecker,
        targetsToSync = targetsToSync,
      )
    return bspMapper.workspaceTargets(project)
  }

  fun workspaceBuildLibraries(cancelChecker: CancelChecker): WorkspaceLibrariesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.workspaceLibraries(project)
  }

  fun workspaceBuildGoLibraries(cancelChecker: CancelChecker): WorkspaceGoLibrariesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.workspaceGoLibraries(project)
  }

  fun workspaceNonModuleTargets(cancelChecker: CancelChecker): NonModuleTargetsResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.workspaceNonModuleTargets(project)
  }

  fun workspaceDirectories(cancelChecker: CancelChecker): WorkspaceDirectoriesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.workspaceDirectories(project)
  }

  fun workspaceInvalidTargets(cancelChecker: CancelChecker): WorkspaceInvalidTargetsResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.workspaceInvalidTargets(project)
  }

  fun buildTargetSources(cancelChecker: CancelChecker, sourcesParams: SourcesParams): SourcesResult {
    val project = projectProvider.get(cancelChecker)
    if (project.modules.isNotEmpty()) return bspMapper.sources(project, sourcesParams)
    val pp = JVMLanguagePluginParser
//    return bspMapper.sources(project, sourcesParams)
    println(project.qqsync.size)
    val a = project.qqsync
      .filter { aa ->
        aa.rule.name in sourcesParams.targets.map { it.uri }
      }.map { a1 ->
        val yy = a1.rule.attributeList.firstOrNull { it.name == "srcs" }?.stringListValueList?.map { a2 ->
          val tt = Path(a2.replace(':', '/').trim('/'))
          workspaceRoot.resolve(tt)
        }?.filter { it.exists() }
          ?.map {ttt ->

            pp.calculateJVMSourceRootAndAdditionalData(ttt)
          }
      SourcesItem(
        BuildTargetIdentifier(a1.rule.name),
        a1.rule.attributeList.firstOrNull { it.name == "srcs" }?.stringListValueList?.map { a2 ->
          val tt = Path(a2.replace(':', '/').trim('/'))
          workspaceRoot.resolve(tt)
        }?.filter { it.exists() }
          ?.map {ttt ->

          val r = pp.calculateJVMSourceRootAndAdditionalData(ttt)
          EnhancedSourceItem(ttt.toUri().toString(), SourceItemKind.FILE, false, r.data)
        } ?: emptyList()
      ).apply {
        roots = (yy?.map { it.sourceRoot.toUri().toString() } ?: emptyList()) + (yy?.map { it.sourceRoot.toUri().toString() + "test" } ?: emptyList())
      }
    }

    return SourcesResult(a)
  }

  fun buildTargetResources(cancelChecker: CancelChecker, resourcesParams: ResourcesParams): ResourcesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.resources(project, resourcesParams)
  }

  fun buildTargetInverseSources(cancelChecker: CancelChecker, inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.inverseSources(project, inverseSourcesParams, cancelChecker)
  }

  fun buildTargetDependencySources(
    cancelChecker: CancelChecker,
    dependencySourcesParams: DependencySourcesParams,
  ): DependencySourcesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.dependencySources(project, dependencySourcesParams)
  }

  fun buildTargetOutputPaths(cancelChecker: CancelChecker, params: OutputPathsParams): OutputPathsResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.outputPaths(project, params)
  }

  fun jvmRunEnvironment(cancelChecker: CancelChecker, params: JvmRunEnvironmentParams): JvmRunEnvironmentResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.jvmRunEnvironment(project, params, cancelChecker)
  }

  fun jvmTestEnvironment(cancelChecker: CancelChecker, params: JvmTestEnvironmentParams): JvmTestEnvironmentResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.jvmTestEnvironment(project, params, cancelChecker)
  }

  fun jvmBinaryJars(cancelChecker: CancelChecker, params: JvmBinaryJarsParams): JvmBinaryJarsResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.jvmBinaryJars(project, params)
  }

  fun jvmCompileClasspath(cancelChecker: CancelChecker, params: JvmCompileClasspathParams): JvmCompileClasspathResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.jvmCompileClasspath(project, params, cancelChecker)
  }

  fun buildTargetJavacOptions(cancelChecker: CancelChecker, params: JavacOptionsParams): JavacOptionsResult {
    val project = projectProvider.get(cancelChecker)
    val includeClasspath = clientCapabilities.jvmCompileClasspathReceiver == false
    return bspMapper.buildTargetJavacOptions(project, params, includeClasspath, cancelChecker)
  }

  fun buildTargetCppOptions(cancelChecker: CancelChecker, params: CppOptionsParams): CppOptionsResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.buildTargetCppOptions(project, params)
  }

  fun buildTargetPythonOptions(cancelChecker: CancelChecker, params: PythonOptionsParams): PythonOptionsResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.buildTargetPythonOptions(project, params)
  }

  fun buildTargetScalacOptions(cancelChecker: CancelChecker, params: ScalacOptionsParams): ScalacOptionsResult {
    val project = projectProvider.get(cancelChecker)
    val includeClasspath = clientCapabilities.jvmCompileClasspathReceiver == false
    return bspMapper.buildTargetScalacOptions(project, params, includeClasspath, cancelChecker)
  }

  fun buildTargetScalaTestClasses(cancelChecker: CancelChecker, params: ScalaTestClassesParams): ScalaTestClassesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.buildTargetScalaTestClasses(project, params)
  }

  fun buildTargetScalaMainClasses(cancelChecker: CancelChecker, params: ScalaMainClassesParams): ScalaMainClassesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.buildTargetScalaMainClasses(project, params)
  }

  fun buildTargetDependencyModules(cancelChecker: CancelChecker, params: DependencyModulesParams): DependencyModulesResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.buildDependencyModules(project, params)
  }

  fun rustWorkspace(cancelChecker: CancelChecker, params: RustWorkspaceParams): RustWorkspaceResult {
    val project = projectProvider.get(cancelChecker)
    return bspMapper.rustWorkspace(project, params)
  }
}
