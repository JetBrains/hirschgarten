package org.jetbrains.bazel

import org.backuity.clist.{Command, arg, opt}
import org.jetbrains.bazel.sonatype.SonatypeClient.StagingRepositoryProfile
import org.jetbrains.bazel.sonatype.{SonatypeClient, SonatypeException, SonatypeService}
import org.sonatype.spice.zapper.Path
import wvlet.log.{LogLevel, LogSupport}

import java.nio.file.Paths
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

class SonatypeKeys extends Command {
  var sonatypeRepository: String =
    opt[String](
      description = "Sonatype repository URL: e.g. https://oss.sonatype.org/service/local",
      default = "https://oss.sonatype.org/service/local"
    )
  var sonatypeSessionName: Option[String] =
    arg[Option[String]](required = false, description = "Used for identifying a sonatype staging repository")
  var sonatypeProfileName: String = arg[String](description = "Profile name at Sonatype: e.g. org.xerial")
  var sonatypeTimeoutMillis: Int =
    opt[Int](default = 60 * 60 * 1000, description = "milliseconds before giving up Sonatype API requests")
  var sonatypeLogLevel: String =
    opt[String](default = "info", description = "log level: trace, debug, info warn, error")
  var sonatypeProjectJar: String        = arg[String](description = "Path to project jar")
  var sonatypeProjectSourcesJar: String = arg[String](description = "Path to project sources jar")
}

class Sonatype(sonatypeKeys: SonatypeKeys) extends LogSupport {
  val username: String = sys.env.getOrElse(
    "SONATYPE_USERNAME",
    throw new IllegalArgumentException("SONATYPE_USERNAME variable is not defined")
  )
  val password: String = sys.env.getOrElse(
    "SONATYPE_PASSWORD",
    throw new IllegalArgumentException("SONATYPE_PASSWORD variable is not defined")
  )

  private implicit val ec = ExecutionContext.global

  lazy val rest: SonatypeService = {
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
    sonatypeKeys.sonatypeSessionName.getOrElse(sonatypeKeys.sonatypeProfileName)
  }

  lazy val filesPaths: List[Path] = List(
    new Path(sonatypeKeys.sonatypeProjectJar),
    new Path(sonatypeKeys.sonatypeProjectSourcesJar),
  )

  private def withSonatypeService()(
    body: SonatypeService => Unit
  ): Boolean = {
    try {
      body(rest)
      true
    } catch {
      case e: SonatypeException =>
        error(e.toString)
        false
      case e: Throwable =>
        error(e)
        false
    } finally {
      rest.close()
    }
  }

  private def prepare(rest: SonatypeService): StagingRepositoryProfile = {
    val descriptionKey = sonatypeSessionName
    // Drop a previous staging repository if exists
    val dropTask = Future.apply(rest.dropIfExistsByKey(descriptionKey))
    // Create a new one
    val createTask = Future.apply(rest.createStage(descriptionKey))
    // Run two tasks in parallel
    val merged                     = dropTask.zip(createTask)
    val (droppedRepo, createdRepo) = Await.result(merged, Duration.Inf)
    createdRepo
  }

  def openRepo(): Unit = {
    withSonatypeService() { rest =>
      rest.openOrCreateByKey(sonatypeSessionName)
    }
  }

  def bundleRelease(): Unit = {
    withSonatypeService() { rest =>
      val repo = prepare(rest)
      rest.uploadBundle(Paths.get(sonatypeKeys.sonatypeProjectJar).getParent.toFile, repo.deployUrl(sonatypeKeys.sonatypeRepository), filesPaths)
      rest.closeAndPromote(repo)
    }
  }
}
