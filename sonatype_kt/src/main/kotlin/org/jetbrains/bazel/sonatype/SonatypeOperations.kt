package org.jetbrains.bazel.sonatype

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.bazel.sonatype.SonatypeClient.StagingRepositoryProfile
import org.sonatype.spice.zapper.Path

class SonatypeOperations(private val config: SonatypeConfig) {

  private val LOG: Logger = LogManager.getLogger(SonatypeOperations::class.java)

  private val sonatypeSplitCoordinates: SonatypeCoordinates = config.coordinates

  private val rest: SonatypeService by lazy {
    setLogLevel(config.logLevel)
    val sonatypeClient = SonatypeClient(
      repositoryUrl = config.repositoryUrl,
      username = config.username,
      password = config.password,
      timeoutMillis = config.timeoutMillis
    )
    SonatypeService(
      sonatypeClient = sonatypeClient,
      profileName = config.profileName
    )
  }

  val sonatypeSessionName: String by lazy {
    config.sessionName ?: "[bazel-sonatype] ${sonatypeSplitCoordinates.sonatypeArtifactId} ${sonatypeSplitCoordinates.sonatypeVersion}"
  }

  val filesPaths: List<Path> = listOf(
    Path(config.projectJar),
    Path(config.projectSourcesJar),
    Path(config.projectDocsJar),
    Path(config.projectPom)
  )

  private fun setLogLevel(logLevel: String) {
    // Implement log level setting if necessary, or rely on log4j2 configuration
    // Possibly adjust log4j2 configuration programmatically
    // For simplicity, skip implementation, assuming log4j2 config is set externally
  }

  private fun withSonatypeService(block: (SonatypeService) -> Unit) {
    try {
      block(rest)
    } finally {
      rest.close()
    }
  }

  fun openRepo() {
    withSonatypeService { service ->
      service.openOrCreateByKey(sonatypeSessionName)
    }
  }

  fun bundleRelease() {
    withSonatypeService { service ->
      val repo = prepare(service)
      service.uploadBundle(
        coordinatesGroupId = sonatypeSplitCoordinates.sonatypeGroupId,
        remoteUrl = repo.deployUrl(config.repositoryUrl),
        filesPaths = filesPaths
      )
      service.closeAndPromote(repo)
    }
  }

  private fun prepare(service: SonatypeService): StagingRepositoryProfile {
    val descriptionKey = sonatypeSessionName
    // Drop a previous staging repository if exists
    service.dropIfExistsByKey(descriptionKey)
    // Create a new staging repository
    return service.createStage(descriptionKey)
  }
}
