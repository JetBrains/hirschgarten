package org.jetbrains.bazel.sonatype

import org.jetbrains.bazel.sonatype.SonatypeClient._
import org.jetbrains.bazel.sonatype.SonatypeException.{MISSING_PROFILE, MISSING_STAGING_PROFILE, MULTIPLE_TARGETS, UNKNOWN_STAGE}
import org.sonatype.spice.zapper.Path
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.log.LogSupport

import java.io.File
import java.nio.file.Files
import scala.io.Source
import scala.util.Try

/** Interface to access the REST API of Nexus
  * @param profileName
  */
class SonatypeService(
    sonatypeClient: SonatypeClient,
    val profileName: String
) extends LogSupport
    with AutoCloseable {
  import SonatypeService._

  info(s"sonatypeRepository  : ${sonatypeClient.repoUri}")
  info(s"sonatypeProfileName : ${profileName}")

  override def close(): Unit = {
    sonatypeClient.close()
  }

  def findTargetRepository(command: CommandType, arg: Option[String]): StagingRepositoryProfile = {
    val repos = command match {
      case Close           => openRepositories
      case Promote         => closedRepositories
      case Drop            => stagingRepositoryProfiles()
      case CloseAndPromote => stagingRepositoryProfiles()
    }
    if (repos.isEmpty) {
      if (stagingProfiles.isEmpty) {
        error(s"No staging profile found for $profileName")
        error("Have you requested a staging profile and successfully published your signed artifact there?")
        throw SonatypeException(MISSING_STAGING_PROFILE, s"No staging profile found for $profileName")
      } else {
        throw new IllegalStateException(command.errNotFound)
      }
    }

    def findSpecifiedInArg(target: String) = {
      repos.find(_.repositoryId == target).getOrElse {
        error(s"Repository $target is not found")
        error(s"Specify one of the repository ids in:\n${repos.mkString("\n")}")
        throw SonatypeException(UNKNOWN_STAGE, s"Repository $target is not found")
      }
    }

    arg.map(findSpecifiedInArg).getOrElse {
      if (repos.size > 1) {
        error(s"Multiple repositories are found:\n${repos.mkString("\n")}")
        error(s"Specify one of the repository ids in the command line or run sonatypeDropAll to cleanup repositories")
        throw SonatypeException(MULTIPLE_TARGETS, "Found multiple staging repositories")
      } else {
        repos.head
      }
    }
  }

  def openRepositories   = stagingRepositoryProfiles().filter(_.isOpen).sortBy(_.repositoryId)
  def closedRepositories = stagingRepositoryProfiles().filter(_.isClosed).sortBy(_.repositoryId)

  def uploadBundle(coordinatesGroupId: String, remoteUrl: String, filesPaths: List[Path]): Unit = {
    sonatypeClient.uploadBundle(
      coordinatesGroupId,
      remoteUrl,
      () => new DirectoryIOSourceMaven(filesPaths)
    )
  }

  def openOrCreateByKey(descriptionKey: String): StagingRepositoryProfile = {
    // Find the already opened profile or create a new one
    val repos = findStagingRepositoryProfilesWithKey(descriptionKey)
    if (repos.size > 1) {
      throw SonatypeException(
        MULTIPLE_TARGETS,
        s"Multiple staging repositories for ${descriptionKey} exists. Run sonatypeDropAll first to clean up old repositories"
      )
    } else if (repos.size == 1) {
      val repo = repos.head
      info(s"Found a staging repository ${repo}")
      repo
    } else {
      // Create a new staging repository by appending [sbt-sonatype] prefix to its description so that we can find the repository id later
      info(s"No staging repository for ${descriptionKey} is found. Create a new one.")
      createStage(descriptionKey)
    }
  }

  def dropIfExistsByKey(descriptionKey: String): Option[StagingRepositoryProfile] = {
    // Drop the staging repository if exists
    val repos = findStagingRepositoryProfilesWithKey(descriptionKey)
    if (repos.isEmpty) {
      info(s"No previous staging repository for ${descriptionKey} was found")
      None
    } else {
      repos.map { repo =>
        info(s"Found a previous staging repository ${repo}")
        dropStage(repo)
      }.lastOption
    }
  }

  def findStagingRepositoryProfilesWithKey(descriptionKey: String): Seq[StagingRepositoryProfile] = {
    stagingRepositoryProfiles(warnIfMissing = false).filter(_.description == descriptionKey)
  }

  def stagingRepositoryProfiles(warnIfMissing: Boolean = true): Seq[StagingRepositoryProfile] = {
    // Note: using /staging/profile_repositories/(profile id) is preferred to reduce the response size,
    // but Sonatype API is quite slow (as of Sep 2019) so using a single request was much better.
    val response   = sonatypeClient.stagingRepositoryProfiles
    val myProfiles = response.filter(_.profileName == profileName)
    if (myProfiles.isEmpty && warnIfMissing) {
      warn(s"No staging repository is found. Do publishSigned first.")
    }
    myProfiles
  }

  private def read(file: File): String = {
    val sourceFile = Source.fromFile(file)
    val content =
      try sourceFile.mkString
      finally sourceFile.close()
    content
  }

  private def write(file: File, content: String): Unit = {
    Files.write(file.toPath, content.getBytes())
  }

  private def withCache[A: scala.reflect.runtime.universe.TypeTag](fileName: String, a: => A): A = {
    val codec     = MessageCodecFactory.defaultFactoryForJSON.of[A]
    val cacheFile = new File(fileName)
    val value: A = if (cacheFile.exists() && cacheFile.length() > 0) {
      Try {
        val json = read(cacheFile)
        codec.fromJson(json)
      }.getOrElse(a)
    } else {
      a
    }
    cacheFile.getParentFile.mkdirs()
    write(cacheFile, codec.toJson(value))
    value
  }

  def stagingProfiles: Seq[StagingProfile] = {
    val profiles = withCache(s"target/sonatype-profile-${profileName}.json", sonatypeClient.stagingProfiles)
    profiles.filter(_.name == profileName)
  }

  lazy val currentProfile: StagingProfile = {
    val profiles = stagingProfiles
    if (profiles.isEmpty) {
      throw SonatypeException(
        MISSING_PROFILE,
        s"Profile ${profileName} is not found. Check your sonatypeProfileName setting in build.sbt"
      )
    }
    profiles.head
  }

  def createStage(description: String = "Requested by sbt-sonatype plugin"): StagingRepositoryProfile = {
    sonatypeClient.createStage(currentProfile, description)
  }

  def closeStage(repo: StagingRepositoryProfile): StagingRepositoryProfile = {
    if (repo.isClosed || repo.isReleased) {
      info(s"Repository ${repo.repositoryId} is already closed")
      repo
    } else {
      sonatypeClient.closeStage(currentProfile, repo)
    }
  }

  def dropStage(repo: StagingRepositoryProfile): StagingRepositoryProfile = {
    sonatypeClient.dropStage(currentProfile, repo)
    info(s"Dropped successfully: ${repo.repositoryId}")
    repo.toDropped
  }

  def promoteStage(repo: StagingRepositoryProfile): StagingRepositoryProfile = {
    if (repo.isReleased) {
      info(s"Repository ${repo.repositoryId} is already released")
    } else {
      // Post promote(release) request
      sonatypeClient.promoteStage(currentProfile, repo)
    }
    dropStage(repo.toReleased)
  }

  def stagingRepositoryInfo(repositoryId: String) = {
    sonatypeClient.stagingRepository(repositoryId)
  }

  def closeAndPromote(repo: StagingRepositoryProfile): StagingRepositoryProfile = {
    if (repo.isReleased) {
      dropStage(repo)
    } else {
      val closed = closeStage(repo)
      promoteStage(closed)
    }
  }

  def activities: Seq[(StagingRepositoryProfile, Seq[StagingActivity])] = {
    for (r <- stagingRepositoryProfiles()) yield r -> sonatypeClient.activitiesOf(r)
  }

}

object SonatypeService {

  /** Switches of a Sonatype command to use
    */
  sealed trait CommandType {
    def errNotFound: String
  }
  case object Close extends CommandType {
    def errNotFound = "No open repository is found. Run publishSigned first"
  }
  case object Promote extends CommandType {
    def errNotFound = "No closed repository is found. Run publishSigned and close commands"
  }
  case object Drop extends CommandType {
    def errNotFound = "No staging repository is found. Run publishSigned first"
  }
  case object CloseAndPromote extends CommandType {
    def errNotFound = "No staging repository is found. Run publishSigned first"
  }

}
