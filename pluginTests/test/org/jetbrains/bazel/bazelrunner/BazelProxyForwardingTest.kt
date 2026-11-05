package org.jetbrains.bazel.bazelrunner

import com.intellij.credentialStore.Credentials
import com.intellij.util.net.ProxyConfiguration
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.installIdeProxyConfiguration
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

@BazelTestApplication
internal class BazelProxyForwardingTest {
  private val testBazel by testBazelProjectFixture()

  @BeforeEach
  fun setUp() {
    testBazel.installIdeProxyConfiguration()
  }

  @Test
  fun `only a static HTTP proxy is supported`() {
    val unsupported = listOf(
      ProxyConfiguration.direct,
      ProxyConfiguration.autodetect,
      ProxyConfiguration.proxyAutoConfiguration(URI("http://wpad.corp/proxy.pac").toURL()),
      ProxyConfiguration.proxy(ProxyConfiguration.ProxyProtocol.SOCKS, "socks.corp", 1080),
    )
    for (configuration in unsupported) {
      BazelProxyForwarding.isSupported(configuration) shouldBe false
      BazelProxyForwarding.environment(configuration, noCredentials) shouldBe emptyMap()
    }
    BazelProxyForwarding.isSupported(httpProxy()) shouldBe true
  }

  @Test
  fun `the environment is empty when forwarding is off`() {
    testBazel.installIdeProxyConfiguration(forward = false)

    BazelProxyForwarding.environment(httpProxy(), noCredentials) shouldBe emptyMap()
  }

  @Test
  fun `http proxy without credentials gives a url without scheme in both cases`() {
    BazelProxyForwarding.environment(httpProxy(), noCredentials) shouldBe expectedEnvironment("proxy.corp:3128")
  }

  @Test
  fun `http proxy with credentials encodes the user and the password`() {
    val environment = BazelProxyForwarding.environment(httpProxy()) { host, port ->
      if (host == "proxy.corp" && port == 3128) Credentials("jane@corp.com", "p@ss:w/rd") else null
    }

    environment shouldBe expectedEnvironment("jane%40corp.com:p%40ss%3Aw%2Frd@proxy.corp:3128")
  }

  @Test
  fun `a space in the credentials becomes a percent escape`() {
    BazelProxyForwarding
      .environment(httpProxy()) { _, _ -> Credentials("jane doe", "my pass") }
      .shouldBe(expectedEnvironment("jane%20doe:my%20pass@proxy.corp:3128"))
  }

  @Test
  fun `a user without a password gives no credentials`() {
    for (password in listOf(null, "")) {
      BazelProxyForwarding
        .environment(httpProxy()) { _, _ -> Credentials("jane.doe", password) }
        .shouldBe(expectedEnvironment("proxy.corp:3128"))
    }
  }

  @Test
  fun `exceptions become suffixes, and other wildcards are left out`() {
    BazelProxyForwarding
      .environment(httpProxy(exceptions = "localhost, *.corp.example ,192.168.*,, *"), noCredentials)
      .shouldBe(expectedEnvironment("proxy.corp:3128", noProxy = "localhost,.corp.example"))
  }

  private fun httpProxy(exceptions: String = "") = ProxyConfiguration.proxy(
    protocol = ProxyConfiguration.ProxyProtocol.HTTP,
    host = "proxy.corp",
    port = 3128,
    exceptions = exceptions,
  )

  private val noCredentials: (String, Int) -> Credentials? = { _, _ -> null }
}

internal fun expectedEnvironment(proxyUrl: String, noProxy: String = ""): Map<String, String> =
  buildMap {
    for (name in listOf("HTTP_PROXY", "HTTPS_PROXY")) {
      put(name, proxyUrl)
      put(name.lowercase(), proxyUrl)
    }
    put("NO_PROXY", noProxy)
    put("no_proxy", noProxy)
  }
