package org.jetbrains.bazel.sonatype

import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.*
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.sonatype.spice.zapper.client.hc4.Hc4ClientBuilder
import org.sonatype.spice.zapper.ParametersBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.IOException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

class SonatypeClient(
  repositoryUrl: String,
  private val username: String,
  private val password: String,
  private val timeoutMillis: Int = 60 * 60 * 1000
) : AutoCloseable {

  val LOG: Logger = LogManager.getLogger(SonatypeClient::class.java)

  private val base64Credentials: String =
    Base64.getEncoder().encodeToString("$username:$password".toByteArray(StandardCharsets.UTF_8))

  private val repoUri: String = repositoryUrl.trimEnd('/')

  private val pathPrefix: String = URL(repoUri).path

  private val mapper = jacksonObjectMapper()

  private val httpClient: CloseableHttpClient

  init {
    val credentialsProvider = BasicCredentialsProvider()
    credentialsProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))

    httpClient = HttpClients.custom()
      .setDefaultCredentialsProvider(credentialsProvider)
      .setConnectionTimeToLive(timeoutMillis.toLong(), TimeUnit.MILLISECONDS)
      .build()
  }

  override fun close() {
    httpClient.close()
  }

  // Function to execute a GET request and parse the response into a specific type
  private inline fun <reified T> getRequest(url: String): T {
    val request = HttpGet(url).apply {
      addHeader("Accept", "application/json")
      addHeader("Authorization", "Basic $base64Credentials")
    }
    httpClient.execute(request).use { response ->
      val status = response.statusLine.statusCode
      val entity = response.entity ?: throw IOException("Empty response body")
      val content = EntityUtils.toString(entity, StandardCharsets.UTF_8)
      if (status in 200..299) {
        return mapper.readValue(content)
      } else {
        throw IOException("GET request failed with status $status: $content")
      }
    }
  }

  // Function to execute a POST request and parse the response into a specific type
  private inline fun <reified Req, reified Res> postRequest(url: String, body: Req): Res {
    val request = HttpPost(url).apply {
      addHeader("Content-Type", "application/json")
      addHeader("Accept", "application/json")
      addHeader("Authorization", "Basic $base64Credentials")
      entity = StringEntity(mapper.writeValueAsString(body), StandardCharsets.UTF_8)
    }
    httpClient.execute(request).use { response ->
      val status = response.statusLine.statusCode
      val entity = response.entity ?: throw IOException("Empty response body")
      val content = EntityUtils.toString(entity, StandardCharsets.UTF_8)
      if (status in 200..299) {
        return mapper.readValue(content)
      } else {
        throw IOException("POST request failed with status $status: $content")
      }
    }
  }

  // Data classes for responses
  data class StagingProfileResponse(val data: List<StagingProfile> = emptyList())
  data class StagingProfile(
    val id: String,
    val name: String,
    val repositoryTargetId: String
  )
  data class StagingRepositoryProfile(
    val profileId: String,
    val profileName: String,
    val type: String,
    val repositoryId: String,
    val description: String
  ) {
    fun isOpen() = type == "open"
    fun isClosed() = type == "closed"
    fun isReleased() = type == "released"

    fun toClosed(): StagingRepositoryProfile = copy(type = "closed")
    fun toDropped(): StagingRepositoryProfile = copy(type = "dropped")
    fun toReleased(): StagingRepositoryProfile = copy(type = "released")

    fun deployUrl(repositoryUrl: String): String = "$repositoryUrl/staging/deployByRepositoryId/$repositoryId"

    override fun toString(): String =
      "[$repositoryId] status:$type, profile:$profileName($profileId) description: $description"
  }

  data class CreateStageResponse(
    val data: StagingRepositoryRef
  )
  data class StagingRepositoryRef(
    val stagedRepositoryId: String,
    val description: String
  )
  data class StageTransitionRequest(
    val stagedRepositoryId: String,
    val targetRepositoryId: String,
    val description: String
  )

  data class CloseStageResponse(val statusCode: Int, val content: String)
  data class PromoteStageResponse(val statusCode: Int, val content: String)
  data class ResponseDropStage(val statusCode: Int, val content: String)

  // Implement stagingRepositoryProfiles
  fun stagingRepositoryProfiles(): List<StagingRepositoryProfile> {
    LOG.info("Reading staging repository profiles...")
    val url = "$repoUri$pathPrefix/staging/profile_repositories"
    return getRequest<StagingRepositoryProfileResponseWrapper>(url).data
  }

  // Wrapper to match expected JSON
  data class StagingRepositoryProfileResponseWrapper(val data: List<StagingRepositoryProfile> = emptyList())

  // Implement stagingRepository
  fun stagingRepository(repositoryId: String): String {
    LOG.info("Searching for repository $repositoryId ...")
    val url = "$repoUri$pathPrefix/staging/repository/$repositoryId"
    return getRequest<String>(url)
  }

  // Implement stagingProfiles
  fun stagingProfiles(): List<StagingProfile> {
    LOG.info("Reading staging profiles...")
    val url = "$repoUri$pathPrefix/staging/profiles"
    return getRequest<StagingProfileResponse>(url).data
  }

  // Implement createStage
  fun createStage(profile: StagingProfile, description: String): StagingRepositoryProfile {
    LOG.info("Creating a staging repository in profile ${profile.name} with a description key: $description")
    val url = "$repoUri$pathPrefix/staging/profiles/${profile.id}/start"
    val requestBody = mapOf("data" to StageTransitionRequest(
      stagedRepositoryId = "",
      targetRepositoryId = profile.repositoryTargetId,
      description = description
    ))
    val response: CreateStageResponse = postRequest(url, requestBody)
    val repo = StagingRepositoryProfile(
      profileId = profile.id,
      profileName = profile.name,
      type = "open",
      repositoryId = response.data.stagedRepositoryId,
      description = response.data.description
    )
    LOG.info("Created successfully: ${repo.repositoryId}")
    return repo
  }

  // Implement closeStage
  fun closeStage(profile: StagingProfile, repo: StagingRepositoryProfile): StagingRepositoryProfile {
    LOG.info("Closing staging repository $repo")
    val url = "$repoUri$pathPrefix/staging/profiles/${repo.profileId}/finish"
    val requestBody = newStageTransitionRequest(profile, repo)
    val response: CloseStageResponse = postRequest(url, requestBody)
    if (response.statusCode != 201) { // Assuming 201 is Created
      throw SonatypeException(SonatypeErrorCode.STAGE_FAILURE, "Failed to close the repository. [${response.statusCode}]: ${response.content}")
    }
    return waitForStageCompletion(
      "close",
      repo,
      terminationCond = { activity -> activity.isCloseSucceeded(repo.repositoryId) }
    ).toClosed()
  }

  // Implement promoteStage
  fun promoteStage(profile: StagingProfile, repo: StagingRepositoryProfile): StagingRepositoryProfile {
    LOG.info("Promoting staging repository $repo")
    val url = "$repoUri$pathPrefix/staging/profiles/${repo.profileId}/promote"
    val requestBody = newStageTransitionRequest(profile, repo)
    val response: PromoteStageResponse = postRequest(url, requestBody)
    if (response.statusCode != 201) { // Assuming 201 is Created
      throw SonatypeException(SonatypeErrorCode.STAGE_FAILURE, "Failed to promote the repository. [${response.statusCode}]: ${response.content}")
    }
    return waitForStageCompletion(
      "promote",
      repo,
      terminationCond = { activity -> activity.isReleaseSucceeded(repo.repositoryId) }
    )
  }

  // Implement dropStage
  fun dropStage(profile: StagingProfile, repo: StagingRepositoryProfile): ResponseDropStage {
    LOG.info("Dropping staging repository $repo")
    val url = "$repoUri$pathPrefix/staging/profiles/${repo.profileId}/drop"
    val requestBody = newStageTransitionRequest(profile, repo)
    val response: ResponseDropStage = postRequest(url, requestBody)
    if (response.statusCode != 201) { // Assuming 201 is Created
      throw SonatypeException(SonatypeErrorCode.STAGE_FAILURE, "Failed to drop the repository. [${response.statusCode}]: ${response.content}")
    }
    return response
  }

  // Implement uploadBundle
  fun uploadBundle(coordinateGroupId: String, remoteUrl: String, getDeployables: () -> DirectoryIOSourceMaven) {
    try {
      LOG.info("Uploading bundle $coordinateGroupId to $remoteUrl")
      val parameters = ParametersBuilder.defaults().build()
      val clientBuilder = Hc4ClientBuilder(parameters, remoteUrl)

      val credentialProvider = org.apache.http.impl.client.BasicCredentialsProvider()
      val creds = UsernamePasswordCredentials(username, password)
      credentialProvider.setCredentials(AuthScope.ANY, creds)
      clientBuilder.withPreemptiveRealm(credentialProvider)

      val client = clientBuilder.build()
      val deployables = getDeployables()

      client.upload(deployables)
      LOG.info("Finished bundle upload: $coordinateGroupId")
      client.close()
    } catch (e: Exception) {
      LOG.error("Bundle upload failed: ${e.message}")
      throw SonatypeException(SonatypeErrorCode.BUNDLE_UPLOAD_FAILURE, "Bundle upload failed: ${e.message}")
    }
  }

  // Implement waitForStageCompletion
  fun waitForStageCompletion(
    taskName: String,
    repo: StagingRepositoryProfile,
    terminationCond: (StagingActivity) -> Boolean
  ): StagingRepositoryProfile {
    val maxInterval = 15000L
    val initInterval = 3000L
    val multiplier = 1.5
    val retryCountUntilMaxInterval = (Math.log(maxInterval.toDouble() / initInterval) / Math.log(multiplier)).toInt().coerceAtLeast(1)
    val numRetry = Math.ceil(timeoutMillis.toDouble() / maxInterval).toInt()
    val totalRetries = retryCountUntilMaxInterval + numRetry

    val monitor = ActivityMonitor()

    for (retry in 1..totalRetries) {
      try {
        val activities = activitiesOf(repo)
        monitor.report(activities)
        val lastActivity = activities.lastOrNull()
        if (lastActivity != null) {
          when {
            terminationCond(lastActivity) -> {
              LOG.info("[$taskName] Finished successfully")
              return repo
            }
            lastActivity.containsError() -> {
              LOG.error("[$taskName] Failed")
              lastActivity.reportFailure()
              throw SonatypeException(SonatypeErrorCode.STAGE_FAILURE, "Failed to $taskName the repository.")
            }
            else -> {
              // Wait and retry
              val waitTime = (initInterval * Math.pow(multiplier, retry.toDouble())).toLong().coerceAtMost(maxInterval)
              LOG.info("Waiting for ${waitTime / 1000.0} seconds before retrying...")
              Thread.sleep(waitTime)
            }
          }
        }
      } catch (e: SonatypeException) {
        LOG.error("Error during $taskName: ${e.message}")
        throw e
      } catch (e: Exception) {
        LOG.warn("[$retry/$totalRetries] Execution failed: ${e.message}. Retrying in ${initInterval / 1000.0} seconds.")
        val waitTime = (initInterval * Math.pow(multiplier, retry.toDouble())).toLong().coerceAtMost(maxInterval)
        Thread.sleep(waitTime)
      }
    }
    throw SonatypeException(SonatypeErrorCode.STAGE_IN_PROGRESS, "The $taskName stage is in progress.")
  }

  // Implement activitiesOf
  fun activitiesOf(repo: StagingRepositoryProfile): List<StagingActivity> {
    LOG.debug("Checking activity logs of ${repo.repositoryId} ...")
    val url = "$repoUri$pathPrefix/staging/repository/${repo.repositoryId}/activity"
    return getRequest(url)
  }

  // Implement helper methods
  private fun newStageTransitionRequest(profile: StagingProfile, repo: StagingRepositoryProfile): Map<String, StageTransitionRequest> {
    return mapOf(
      "data" to StageTransitionRequest(
        stagedRepositoryId = repo.repositoryId,
        targetRepositoryId = profile.repositoryTargetId,
        description = repo.description
      )
    )
  }

  // ActivityMonitor and related classes
  class ActivityMonitor {
    private val reportedActivities = mutableSetOf<String>()
    private val reportedEvents = mutableSetOf<ActivityEvent>()

    private val LOG: Logger = LogManager.getLogger(ActivityMonitor::class.java)

    fun report(stagingActivities: List<StagingActivity>) {
      for (sa in stagingActivities) {
        if (!reportedActivities.contains(sa.started)) {
          LOG.info(sa.activityLog())
          reportedActivities.add(sa.started)
        }
        for (ae in sa.events) {
          if (!reportedEvents.contains(ae)) {
            ae.showProgress(false)
            reportedEvents.add(ae)
          }
        }
      }
    }
  }

  // Define ActivityEvent and StagingActivity
  data class ActivityEvent(
    val timestamp: String,
    val name: String,
    val severity: Int,
    val properties: List<Prop>
  ) {
    val LOG = LogManager.getLogger(ActivityEvent::class.java)
    val map: Map<String, String> = properties.associate { it.name to it.value }
    fun ruleType(): String = map["typeId"] ?: "other"
    fun isFailure(): Boolean = name == "ruleFailed"

    override fun toString(): String =
      "-event -- timestamp:$timestamp, name:$name, severity:$severity, " +
        properties.joinToString(", ") { "${it.name}:${it.value}" }

    fun showProgress(useErrorLog: Boolean) {
      val props = mutableListOf<String>()
      if (map.containsKey("typeId")) {
        props.add(map["typeId"]!!)
      }
      props += map.filterKeys { it != "typeId" }.map { "${it.key}:${it.value}" }
      val messageLine = props.joinToString(", ")
      val name_s = name.replace(Regex("rule(s)?"), "")
      val message = String.format("%10s: %s", name_s, messageLine)
      if (useErrorLog) {
        LOG.error(message)
      } else {
        LOG.info(message)
      }
    }
  }

  data class Prop(val name: String, val value: String)

  data class StagingActivity(
    val name: String,
    val started: String,
    val stopped: String,
    val events: List<ActivityEvent>
  ) {
    val LOG = LogManager.getLogger(StagingActivity::class.java)

    override fun toString(): String {
      val builder = StringBuilder()
      builder.append(activityLog())
      for (e in events) {
        builder.append("\n ").append(e.toString())
      }
      return builder.toString()
    }

    fun activityLog(): String {
      val builder = StringBuilder()
      builder.append("Activity name:$name")
      builder.append(", started:$started")
      if (stopped.isNotEmpty()) {
        builder.append(", stopped:$stopped")
      }
      return builder.toString()
    }

    fun showProgress() {
      LOG.info(activityLog())
      val hasError = containsError()
      for (e in suppressEvaluateLog()) {
        e.showProgress(hasError)
      }
    }

    private fun suppressEvaluateLog(): List<ActivityEvent> {
      val inEvents = events
      val b = mutableListOf<ActivityEvent>()
      var cursor = 0
      while (cursor < inEvents.size) {
        val current = inEvents[cursor]
        if (cursor < inEvents.size - 1) {
          val next = inEvents[cursor + 1]
          if (current.name == "ruleEvaluate" && current.ruleType() == next.ruleType()) {
            // skip
          } else {
            b.add(current)
          }
        } else {
          b.add(current)
        }
        cursor += 1
      }
      return b
    }

    fun containsError(): Boolean = events.any { it.severity != 0 }

    fun failureReport(): List<ActivityEvent> = suppressEvaluateLog().filter { it.isFailure() }

    fun reportFailure() {
      LOG.error(activityLog())
      for (e in failureReport()) {
        e.showProgress(true)
      }
    }

    fun isReleaseSucceeded(repositoryId: String): Boolean {
      return events.find { it.name == "repositoryReleased" }
        ?.map?.getOrDefault("id", "") == repositoryId
    }

    fun isCloseSucceeded(repositoryId: String): Boolean {
      return events.find { it.name == "repositoryClosed" }
        ?.map?.getOrDefault("id", "") == repositoryId
    }
  }
}
