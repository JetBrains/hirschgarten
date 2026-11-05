package org.jetbrains.bazel.bazelrunner

import com.intellij.credentialStore.Credentials
import com.intellij.util.io.URLUtil.encodeURIComponent
import com.intellij.util.net.ProxyAuthentication
import com.intellij.util.net.ProxyConfiguration
import com.intellij.util.net.ProxySettings
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.settings.bazel.BazelApplicationSettingsService
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ApiStatus.Internal
object BazelProxyForwarding {

  /**
   * Only static HTTP proxy is supported.
   */
  @OptIn(ExperimentalContracts::class)
  fun isSupported(configuration: ProxyConfiguration = ProxySettings.getInstance().getProxyConfiguration()): Boolean {
    contract { returns(true) implies (configuration is ProxyConfiguration.StaticProxyConfiguration) }
    return when (configuration) {
      is ProxyConfiguration.StaticProxyConfiguration -> when (configuration.protocol) {
        ProxyConfiguration.ProxyProtocol.HTTP -> true
        ProxyConfiguration.ProxyProtocol.SOCKS -> false
      }
      is ProxyConfiguration.DirectProxy, is ProxyConfiguration.AutoDetectProxy, is ProxyConfiguration.ProxyAutoConfiguration -> false
    }
  }

  /**
   * Returns the proxy variables for a Bazel process, or an empty map when Bazel cannot use [configuration] or if proxy forwarding is disabled.
   */
  fun environment(
    configuration: ProxyConfiguration = ProxySettings.getInstance().getProxyConfiguration(),
    credentials: (host: String, port: Int) -> Credentials? = ProxyAuthentication.getInstance()::getKnownAuthentication,
  ): Map<String, String> = when {
    !isEnabled() || !isSupported(configuration) -> emptyMap()
    else -> httpProxyEnvs(configuration, credentials(configuration.host, configuration.port))
  }

  private fun httpProxyEnvs(
    configuration: ProxyConfiguration.StaticProxyConfiguration,
    credentials: Credentials?,
  ): Map<String, String> {
    val userName = credentials?.userName?.ifEmpty { null }
    val password = credentials?.getPasswordAsString()?.ifEmpty { null }
    // Bazel rejects a user without a password, and the IDE also treats such credentials as unknown
    val userInfo = if (userName != null && password != null) "${encodeURIComponent(userName)}:${encodeURIComponent(password)}@" else ""
    val proxyUrl = "$userInfo${configuration.host}:${configuration.port}"
    val exceptions = configuration
      .exceptions
      .split(',')
      .mapNotNull { it.trim().toNoProxyEntry() }
    return buildMap {
      // Bazel can read both, so to avoid mix-up we put both cases
      putEnvUpperAndLowerCase("HTTP_PROXY", proxyUrl)
      putEnvUpperAndLowerCase("HTTPS_PROXY", proxyUrl)
      // Always set, so that NO_PROXY of the parent environment does not apply to the IDE proxy
      putEnvUpperAndLowerCase("NO_PROXY", exceptions.joinToString(","))
    }
  }

  private fun isEnabled(): Boolean = BazelApplicationSettingsService
    .getInstance()
    .settings
    .forwardProxySettings

  /**
   * Converts an IDE proxy exception to a `NO_PROXY` entry, or returns `null` when Bazel has no equivalent.
   *
   * Bazel does not support wildcards in `NO_PROXY`. An entry with a leading dot matches only the subdomains, as `*.` does in the IDE.
   * So `*.corp.com` becomes `.corp.com`. An entry without the dot, such as `corp.com`, also matches the host itself.
   * Bazel cannot express other wildcards, so the function drops them.
   *
   * See `ProxyHelper.createProxyIfNeeded` in the Bazel sources:
   * https://github.com/bazelbuild/bazel/blob/9.3.1/src/main/java/com/google/devtools/build/lib/bazel/repository/downloader/ProxyHelper.java
   */
  private fun String.toNoProxyEntry(): String? {
    val entry = when {
      startsWith("*.") -> substring(1)
      else -> this
    }
    return entry.takeIf { it.isNotEmpty() && '*' !in it }
  }

  private fun MutableMap<String, String>.putEnvUpperAndLowerCase(name: String, value: String) {
    put(name.uppercase(), value)
    put(name.lowercase(), value)
  }
}
