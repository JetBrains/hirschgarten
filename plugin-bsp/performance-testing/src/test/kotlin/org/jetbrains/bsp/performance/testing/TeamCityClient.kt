@file:Suppress("detekt:all")
package org.jetbrains.bsp.performance.testing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.ide.starter.ci.CIServer
import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.utils.HttpClient
import com.intellij.ide.starter.utils.replaceSpecialCharactersWithHyphens
import com.intellij.tools.ide.util.common.logError
import com.intellij.tools.ide.util.common.logOutput
import com.intellij.tools.ide.util.common.withRetryBlocking
import org.apache.http.HttpRequest
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.auth.BasicScheme
import org.kodein.di.direct
import org.kodein.di.instance
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

// Copied from monorepo to fix an authentication issue
fun <T : HttpRequest> T.withAuth(): T = this.apply {
  val teamCityCI by lazy { CIServer.instance.asTeamCity() }

  addHeader(BasicScheme().authenticate(UsernamePasswordCredentials(teamCityCI.userName, teamCityCI.password), this, null))
}

// TODO: move on to use TeamCityRest client library or stick with Okhttp
object TeamCityClient {
  private val teamCityURI by lazy { di.direct.instance<URI>(tag = "teamcity.uri") }

  // temporary directory, where artifact will be moved for preparation for publishing
  val artifactForPublishingDir: Path by lazy { GlobalPaths.instance.testsDirectory / "teamcity-artifacts-for-publish" }

  val restUri: URI = teamCityURI.resolve("/app/rest/")
  val guestAuthUri: URI = teamCityURI.resolve("/guestAuth/app/rest/")

  fun get(fullUrl: URI, additionalRequestActions: (HttpRequest) -> HttpRequest = { it }): JsonNode {
    val request = HttpGet(fullUrl).apply {
      addHeader("Content-Type", "application/json")
      addHeader("Accept", "application/json")
      additionalRequestActions(this)
    }

    logOutput("Request to TeamCity: $fullUrl")

    val result = withRetryBlocking(messageOnFailure = "Failure during request to TeamCity") {
      HttpClient.sendRequest(request) {
        if (it.statusLine.statusCode != 200) {
          logError(InputStreamReader(it.entity.content).readText())
          throw RuntimeException("TeamCity returned not successful status code ${it.statusLine.statusCode}")
        }

        jacksonObjectMapper().readTree(it.entity.content)
      }
    }

    return requireNotNull(result) { "Request ${request.uri} failed" }
  }

  /** @return <BuildId, BuildNumber> */
  fun getLastSuccessfulBuild(ideInfo: IdeInfo): Pair<String, String> {
    val tag = if (!ideInfo.tag.isNullOrBlank()) "tag:${ideInfo.tag}," else ""
    val number = if (!ideInfo.buildNumber.isBlank()) "number:${ideInfo.buildNumber}," else ""
    val branchName = System.getProperty("use.branch.name", "")
    val branch = if (branchName.isNotEmpty()) "branch:$branchName," else ""
    val fullUrl = guestAuthUri.resolve("builds?locator=buildType:${ideInfo.buildType},${branch}${tag}${number}status:SUCCESS,branch:default:any,state:(finished:true),count:1")

    val build = get(fullUrl).fields().asSequence().first { it.key == "build" }.value
    val buildId = build.findValue("id").asText()
    val buildNumber = ideInfo.buildNumber.ifBlank { build.findValue("number").asText() }
    return Pair(buildId, buildNumber)
  }

  fun downloadArtifact(buildId: String, artifactName: String, outFile: File) {
    val artifactUrl = guestAuthUri.resolve("builds/id:$buildId/artifacts/content/$artifactName")
    HttpClient.download(artifactUrl.toString(), outFile)
  }

  private fun printTcArtifactsPublishMessage(spec: String) {
    logOutput(" !!teamcity[publishArtifacts '$spec'] ") //we need this to see in the usual IDEA log
    logOutput(" ##teamcity[publishArtifacts '$spec'] ")
  }

  /**
   * [source] - source path of artifact
   * [artifactPath] - new path (relative, where artifact will be present)
   * [artifactName] - name of artifact
   */
  fun publishTeamCityArtifacts(
    source: Path,
    artifactPath: String,
    artifactName: String = source.fileName.toString(),
    zipContent: Boolean = true,
  ) {
    val sanitizedArtifactPath = artifactPath.replaceSpecialCharactersWithHyphens()
    val sanitizedArtifactName = artifactName.replaceSpecialCharactersWithHyphens()

    if (!source.exists()) {
      logOutput("TeamCity artifact $source does not exist")
      return
    }

    var suffix: String
    var nextSuffix = 0
    var artifactDir: Path
    do {
      suffix = if (nextSuffix == 0) "" else "-$nextSuffix"
      artifactDir = (artifactForPublishingDir / sanitizedArtifactPath / (sanitizedArtifactName + suffix)).normalize().toAbsolutePath()
      nextSuffix++
    }
    while (artifactDir.exists())

    logOutput("Creating directories for artifact publishing $artifactDir")
    artifactDir.toFile().deleteRecursively()
    artifactDir.createDirectories()

    if (source.isDirectory()) {
      Files.walk(source).use { files ->
        for (path in files) {
          path.copyTo(target = artifactDir.resolve(source.relativize(path)), overwrite = true)
        }
      }
      if (zipContent) {
        printTcArtifactsPublishMessage("${artifactDir.toRealPath()}/** => $sanitizedArtifactPath/$sanitizedArtifactName$suffix.zip")
      }
      else {
        printTcArtifactsPublishMessage("${artifactDir.toRealPath()}/** => $sanitizedArtifactPath$suffix")
      }
    }
    else {
      val tempFile = artifactDir
      source.copyTo(tempFile, overwrite = true)
      if (zipContent) {
        printTcArtifactsPublishMessage("${tempFile.toRealPath()} => $sanitizedArtifactPath/${sanitizedArtifactName + suffix}.zip")
      }
      else {
        printTcArtifactsPublishMessage("${tempFile.toRealPath()} => $sanitizedArtifactPath")
      }
    }
  }
}

