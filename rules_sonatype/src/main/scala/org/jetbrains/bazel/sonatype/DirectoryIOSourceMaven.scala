package org.jetbrains.bazel.sonatype

import org.sonatype.spice.zapper.fs.DirectoryIOSource
import org.sonatype.spice.zapper.{Path, ZFile}

import java.io.{BufferedWriter, File, IOException, OutputStreamWriter}
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.security.{MessageDigest, NoSuchAlgorithmException}
import java.util
import java.util.logging.Logger
import scala.jdk.CollectionConverters._

class DirectoryIOSourceMaven(filesPaths: List[Path]) extends DirectoryIOSource(new File("").getCanonicalFile) {

  private val LOG = Logger.getLogger(classOf[DirectoryIOSourceMaven].getName)

  def processFiles(filesPaths: List[Path]): List[Path] =
    filesPaths
      .map(file => Paths.get(file.stringValue()))
      .flatMap(file => {
        val signed = Paths.get(s"${file.toString}.asc")
        sign(file, signed)
        List(file, signed)
      })
      .flatMap(file => {
        val toHash   = Files.readAllBytes(file)
        val md5Path  = Paths.get(s"${file.toString}.md5")
        val sha1Path = Paths.get(s"${file.toString}.sha1")
        Files.deleteIfExists(md5Path)
        Files.deleteIfExists(sha1Path)
        val md5  = Files.createFile(md5Path)
        val sha1 = Files.createFile(sha1Path)
        Files.write(md5, toMd5(toHash).getBytes(StandardCharsets.UTF_8))
        Files.write(sha1, toSha1(toHash).getBytes(StandardCharsets.UTF_8))
        List(file, md5, sha1)
      })
      .map(file => new Path(file.toString))

  override def scanDirectory(dir: File, zfiles: util.List[ZFile]): Int = {
    val processedFiles = processFiles(filesPaths)
    val files = processedFiles.map(createZFile).asJava

    zfiles.addAll(files)
    0
  }

  private def toSha1(toHash: Array[Byte]) = toHexS("%040x", "SHA-1", toHash)

  private def toMd5(toHash: Array[Byte]) = toHexS("%032x", "MD5", toHash)

  private def toHexS(fmt: String, algorithm: String, toHash: Array[Byte]) = try {
    val digest = MessageDigest.getInstance(algorithm)
    digest.update(toHash)
    String.format(fmt, new BigInteger(1, digest.digest))
  } catch {
    case e: NoSuchAlgorithmException =>
      throw new RuntimeException(e)
  }

  @throws[IOException]
  @throws[InterruptedException]
  private def sign(toSign: java.nio.file.Path, signed: java.nio.file.Path): Unit = {
    // Ideally, we'd use BouncyCastle for this, but for now brute force by assuming
    // the gpg binary is on the path
    import scala.sys.process._

    val proclog = ProcessLogger.apply(LOG.info, LOG.warning)
    val io = BasicIO(withIn = false, proclog).withInput { out =>
      val writer = new BufferedWriter(new OutputStreamWriter(out))
      writer.write(sys.env.getOrElse("GPG_PASSPHRASE", "''"))
      writer.flush()
      writer.close()
    }

    val gpgSign = Seq(
      "gpg",
      "--verbose",
      "--verbose",
      "--use-agent",
      "--armor",
      "--detach-sign",
      "--batch",
      "--passphrase-fd",
      "0",
      "--no-tty",
      "-o",
      signed.toAbsolutePath.toString,
      toSign.toAbsolutePath.toString
    ).run(io)
    if (gpgSign.exitValue() != 0) throw SigningException("Unable to sign: " + toSign)

    // Verify the signature
    val gpgVerify = Seq(
      "gpg",
      "--verify",
      "--verbose",
      "--verbose",
      signed.toAbsolutePath.toString,
      toSign.toAbsolutePath.toString
    ).run(proclog)
    if (gpgVerify.exitValue() != 0) throw SigningException("Unable to verify signature of " + toSign)
  }
}

final case class SigningException(message: String) extends IOException(message)

package org.jetbrains.bazel.sonatype

import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.jetbrains.bazel.sonatype.SonatypeClient.{ActivityMonitor, CreateStageResponse, StageTransitionRequest, StagingActivity, StagingProfile, StagingProfileResponse, StagingRepositoryProfile}
import org.jetbrains.bazel.sonatype.SonatypeException._
import org.sonatype.spice.zapper.ParametersBuilder
import org.sonatype.spice.zapper.client.hc4.Hc4ClientBuilder
import org.sonatype.spice.zapper.fs.DirectoryIOSource
import wvlet.airframe.control.{Control, ResultClass, Retry}
import wvlet.airframe.http.HttpHeader.MediaType
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.airframe.http.client.{URLConnectionClient, URLConnectionClientConfig}
import wvlet.log.LogSupport

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class SonatypeClient(
    repositoryUrl: String,
    username: String,
    password: String,
    timeoutMillis: Int = 60 * 60 * 1000
) extends AutoCloseable
    with LogSupport {
  private val base64Credentials =
    Base64.getEncoder.encodeToString(s"$username:$password".getBytes(StandardCharsets.UTF_8))

  lazy val repoUri: String = {
    def repoBase(url: String) = if (url.endsWith("/")) url.dropRight(1) else url
    val url                   = repoBase(repositoryUrl)
    url
  }
  private val pathPrefix = {
    new java.net.URL(repoUri).getPath
  }

  private object SonatypeClientBackend extends HttpClientBackend {
    def newSyncClient(serverAddress: String, clientConfig: HttpClientConfig): HttpSyncClient[Request, Response] = {
      new URLConnectionClient(
        ServerAddress(serverAddress),
        URLConnectionClientConfig(
          requestFilter = clientConfig.requestFilter,
          retryContext = clientConfig.retryContext,
          codecFactory = clientConfig.codecFactory,
          readTimeout = Duration(timeoutMillis, TimeUnit.MILLISECONDS)
        )
      )
    }
  }

  private val clientConfig = HttpClientConfig(SonatypeClientBackend)
    // airframe-http will retry the request several times within this timeout duration.
    .withRetryContext { context =>
      // For individual REST calls, use a normal jittering
      context
        .withMaxRetry(15)
        .withJitter(initialIntervalMillis = 1500, maxIntervalMillis = 30000)
    }
    .withRequestFilter { request =>
      request.withContentTypeJson
        .withAccept(MediaType.ApplicationJson)
        .withHeader(HttpHeader.Authorization, s"Basic $base64Credentials")
    }

  private val httpClient = clientConfig.newSyncClient(repoUri)

  // Create stage is not idempotent, so we just need to wait for a long time without retry
  private val httpClientForCreateStage =
    clientConfig
      .withRetryContext(_.noRetry)
      .newSyncClient(repoUri)

  override def close(): Unit = {
    Control.closeResources(httpClient, httpClientForCreateStage)
  }
  def stagingRepositoryProfiles: Seq[StagingRepositoryProfile] = {
    info("Reading staging repository profiles...")
    val result =
      httpClient.get[Map[String, Seq[StagingRepositoryProfile]]](s"$pathPrefix/staging/profile_repositories")
    result.getOrElse("data", Seq.empty)
  }

  def stagingRepository(repositoryId: String): String = {
    info(s"Searching for repository $repositoryId ...")
    httpClient.get[String](s"$pathPrefix/staging/repository/$repositoryId")
  }

  def stagingProfiles: Seq[StagingProfile] = {
    info("Reading staging profiles...")
    val result = httpClient.get[StagingProfileResponse](s"$pathPrefix/staging/profiles")
    result.data
  }

  def createStage(profile: StagingProfile, description: String): StagingRepositoryProfile = {
    info(s"Creating a staging repository in profile ${profile.name} with a description key: $description")
    val ret = httpClientForCreateStage.postOps[Map[String, Map[String, String]], CreateStageResponse](
      s"$pathPrefix/staging/profiles/${profile.id}/start",
      Map("data" -> Map("description" -> description))
    )
    // Extract created staging repository ids
    val repo = StagingRepositoryProfile(
      profile.id,
      profile.name,
      "open",
      repositoryId = ret.data.stagedRepositoryId,
      ret.data.description
    )
    info(s"Created successfully: ${repo.repositoryId}")
    repo
  }

  private val monitor = new ActivityMonitor()

  /** backoff retry (max 15 sec. / each http request) until the timeout reaches (upto 60 min by default)
    */
  private val retryer = {
    val maxInterval  = 15000
    val initInterval = 3000
    // init * (multiplier ^ n) = max
    // n = log(max / init) / log(multiplier)
    val retryCountUntilMaxInterval = (math.log(maxInterval.toDouble / initInterval) / math.log(1.5)).toInt.max(1)
    val numRetry                   = (timeoutMillis.toFloat / maxInterval).ceil.toInt
    Retry.withBackOff(
      maxRetry = retryCountUntilMaxInterval + numRetry,
      initialIntervalMillis = initInterval,
      maxIntervalMillis = maxInterval
    )
  }

  private def waitForStageCompletion(
      taskName: String,
      repo: StagingRepositoryProfile,
      terminationCond: StagingActivity => Boolean
  ): StagingRepositoryProfile = {
    retryer
      .beforeRetry { ctx =>
        ctx.lastError match {
          case SonatypeException(STAGE_IN_PROGRESS, msg) =>
            info(f"$msg Waiting for ${ctx.nextWaitMillis / 1000.0}%.2f sec.")
          case _ =>
            warn(
              f"[${ctx.retryCount}/${ctx.maxRetry}] Execution failed: ${ctx.lastError.getMessage}. Retrying in ${ctx.nextWaitMillis / 1000.0}%.2f sec."
            )
        }
      }
      .withResultClassifier[Option[StagingActivity]] {
        case Some(activity) if terminationCond(activity) =>
          info(s"[$taskName] Finished successfully")
          ResultClass.Succeeded
        case Some(activity) if activity.containsError =>
          error(s"[$taskName] Failed")
          activity.reportFailure()
          ResultClass.nonRetryableFailure(SonatypeException(STAGE_FAILURE, s"Failed to $taskName the repository."))
        case _ =>
          ResultClass.retryableFailure(SonatypeException(STAGE_IN_PROGRESS, s"The $taskName stage is in progress."))
      }
      .run {
        val activities = activitiesOf(repo)
        monitor.report(activities)
        activities.lastOption
      }

    repo
  }

  def closeStage(currentProfile: StagingProfile, repo: StagingRepositoryProfile): StagingRepositoryProfile = {
    info(s"Closing staging repository $repo")
    val ret = httpClient.postOps[Map[String, StageTransitionRequest], Response](
      s"$pathPrefix/staging/profiles/${repo.profileId}/finish",
      newStageTransitionRequest(currentProfile, repo)
    )
    if (ret.statusCode != HttpStatus.Created_201.code) {
      throw SonatypeException(STAGE_FAILURE, s"Failed to close the repository. [${ret.status}]: ${ret.contentString}")
    }
    waitForStageCompletion(
      "close",
      repo,
      terminationCond = {
        _.isCloseSucceeded(repo.repositoryId)
      }
    ).toClosed
  }

  def promoteStage(currentProfile: StagingProfile, repo: StagingRepositoryProfile): StagingRepositoryProfile = {
    info(s"Promoting staging repository $repo")
    val ret = httpClient.postOps[Map[String, StageTransitionRequest], Response](
      s"$pathPrefix/staging/profiles/${repo.profileId}/promote",
      newStageTransitionRequest(currentProfile, repo)
    )
    if (ret.statusCode != HttpStatus.Created_201.code) {
      throw SonatypeException(STAGE_FAILURE, s"Failed to promote the repository. [${ret.status}]: ${ret.contentString}")
    }

    waitForStageCompletion("promote", repo, terminationCond = { _.isReleaseSucceeded(repo.repositoryId) })
  }

  def dropStage(currentProfile: StagingProfile, repo: StagingRepositoryProfile): Response = {
    info(s"Dropping staging repository $repo")
    val ret = httpClient.postOps[Map[String, StageTransitionRequest], Response](
      s"$pathPrefix/staging/profiles/${repo.profileId}/drop",
      newStageTransitionRequest(currentProfile, repo)
    )
    if (ret.statusCode != HttpStatus.Created_201.code) {
      throw SonatypeException(STAGE_FAILURE, s"Failed to drop the repository. [${ret.status}]: ${ret.contentString}")
    }
    ret
  }

  private def newStageTransitionRequest(
      currentProfile: StagingProfile,
      repo: StagingRepositoryProfile
  ): Map[String, StageTransitionRequest] = {
    Map(
      "data" -> StageTransitionRequest(
        stagedRepositoryId = repo.repositoryId,
        targetRepositoryId = currentProfile.repositoryTargetId,
        description = repo.description
      )
    )
  }

  def activitiesOf(r: StagingRepositoryProfile): Seq[StagingActivity] = {
    debug(s"Checking activity logs of ${r.repositoryId} ...")
    httpClient.get[Seq[StagingActivity]](s"$pathPrefix/staging/repository/${r.repositoryId}/activity")
  }

  def uploadBundle(coordinateGroupId: String, remoteUrl: String, getDeployables: () => DirectoryIOSource): Unit = {
    retryer
      .retryOn {
        case e: IOException if e.getMessage.contains("400 Bad Request") =>
          Retry.nonRetryableFailure(
            SonatypeException(
              BUNDLE_UPLOAD_FAILURE,
              s"Bundle upload failed. Probably a previously uploaded bundle remains. Run sonatypeClean or sonatypeDropAll first: ${e.getMessage}"
            )
          )
        case e: SigningException =>
          Retry.nonRetryableFailure(e)
      }
      .withResultClassifier[Try[_]] {
        case Success(_) => ResultClass.Succeeded
        case Failure(x) => ResultClass.Failed(isRetryable = false, x)
      }
      .run {
        val parameters = ParametersBuilder.defaults().build()
        // Adding a trailing slash is necessary upload a bundle file to a proper location:
        val endpoint      = s"$remoteUrl/"
        val clientBuilder = new Hc4ClientBuilder(parameters, endpoint)

        val credentialProvider = new BasicCredentialsProvider()
        val usernamePasswordCredentials =
          new UsernamePasswordCredentials(username, password)

        credentialProvider.setCredentials(AuthScope.ANY, usernamePasswordCredentials)

        clientBuilder.withPreemptiveRealm(credentialProvider)
        val deployables = getDeployables()

        val client = clientBuilder.build()
        val tried = Try {
          info(s"Uploading bundle $coordinateGroupId to $endpoint")
          client.upload(deployables)
          info(s"Finished bundle upload: $coordinateGroupId")
        }
        client.close()
        tried
      }
  }
}

object SonatypeClient extends LogSupport {

  case class StagingProfileResponse(data: Seq[StagingProfile] = Seq.empty)

  /** Staging profile is the information associated to a Sonatype account.
    */
  case class StagingProfile(id: String, name: String, repositoryTargetId: String)

  /** Staging repository profile has an id of deployed artifact and the current staging state.
    */
  case class StagingRepositoryProfile(
      profileId: String,
      profileName: String,
      `type`: String,
      repositoryId: String,
      description: String
  ) {
    def stagingType: String = `type`

    override def toString =
      s"[$repositoryId] status:$stagingType, profile:$profileName($profileId) description: $description"
    def isOpen: Boolean = stagingType == "open"
    def isClosed: Boolean = stagingType == "closed"
    def isReleased: Boolean = stagingType == "released"

    def toClosed: StagingRepositoryProfile = copy(`type` = "closed")
    def toDropped: StagingRepositoryProfile = copy(`type` = "dropped")
    def toReleased: StagingRepositoryProfile = copy(`type` = "released")

    def deployUrl(repositoryUrl: String): String = s"$repositoryUrl/staging/deployByRepositoryId/$repositoryId"
  }

  case class CreateStageResponse(
      data: StagingRepositoryRef
  )
  //noinspection ScalaWeakerAccess
  case class StagingRepositoryRef(
      stagedRepositoryId: String,
      description: String
  )

  case class StageTransitionRequest(
      stagedRepositoryId: String,
      targetRepositoryId: String,
      description: String
  )

  //noinspection ScalaWeakerAccess
  case class Prop(name: String, value: String)

  /** ActivityEvent is an evaluation result (e.g., checksum, signature check, etc.)
    * of a rule defined in a StagingActivity ruleset
    */
  case class ActivityEvent(timestamp: String, name: String, severity: Int, properties: Seq[Prop]) {
    lazy val map: Map[String, String] = properties.map(x => x.name -> x.value).toMap
    def ruleType: String = map.getOrElse("typeId", "other")
    def isFailure: Boolean = name == "ruleFailed"

    override def toString: String = {
      s"-event -- timestamp:$timestamp, name:$name, severity:$severity, ${properties.map(p => s"${p.name}:${p.value}").mkString(", ")}"
    }

    def showProgress(useErrorLog: Boolean = false): Unit = {
      val props = {
        val front =
          if (map.contains("typeId"))
            Seq(map("typeId"))
          else
            Seq.empty
        front ++ map.filter(_._1 != "typeId").map(p => s"${p._1}:${p._2}")
      }
      val messageLine = props.mkString(", ")
      val name_s      = name.replaceAll("rule(s)?", "")
      val message     = f"$name_s%10s: $messageLine"
      if (useErrorLog)
        logger.error(message)
      else
        logger.info(message)
    }
  }

  class ActivityMonitor {
    private var reportedActivities = Set.empty[String]
    private var reportedEvents     = Set.empty[ActivityEvent]

    def report(stagingActivities: Seq[StagingActivity]): Unit = {
      for (sa <- stagingActivities) {
        if (!reportedActivities.contains(sa.started)) {
          logger.info(sa.activityLog)
          reportedActivities += sa.started
        }
        for (ae <- sa.events if !reportedEvents.contains(ae)) {
          ae.showProgress(useErrorLog = false)
          reportedEvents += ae
        }
      }
    }
  }

  /** Staging activity is an action to the staged repository
    * @param name activity name, e.g. open, close, promote, etc.
    */
  case class StagingActivity(name: String, started: String, stopped: String, events: Seq[ActivityEvent]) {
    override def toString: String = {
      val b = Seq.newBuilder[String]
      b += activityLog
      for (e <- events)
        b += s" ${e.toString}"
      b.result().mkString("\n")
    }

    def activityLog: String = {
      val b = Seq.newBuilder[String]
      b += s"Activity name:$name"
      b += s"started:$started"
      if (stopped.nonEmpty) {
        b += s"stopped:$stopped"
      }
      b.result().mkString(", ")
    }

    def showProgress(): Unit = {
      logger.info(activityLog)
      val hasError = containsError
      for (e <- suppressEvaluateLog) {
        e.showProgress(hasError)
      }
    }

    private def suppressEvaluateLog: Seq[ActivityEvent] = {
      val in     = events.toIndexedSeq
      var cursor = 0
      val b      = Seq.newBuilder[ActivityEvent]
      while (cursor < in.size) {
        val current = in(cursor)
        if (cursor < in.size - 1) {
          val next = in(cursor + 1)
          if (current.name == "ruleEvaluate" && current.ruleType == next.ruleType) {
            // skip
          } else {
            b += current
          }
        }
        cursor += 1
      }
      b.result()
    }

    def containsError: Boolean = events.exists(_.severity != 0)

    def failureReport: Seq[ActivityEvent] = suppressEvaluateLog.filter(_.isFailure)

    def reportFailure(): Unit = {
      logger.error(activityLog)
      for (e <- failureReport) {
        e.showProgress(useErrorLog = true)
      }
    }

    def isReleaseSucceeded(repositoryId: String): Boolean = {
      events
        .find(_.name == "repositoryReleased")
        .exists(_.map.getOrElse("id", "") == repositoryId)
    }

    def isCloseSucceeded(repositoryId: String): Boolean = {
      events
        .find(_.name == "repositoryClosed")
        .exists(_.map.getOrElse("id", "") == repositoryId)
    }
  }
}


package org.jetbrains.bazel.sonatype

case class SonatypeCoordinates(sonatypeGroupId: String, sonatypeArtifactId: String, sonatypeVersion: String)

object SonatypeCoordinates {
  def apply(sonatypeCoordinates: String): SonatypeCoordinates = {
    val parts = sonatypeCoordinates.split(":")
    if (parts.size != 3)
      throw new IllegalArgumentException("Coordinates must be a triplet, got: " + sonatypeCoordinates)

    new SonatypeCoordinates(parts(0), parts(1), parts(2))
  }
}

package org.jetbrains.bazel.sonatype

/** An exception used for showing only an error message when there is no need to show stack traces
  */
case class SonatypeException(errorCode: ErrorCode, message: String) extends Exception(message) {
  override def toString = s"[${errorCode}] ${message}"
}

sealed trait ErrorCode

object SonatypeException {

  case object STAGE_IN_PROGRESS extends ErrorCode

  case object STAGE_FAILURE extends ErrorCode

  case object BUNDLE_UPLOAD_FAILURE extends ErrorCode

  case object MISSING_CREDENTIAL extends ErrorCode

  case object MISSING_STAGING_PROFILE extends ErrorCode

  case object MISSING_PROFILE extends ErrorCode

  case object UNKNOWN_STAGE extends ErrorCode

  case object MULTIPLE_TARGETS extends ErrorCode

}

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

package org.jetbrains.bazel

import sonatype.SonatypeClient.StagingRepositoryProfile
import sonatype.{SonatypeClient, SonatypeCoordinates, SonatypeService}

import org.backuity.clist.{Command, arg, opt}
import org.sonatype.spice.zapper.Path
import wvlet.log.{LogLevel, LogSupport}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Try

class SonatypeKeys extends Command {
  var sonatypeUsername: Option[String] =
  arg[Option[String]](required = false, description = "Username for the sonatype repository")
  var sonatypePassword: Option[String] =
  arg[Option[String]](required = false, description = "Password for the sonatype repository")
  var sonatypeRepository: String =
    arg[String](
      description = "Sonatype repository URL: e.g. https://oss.sonatype.org/service/local",
    )
  var sonatypeProfileName: String =
    arg[String](description = "Profile name at Sonatype: e.g. org.xerial")
  var sonatypeSessionName: Option[String] =
    opt[Option[String]](description = "Used for identifying a sonatype staging repository")
  var sonatypeCoordinates: String = arg[String](description = "Coordinates at Sonatype: e.g. org.xerial.sbt-sonatype")
  var sonatypeTimeoutMillis: Int =
    opt[Int](default = 60 * 60 * 1000, description = "milliseconds before giving up Sonatype API requests")
  var sonatypeLogLevel: String =
    opt[String](default = "info", description = "log level: trace, debug, info warn, error")
  var sonatypeProjectJar: String        = arg[String](description = "Path to project jar")
  var sonatypeProjectSourcesJar: String = arg[String](description = "Path to project sources jar")
  var sonatypeProjectDocsJar: String    = arg[String](description = "Path to project docs jar")
  var sonatypeProjectPom: String        = arg[String](description = "Path to project pom file")
}

class Sonatype(sonatypeKeys: SonatypeKeys) extends LogSupport {

  val username: String = sonatypeKeys.sonatypeUsername.getOrElse(
    sys.env.getOrElse(
      "SONATYPE_USERNAME",
      throw new IllegalArgumentException("SONATYPE_USERNAME variable is not defined")
    )
  )
  val password: String = sonatypeKeys.sonatypePassword.getOrElse(
    sys.env.getOrElse(
      "SONATYPE_PASSWORD",
      throw new IllegalArgumentException("SONATYPE_PASSWORD variable is not defined")
    )
  )

  private lazy val sonatypeSplitCoordinates: SonatypeCoordinates =
    SonatypeCoordinates(sonatypeKeys.sonatypeCoordinates)

  private implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  private lazy val rest: SonatypeService = {
    val logLevel = LogLevel(sonatypeKeys.sonatypeLogLevel)
    wvlet.log.Logger.setDefaultLogLevel(logLevel)
    val sonatypeClient = new SonatypeClient(
      repositoryUrl = sonatypeKeys.sonatypeRepository,
      username = username,
      password = password,
      timeoutMillis = sonatypeKeys.sonatypeTimeoutMillis
    )

    new SonatypeService(
      sonatypeClient,
      sonatypeKeys.sonatypeProfileName
    )
  }

  lazy val sonatypeSessionName: String = {
    sonatypeKeys.sonatypeSessionName.getOrElse(
      s"[bazel-sonatype] ${sonatypeSplitCoordinates.sonatypeArtifactId} ${sonatypeSplitCoordinates.sonatypeVersion}"
    )
  }

  lazy val filesPaths: List[Path] = List(
    new Path(sonatypeKeys.sonatypeProjectJar),
    new Path(sonatypeKeys.sonatypeProjectSourcesJar),
    new Path(sonatypeKeys.sonatypeProjectDocsJar),
    new Path(sonatypeKeys.sonatypeProjectPom)
  )

  private def withSonatypeService[R]()(
      body: SonatypeService => R
  ): Try[R] = {
    val result = Try(body(rest))
    rest.close()
    result
  }

  private def prepare(rest: SonatypeService): StagingRepositoryProfile = {
    val descriptionKey = sonatypeSessionName
    // Drop a previous staging repository if exists
    val dropTask = Future.apply(rest.dropIfExistsByKey(descriptionKey))
    // Create a new one
    val createTask = Future.apply(rest.createStage(descriptionKey))
    // Run two tasks in parallel
    val merged = dropTask.zip(createTask)
    val (_, createdRepo) = Await.result(merged, Duration.Inf)
    createdRepo
  }

  def openRepo(): Try[StagingRepositoryProfile] = {
    withSonatypeService() { rest =>
      rest.openOrCreateByKey(sonatypeSessionName)
    }
  }

  def bundleRelease(): Try[StagingRepositoryProfile] = {
    withSonatypeService() { rest =>
      val repo = prepare(rest)
      rest.uploadBundle(
        sonatypeSplitCoordinates.sonatypeGroupId,
        repo.deployUrl(sonatypeKeys.sonatypeRepository),
        filesPaths
      )
      rest.closeAndPromote(repo)
    }
  }
}

package org.jetbrains.bazel

import org.backuity.clist.Cli

object SonatypeOpen {
  def main(args: Array[String]): Unit = {
    Cli.parse(args)
      .withCommand(new SonatypeKeys) {
        keys: SonatypeKeys =>
          new Sonatype(keys).openRepo()
      }
      .get.get // just throw if anything broke
  }
}

package org.jetbrains.bazel

import org.backuity.clist.Cli

object SonatypePublish {
  def main(args: Array[String]): Unit = {
    Cli.parse(args)
      .withCommand(new SonatypeKeys) { keys: SonatypeKeys =>
        new Sonatype(keys).bundleRelease()
      }
      .get.get // just throw here if anything broke

  }
}

