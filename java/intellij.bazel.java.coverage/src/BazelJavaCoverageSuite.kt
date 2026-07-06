package org.jetbrains.bazel.java.coverage

import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.JavaCoverageSuite
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.components.service
import com.intellij.openapi.module.impl.scopes.MODULES
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScopeCache
import com.intellij.openapi.module.impl.scopes.TESTS
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.target.ModuleTargetService

internal class BazelJavaCoverageSuite : JavaCoverageSuite {
  constructor(coverageEngine: CoverageEngine) : super(coverageEngine)

  constructor(
    name: String?,
    coverageDataFileProvider: CoverageFileProvider?,
    includeFilters: Array<String>?,
    excludePatterns: Array<String>?,
    lastCoverageTimeStamp: Long,
    coverageByTestEnabled: Boolean,
    branchCoverage: Boolean,
    trackTestFolders: Boolean,
    coverageRunner: CoverageRunner?,
    coverageEngine: CoverageEngine,
    project: Project?,
  ) : super(
    name,
    coverageDataFileProvider,
    includeFilters,
    excludePatterns,
    lastCoverageTimeStamp,
    coverageByTestEnabled,
    branchCoverage,
    trackTestFolders,
    coverageRunner,
    coverageEngine,
    project,
  )

  init {
    /**
     * To show uncovered classes in the coverage results, the platform needs to scan all the output jars (e.g., via OrderEnumerationHandler).
     * Not only is this slow for big monorepos, we need to make sure these jars are refreshed in the VFS (and we don't want to do it with bazel-out).
     * So while theoretically possible, it's not really worth the performance hit.
     */
    isSkipUnloadedClassesAnalysis = true
  }

  /**
   * [com.intellij.coverage.BaseCoverageSuite.getSearchScope] assumes a configuration is an instance of [ModuleBasedConfiguration].
   * [BazelRunConfiguration] isn't, hence a custom implementation here to get the module for limiting the search scope.
   */
  override fun getSearchScope(project: Project): GlobalSearchScope {
    val configuration = getConfiguration()
    val scope = if (isTrackTestFolders) {
      GlobalSearchScope.projectScope(project)
    }
    else {
      GlobalSearchScopesCore.projectProductionScope(project)
    }

    val module = (configuration as? BazelRunConfiguration)
                   ?.targets
                   ?.singleOrNull()
                   ?.let { project.service<ModuleTargetService>().findLegacyModulesByLabel(label = it).firstOrNull() }
                 ?: return scope
    val cache = project.service<ModuleWithDependenciesScopeCache>()
    // Follow both runtime and compile dependencies, but don't add libraries
    val moduleScope = cache.getCachedScope(module, TESTS or MODULES)
    return moduleScope.intersectWith(scope)
  }
}
