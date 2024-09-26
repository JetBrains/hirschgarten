package org.jetbrains.bazel.sonatype

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.bazel.sonatype.SonatypeClient.StagingActivity
import org.jetbrains.bazel.sonatype.SonatypeClient.StagingProfile
import org.jetbrains.bazel.sonatype.SonatypeClient.StagingRepositoryProfile
import org.sonatype.spice.zapper.Path

class SonatypeService(
  private val sonatypeClient: SonatypeClient,
  private val profileName: String
) : AutoCloseable {

  private val LOG: Logger = LogManager.getLogger(SonatypeService::class.java)

  override fun close() {
    sonatypeClient.close()
  }

  fun findTargetRepository(command: CommandType, arg: String?): StagingRepositoryProfile {
    val repos = when (command) {
      CommandType.Close -> openRepositories()
      CommandType.Promote -> closedRepositories()
      CommandType.Drop, CommandType.CloseAndPromote -> stagingRepositoryProfiles()
    }

    if (repos.isEmpty()) {
      if (stagingProfiles().isEmpty()) {
        LOG.error("No staging profile found for $profileName")
        LOG.error("Have you requested a staging profile and successfully published your signed artifact there?")
        throw SonatypeException(SonatypeErrorCode.MISSING_STAGING_PROFILE, "No staging profile found for $profileName")
      } else {
        throw IllegalStateException(command.errNotFound)
      }
    }

    fun findSpecifiedInArg(target: String): StagingRepositoryProfile {
      return repos.find { it.repositoryId == target } ?: run {
        LOG.error("Repository $target is not found")
        LOG.error("Specify one of the repository ids in:\n${repos.joinToString("\n")}")
        throw SonatypeException(SonatypeErrorCode.UNKNOWN_STAGE, "Repository $target is not found")
      }
    }

    return arg?.let { findSpecifiedInArg(it) } ?: run {
      when {
        repos.size > 1 -> {
          LOG.error("Multiple repositories are found:\n${repos.joinToString("\n")}")
          LOG.error("Specify one of the repository ids in the command line or run sonatypeDropAll to cleanup repositories")
          throw SonatypeException(SonatypeErrorCode.MULTIPLE_TARGETS, "Found multiple staging repositories")
        }
        else -> repos.first()
      }
    }
  }

  fun openRepositories(): List<StagingRepositoryProfile> =
    stagingRepositoryProfiles().filter { it.isOpen() }.sortedBy { it.repositoryId }

  fun closedRepositories(): List<StagingRepositoryProfile> =
    stagingRepositoryProfiles().filter { it.isClosed() }.sortedBy { it.repositoryId }

  fun uploadBundle(coordinatesGroupId: String, remoteUrl: String, filesPaths: List<Path>) {
    sonatypeClient.uploadBundle(coordinatesGroupId, remoteUrl) { DirectoryIOSourceMaven(filesPaths) }
  }

  fun openOrCreateByKey(descriptionKey: String): StagingRepositoryProfile {
    val repos = findStagingRepositoryProfilesWithKey(descriptionKey)
    return when {
      repos.size > 1 -> throw SonatypeException(
        SonatypeErrorCode.MULTIPLE_TARGETS,
        "Multiple staging repositories for $descriptionKey exists. Run sonatypeDropAll first to clean up old repositories"
      )
      repos.size == 1 -> {
        val repo = repos.first()
        LOG.info("Found a staging repository $repo")
        repo
      }
      else -> {
        LOG.info("No staging repository for $descriptionKey is found. Create a new one.")
        createStage(descriptionKey)
      }
    }
  }

  fun dropIfExistsByKey(descriptionKey: String): StagingRepositoryProfile? {
    val repos = findStagingRepositoryProfilesWithKey(descriptionKey)
    return if (repos.isEmpty()) {
      LOG.info("No previous staging repository for $descriptionKey was found")
      null
    } else {
      repos.forEach { repo ->
        LOG.info("Found a previous staging repository $repo")
        dropStage(repo)
      }
      repos.lastOrNull()
    }
  }

  fun findStagingRepositoryProfilesWithKey(descriptionKey: String): List<StagingRepositoryProfile> {
    return stagingRepositoryProfiles(warnIfMissing = false).filter { it.description == descriptionKey }
  }

  fun stagingRepositoryProfiles(warnIfMissing: Boolean = true): List<StagingRepositoryProfile> {
    val response = sonatypeClient.stagingRepositoryProfiles()
    val myProfiles = response.filter { it.profileName == profileName }
    if (myProfiles.isEmpty() && warnIfMissing) {
      LOG.warn("No staging repository is found. Do publishSigned first.")
    }
    return myProfiles
  }

  fun stagingProfiles(): List<StagingProfile> {
    return sonatypeClient.stagingProfiles()
  }

  fun currentProfile(): StagingProfile {
    val profiles = stagingProfiles().filter { it.name == profileName }
    if (profiles.isEmpty()) {
      throw SonatypeException(
        SonatypeErrorCode.MISSING_PROFILE,
        "Profile $profileName is not found. Check your sonatypeProfileName setting."
      )
    }
    return profiles.first()
  }

  fun createStage(description: String = "Requested by sbt-sonatype plugin"): StagingRepositoryProfile {
    return sonatypeClient.createStage(currentProfile(), description)
  }

  fun closeStage(repo: StagingRepositoryProfile): StagingRepositoryProfile {
    return if (repo.isClosed() || repo.isReleased()) {
      LOG.info("Repository ${repo.repositoryId} is already closed")
      repo
    } else {
      sonatypeClient.closeStage(currentProfile(), repo)
    }
  }

  fun dropStage(repo: StagingRepositoryProfile): StagingRepositoryProfile {
    sonatypeClient.dropStage(currentProfile(), repo)
    LOG.info("Dropped successfully: ${repo.repositoryId}")
    return repo.toDropped()
  }

  fun promoteStage(repo: StagingRepositoryProfile): StagingRepositoryProfile {
    return if (repo.isReleased()) {
      LOG.info("Repository ${repo.repositoryId} is already released")
      repo
    } else {
      sonatypeClient.promoteStage(currentProfile(), repo)
      dropStage(repo.toReleased())
    }
  }

  fun stagingRepositoryInfo(repositoryId: String): String {
    return sonatypeClient.stagingRepository(repositoryId)
  }

  fun closeAndPromote(repo: StagingRepositoryProfile): StagingRepositoryProfile {
    return if (repo.isReleased()) {
      dropStage(repo)
    } else {
      val closed = closeStage(repo)
      promoteStage(closed)
    }
  }

  fun activities(): List<Pair<StagingRepositoryProfile, List<StagingActivity>>> {
    return stagingRepositoryProfiles().map { repo ->
      repo to sonatypeClient.activitiesOf(repo)
    }
  }
}
