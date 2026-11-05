package org.jetbrains.bazel.bazelrunner

import com.intellij.credentialStore.Credentials
import com.intellij.openapi.application.PathManager
import com.intellij.testFramework.junit5.fixture.disposableFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.util.EnvironmentUtil
import com.intellij.util.ReadEnv
import com.intellij.util.net.ProxyConfiguration.ProxyProtocol
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.installIdeProxyConfiguration
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@BazelTestApplication
internal class BazelProxyForwardingLauncherTest {

  private val workspaceRoot by tempPathFixture()
  private val disposable by disposableFixture()

  @Test
  fun `the process gets the IDE proxy with credentials when forwarding is on`() {
    installIdeProxyConfiguration(
      disposable,
      host = "proxy.corp",
      port = 3128,
      exceptions = "localhost,*.corp.example",
      credentials = Credentials("jane.doe", "p@ss"),
    )

    launchAndReadProxyVariables() shouldBe expectedEnvironment("jane.doe:p%40ss@proxy.corp:3128", noProxy = "localhost,.corp.example")
  }

  @Test
  fun `the process gets no proxy when forwarding is off`() {
    installIdeProxyConfiguration(disposable, forward = false)

    launchAndReadProxyVariables() shouldBe emptyMap()
  }

  @Test
  fun `the process gets no proxy for a SOCKS proxy`() {
    installIdeProxyConfiguration(disposable, protocol = ProxyProtocol.SOCKS)

    launchAndReadProxyVariables() shouldBe emptyMap()
  }

  @Test
  fun `the environment of the command replaces the forwarded proxy`() {
    installIdeProxyConfiguration(disposable, host = "proxy.corp", port = 3128)

    launchAndReadProxyVariables(commandEnvironment = mapOf("https_proxy" to "other.proxy:8080")) shouldBe expectedEnvironment("proxy.corp:3128") + ("https_proxy" to "other.proxy:8080")
  }

  private fun launchAndReadProxyVariables(commandEnvironment: Map<String, String> = emptyMap()): Map<String, String> {
    val launcher = BazelProcessLauncherProvider
      .getInstance()
      .createBazelProcessLauncher(workspaceRoot, emptyMap())
    val process = launcher.launchProcess(
      BazelCommandExecutionDescriptor(printEnvironmentCommand(), enablePty = false, environment = commandEnvironment),
    )
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor(30, TimeUnit.SECONDS) shouldBe true
    process.exitValue() shouldBe 0
    return EnvironmentUtil.parseEnv(output.split('\u0000').toTypedArray())
      .filterKeys { it.uppercase() in PROXY_VARIABLES }
  }

  /** [ReadEnv] prints the environment of its own process, the same way on every OS. */
  private fun printEnvironmentCommand(): List<String> = listOf(
    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
    "-cp",
    PathManager.getJarPathForClass(ReadEnv::class.java) ?: error("No class path for ${ReadEnv::class.java.name}"),
    ReadEnv::class.java.name,
  )

  private companion object {
    val PROXY_VARIABLES = setOf("HTTP_PROXY", "HTTPS_PROXY", "NO_PROXY")
  }
}
