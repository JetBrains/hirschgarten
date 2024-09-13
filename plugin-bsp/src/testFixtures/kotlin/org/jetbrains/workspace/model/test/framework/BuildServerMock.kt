package org.jetbrains.workspace.model.test.framework

import ch.epfl.scala.bsp4j.CleanCacheParams
import ch.epfl.scala.bsp4j.CleanCacheResult
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.CppOptionsParams
import ch.epfl.scala.bsp4j.CppOptionsResult
import ch.epfl.scala.bsp4j.DebugSessionAddress
import ch.epfl.scala.bsp4j.DebugSessionParams
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildParams
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
import ch.epfl.scala.bsp4j.ReadParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.RustWorkspaceParams
import ch.epfl.scala.bsp4j.RustWorkspaceResult
import ch.epfl.scala.bsp4j.ScalaMainClassesParams
import ch.epfl.scala.bsp4j.ScalaMainClassesResult
import ch.epfl.scala.bsp4j.ScalaTestClassesParams
import ch.epfl.scala.bsp4j.ScalaTestClassesResult
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestWithDebugParams
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import java.util.concurrent.CompletableFuture

class BuildServerMock(
  private val initializeBuildResult: InitializeBuildResult? = null,
  private val workspaceBuildTargetsResult: WorkspaceBuildTargetsResult? = null,
  private val sourcesResult: SourcesResult? = null,
  private val inverseSourcesResult: InverseSourcesResult? = null,
  private val dependencySourcesResult: DependencySourcesResult? = null,
  private val dependencyModulesResult: DependencyModulesResult? = null,
  private val resourcesResult: ResourcesResult? = null,
  private val outputPathsResult: OutputPathsResult? = null,
  private val compileResult: CompileResult? = null,
  private val runResult: RunResult? = null,
  private val testResult: TestResult? = null,
  private val debugSessionAddress: DebugSessionAddress? = null,
  private val cleanCacheResult: CleanCacheResult? = null,
  private val jvmTestEnvironmentResult: JvmTestEnvironmentResult? = null,
  private val jvmRunEnvironmentResult: JvmRunEnvironmentResult? = null,
  private val jvmCompileClasspathResult: JvmCompileClasspathResult? = null,
  private val scalacOptionsResult: ScalacOptionsResult? = null,
  private val scalaTestClassesResult: ScalaTestClassesResult? = null,
  private val scalaMainClassesResult: ScalaMainClassesResult? = null,
  private val javacOptionsResult: JavacOptionsResult? = null,
  private val cppOptionsResult: CppOptionsResult? = null,
  private val workspaceLibrariesResult: WorkspaceLibrariesResult? = null,
  private val workspaceGoLibrariesResult: WorkspaceGoLibrariesResult? = null,
  private val workspaceNonModuleTargetsResult: NonModuleTargetsResult? = null,
  private val workspaceDirectoriesResult: WorkspaceDirectoriesResult? = null,
  private val workspaceInvalidTargetsResult: WorkspaceInvalidTargetsResult? = null,
  private val runResultWithDebug: RunResult? = null,
  private val testResultWithDebug: TestResult? = null,
  private val mobileInstallResult: MobileInstallResult? = null,
  private val jvmBinaryJarsResult: JvmBinaryJarsResult? = null,
  private val workspaceBuildTargetsResultAndBuild: WorkspaceBuildTargetsResult? = null,
  private val pythonOptionsResult: PythonOptionsResult? = null,
  private val rustWorkspaceResult: RustWorkspaceResult? = null,
) : JoinedBuildServer {
  override fun buildInitialize(initializeBuildParams: InitializeBuildParams): CompletableFuture<InitializeBuildResult> =
    wrapInFuture(initializeBuildResult)

  override fun onBuildInitialized() { /* it's a mock, nothing to do */ }

  override fun buildShutdown(): CompletableFuture<Any> = wrapInFuture(null)

  override fun onBuildExit() { /* it's a mock, nothing to do */ }

  override fun workspaceBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> = wrapInFuture(workspaceBuildTargetsResult)

  override fun workspaceReload(): CompletableFuture<Any> = wrapInFuture(null)

  override fun buildTargetSources(sourcesParams: SourcesParams): CompletableFuture<SourcesResult> = wrapInFuture(sourcesResult)

  override fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): CompletableFuture<InverseSourcesResult> =
    wrapInFuture(inverseSourcesResult)

  override fun buildTargetDependencySources(dependencySourcesParams: DependencySourcesParams): CompletableFuture<DependencySourcesResult> =
    wrapInFuture(dependencySourcesResult)

  override fun buildTargetDependencyModules(dependencyModulesParams: DependencyModulesParams): CompletableFuture<DependencyModulesResult> =
    wrapInFuture(dependencyModulesResult)

  override fun buildTargetResources(resourcesParams: ResourcesParams): CompletableFuture<ResourcesResult> = wrapInFuture(resourcesResult)

  override fun buildTargetOutputPaths(outputPathsParams: OutputPathsParams): CompletableFuture<OutputPathsResult> =
    wrapInFuture(outputPathsResult)

  override fun buildTargetCompile(compileParams: CompileParams): CompletableFuture<CompileResult> = wrapInFuture(compileResult)

  override fun buildTargetRun(runParams: RunParams): CompletableFuture<RunResult> = wrapInFuture(runResult)

  override fun buildTargetTest(testParams: TestParams): CompletableFuture<TestResult> = wrapInFuture(testResult)

  override fun debugSessionStart(debugSessionParams: DebugSessionParams): CompletableFuture<DebugSessionAddress> =
    wrapInFuture(debugSessionAddress)

  override fun buildTargetCleanCache(cleanCacheParams: CleanCacheParams): CompletableFuture<CleanCacheResult> =
    wrapInFuture(cleanCacheResult)

  override fun onRunReadStdin(p0: ReadParams?) { /* it's a mock, nothing to do */ }

  override fun buildTargetJvmTestEnvironment(
    jvmTestEnvironmentParams: JvmTestEnvironmentParams,
  ): CompletableFuture<JvmTestEnvironmentResult> = wrapInFuture(jvmTestEnvironmentResult)

  override fun buildTargetJvmRunEnvironment(jvmRunEnvironmentParams: JvmRunEnvironmentParams): CompletableFuture<JvmRunEnvironmentResult> =
    wrapInFuture(jvmRunEnvironmentResult)

  override fun buildTargetJvmCompileClasspath(
    jvmCompileClasspathParams: JvmCompileClasspathParams,
  ): CompletableFuture<JvmCompileClasspathResult> = wrapInFuture(jvmCompileClasspathResult)

  override fun buildTargetScalacOptions(scalacOptionsParams: ScalacOptionsParams): CompletableFuture<ScalacOptionsResult> =
    wrapInFuture(scalacOptionsResult)

  override fun buildTargetScalaTestClasses(scalaTestClassesParams: ScalaTestClassesParams): CompletableFuture<ScalaTestClassesResult> =
    wrapInFuture(scalaTestClassesResult)

  override fun buildTargetScalaMainClasses(scalaMainClassesParams: ScalaMainClassesParams): CompletableFuture<ScalaMainClassesResult> =
    wrapInFuture(scalaMainClassesResult)

  override fun buildTargetJavacOptions(javacOptionsParams: JavacOptionsParams): CompletableFuture<JavacOptionsResult> =
    wrapInFuture(javacOptionsResult)

  override fun buildTargetCppOptions(p0: CppOptionsParams?): CompletableFuture<CppOptionsResult> = wrapInFuture(cppOptionsResult)

  override fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult> = wrapInFuture(workspaceLibrariesResult)

  override fun workspaceGoLibraries(): CompletableFuture<WorkspaceGoLibrariesResult> = wrapInFuture(workspaceGoLibrariesResult)

  override fun workspaceNonModuleTargets(): CompletableFuture<NonModuleTargetsResult> = wrapInFuture(workspaceNonModuleTargetsResult)

  override fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult> = wrapInFuture(workspaceDirectoriesResult)

  override fun workspaceInvalidTargets(): CompletableFuture<WorkspaceInvalidTargetsResult> = wrapInFuture(workspaceInvalidTargetsResult)

  override fun buildTargetRunWithDebug(params: RunWithDebugParams): CompletableFuture<RunResult> = wrapInFuture(runResultWithDebug)

  override fun buildTargetTestWithDebug(params: TestWithDebugParams): CompletableFuture<TestResult> = wrapInFuture(testResultWithDebug)

  override fun buildTargetMobileInstall(params: MobileInstallParams): CompletableFuture<MobileInstallResult> =
    wrapInFuture(mobileInstallResult)

  override fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): CompletableFuture<JvmBinaryJarsResult> =
    wrapInFuture(jvmBinaryJarsResult)

  override fun workspaceBuildAndGetBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> =
    wrapInFuture(workspaceBuildTargetsResultAndBuild)

  override fun buildTargetPythonOptions(pythonOptionsParams: PythonOptionsParams?): CompletableFuture<PythonOptionsResult> =
    wrapInFuture(pythonOptionsResult)

  override fun rustWorkspace(p0: RustWorkspaceParams?): CompletableFuture<RustWorkspaceResult> = wrapInFuture(rustWorkspaceResult)

  private fun <T> wrapInFuture(value: T?): CompletableFuture<T> =
    value?.let { CompletableFuture.completedFuture(it) } ?: CompletableFuture.failedFuture(Exception("mock value is null"))
}
