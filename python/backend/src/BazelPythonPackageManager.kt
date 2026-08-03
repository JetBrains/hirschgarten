package com.intellij.bazel.python.backend

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.python.pyproject.PyDependencyGroup
import com.intellij.python.requirements.PyPackageVersion
import com.jetbrains.python.Result
import com.jetbrains.python.errorProcessing.MessageError
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.common.PythonOutdatedPackage
import com.jetbrains.python.packaging.common.PythonPackage
import com.jetbrains.python.packaging.common.PythonPackageDetails
import com.jetbrains.python.packaging.common.PythonRepositoryPackageSpecification
import com.jetbrains.python.packaging.management.PyWorkspaceMember
import com.jetbrains.python.packaging.management.PythonPackageInstallRequest
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.PythonPackageManagerProvider
import com.jetbrains.python.packaging.management.PythonRepositoryManager
import com.jetbrains.python.packaging.repository.PyPackageRepository
import org.jetbrains.bazel.config.isBazelProject
import java.nio.file.Path

internal class BazelPythonPackageManagerProvider : PythonPackageManagerProvider {
  override fun createPackageManagerForSdk(
    project: Project,
    sdk: Sdk,
  ): PythonPackageManager? {
    if (!project.isBazelProject) return null
    return BazelPythonPackageManager(project, sdk)
  }
}

// TODO: fetch the list of installed Python packages properly from Bazel.
//  For now this just hardcodes pytest to make run gutters work
private class BazelPythonPackageManager(project: Project, sdk: Sdk) : PythonPackageManager(project, sdk) {
  private companion object {
    val PYTEST = PythonPackage("pytest", "1.0.0", false)
  }

  init {
    installedPackages = listOf(PYTEST)
  }

  override val dependenciesFilesRelativePaths: List<Path>
    get() = emptyList()

  override val repositoryManager: PythonRepositoryManager
    get() = BazelPythonRepositoryManager(project)

  override suspend fun syncLockedCommand(): PyResult<Unit> = Result.Success(Unit)

  override suspend fun installPackageCommand(
    installRequest: PythonPackageInstallRequest,
    options: List<String>,
    module: Module?,
    dependencyGroup: PyDependencyGroup?,
  ): PyResult<Unit> = Result.Success(Unit)

  override suspend fun updatePackageCommand(vararg specifications: PythonRepositoryPackageSpecification): PyResult<Unit> =
    Result.Success(Unit)

  override suspend fun uninstallPackageCommand(
    vararg pythonPackages: String,
    workspaceMember: PyWorkspaceMember?,
    dependencyGroup: PyDependencyGroup?,
  ): PyResult<Unit> = Result.Success(Unit)

  override suspend fun loadPackagesCommand(): PyResult<List<PythonPackage>> {
    return Result.Success(listOf(PYTEST))
  }

  override suspend fun loadOutdatedPackagesCommand(): PyResult<List<PythonOutdatedPackage>> =
    Result.Success(emptyList())
}

private class BazelPythonRepositoryManager(
  override val project: Project,
) : PythonRepositoryManager {
  override val allRepositories: List<PyPackageRepository>
    get() = emptyList()

  override suspend fun getPackageDetails(
    packageName: String,
    repository: PyPackageRepository?,
  ): PyResult<PythonPackageDetails> =
    @Suppress("HardCodedStringLiteral")
    Result.Failure(MessageError("Not implemented"))

  override suspend fun getLatestVersion(
    packageName: String,
    repository: PyPackageRepository?,
  ): PyPackageVersion? = null

  override suspend fun getVersions(
    packageName: String,
    repository: PyPackageRepository?,
  ): List<String>? = null

  override suspend fun refreshCaches(): Result<Unit, PythonRepositoryManager.PythonRepositoryIOError> = Result.Success(Unit)

  override suspend fun initCaches(): Result<Unit, PythonRepositoryManager.PythonRepositoryIOError> = Result.Success(Unit)

  override suspend fun findPackageSpecification(
    requirement: PyRequirement,
    repository: PyPackageRepository?,
  ): PythonRepositoryPackageSpecification? = null
}
