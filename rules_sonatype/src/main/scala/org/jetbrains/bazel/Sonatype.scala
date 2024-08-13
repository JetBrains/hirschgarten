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
