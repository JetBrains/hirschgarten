package org.jetbrains.bsp.performance.testing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.IDEStartResult
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.RemoteArchiveProjectInfo
import com.intellij.ide.starter.runner.CurrentTestMethod
import com.intellij.openapi.util.BuildNumber
import com.intellij.tools.ide.metrics.collector.metrics.PerformanceMetrics
import com.intellij.tools.ide.metrics.collector.publishing.CIServerBuildInfo
import com.intellij.tools.ide.metrics.collector.publishing.PerformanceMetricsDto
import org.kodein.di.direct
import org.kodein.di.instance
import java.nio.file.Files
import java.nio.file.Path

// This file is copied from the monorepo, because these functions aren't yet published to Maven.
/**
 * Copies the files to a temp directory to make sure that the artifacts are published by TeamCity.
 * Otherwise, the files might be cleaned before the TeamCity build finishes.
 */
fun IDEStartResult.publishTeamCityArtifacts(
  source: Path,
  artifactPath: String = runContext.contextName,
  artifactName: String = source.fileName.toString(),
  zipContent: Boolean = true,
) {
  TeamCityClient.publishTeamCityArtifacts(
    source = source,
    artifactPath = artifactPath,
    artifactName = artifactName,
    zipContent = zipContent
  )
}

fun IDEStartResult.publishPerformanceMetrics(
  artifactPath: String = runContext.contextName,
  artifactName: String = "metrics.performance.json",
  projectName: String = runContext.contextName,
  metrics: Collection<PerformanceMetrics.Metric>,
) {
  val buildInfo: CIServerBuildInfo = CIServer.instance.asTeamCity().run {
    val buildTypeId = this.buildTypeId ?: ""

    CIServerBuildInfo(
      buildId = this.buildId,
      typeId = buildTypeId,
      configName = this.configurationName ?: "",
      buildNumber = this.buildNumber,
      branchName = this.branchName,
      url = "${this.serverUri}/viewLog.html?buildId=$buildId&buildTypeId=$buildTypeId",
      isPersonal = this.isPersonalBuild,
    )
  }

  val metricsSortedByName = metrics.sortedBy { it.id.name }

  val appMetrics = PerformanceMetricsDto.create(
    projectName = projectName,
    buildNumber = BuildNumber.fromStringWithProductCode(context.ide.build, context.ide.productCode)!!,
    methodName = getMethodName(),
    projectURL = context.getProjectURL(),
    projectDescription = context.getProjectDescription(),
    metrics = metricsSortedByName,
    buildInfo = buildInfo,
    generated = CIServer.instance.asTeamCity().buildStartTime
  )

  val reportFile = Files.createTempFile(runContext.snapshotsDir, artifactName, null)
  jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), appMetrics)
  if (CIServer.instance.asTeamCity().buildId == TeamCityCIServer.LOCAL_RUN_ID) {
    println(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(appMetrics))
  }
  publishTeamCityArtifacts(reportFile, zipContent = false, artifactPath = artifactPath, artifactName = artifactName)
}

private fun getMethodName(): String {
  val method = di.direct.instance<CurrentTestMethod>().get()
  return if (method == null) "" else "${method.clazz}#${method.name}"
}

private fun IDETestContext.getProjectDescription(): String = testCase.projectInfo.getDescription()

private fun IDETestContext.getProjectURL(): String {
  val url = when (val projectInfo = this.testCase.projectInfo) {
    is RemoteArchiveProjectInfo -> projectInfo.projectURL
    is GitProjectInfo -> projectInfo.repositoryUrl
    else -> ""
  }
  return url
}
