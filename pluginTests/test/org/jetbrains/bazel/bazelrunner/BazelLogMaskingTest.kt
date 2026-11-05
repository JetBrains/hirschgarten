package org.jetbrains.bazel.bazelrunner

import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.junit5.fixture.disposableFixture
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class BazelLogMaskingTest {
  private val disposable by disposableFixture()

  @Test
  fun `the registry key turns the masking off`() {
    Registry.get("bazel.log.mask.secrets").setValue(false, disposable)

    BazelLogMasking.maskEnvValue("GITHUB_TOKEN", "s3cr3t") shouldBe "s3cr3t"
    BazelLogMasking.maskArguments(listOf("--action_env=GITHUB_TOKEN=s3cr3t", "--remote_header=Authorization=Bearer s3cr3t"))
      .shouldBe(listOf("--action_env=GITHUB_TOKEN=s3cr3t", "--remote_header=Authorization=Bearer s3cr3t"))
  }

  @Test
  fun `a sensitive name masks the whole value`() {
    for (name in listOf("BAZELISK_GITHUB_TOKEN", "COURSIER_CREDENTIALS", "BUILDBUDDY_API_KEY", "AWS_SECRET_ACCESS_KEY", "db_password", "NPM_AUTH", "SSH_KEY")) {
      BazelLogMasking.maskEnvValue(name, "s3cr3t") shouldBe "******"
    }
  }

  @Test
  fun `a proxy without a scheme masks the user info`() {
    BazelLogMasking.maskEnvValue("HTTPS_PROXY", "jane:p%40ss@proxy.corp:3128") shouldBe "******@proxy.corp:3128"
    BazelLogMasking.maskEnvValue("http_proxy", "http://jane:pass@proxy.corp:3128") shouldBe "http://******@proxy.corp:3128"
    BazelLogMasking.maskEnvValue("HTTP_PROXY", "proxy.corp:3128") shouldBe "proxy.corp:3128"
    BazelLogMasking.maskEnvValue("ALL_PROXY", "jane:pass@proxy.corp:1080") shouldBe "******@proxy.corp:1080"
  }

  @Test
  fun `other values mask the user info of each URL`() {
    BazelLogMasking.maskEnvValue("PIP_INDEX_URL", "https://jane:pass@pypi.corp/simple") shouldBe "https://******@pypi.corp/simple"
    BazelLogMasking.maskEnvValue("GOPROXY", "https://a:b@go.corp,https://proxy.golang.org,direct")
      .shouldBe("https://******@go.corp,https://proxy.golang.org,direct")
    BazelLogMasking.maskEnvValue("NO_PROXY", "localhost,.corp.com") shouldBe "localhost,.corp.com"
    BazelLogMasking.maskEnvValue("PATH", "/usr/bin:/bin") shouldBe "/usr/bin:/bin"
  }

  @Test
  fun `an environment flag masks the value of its variable`() {
    maskArgument("--action_env=GITHUB_TOKEN=s3cr3t") shouldBe "--action_env=GITHUB_TOKEN=******"
    maskArgument("--test_env=HTTPS_PROXY=jane:pass@proxy.corp:3128") shouldBe "--test_env=HTTPS_PROXY=******@proxy.corp:3128"
    maskArgument("--repo_env=FOO=bar") shouldBe "--repo_env=FOO=bar"
    maskArgument("--action_env=GITHUB_TOKEN") shouldBe "--action_env=GITHUB_TOKEN"
  }

  @Test
  fun `other arguments mask the user info of a URL`() {
    maskArgument("--remote_cache=grpcs://jane:pass@cache.corp") shouldBe "--remote_cache=grpcs://******@cache.corp"
    maskArgument("--remote_proxy=socks5://jane:pass@proxy.corp:1080") shouldBe "--remote_proxy=socks5://******@proxy.corp:1080"
    maskArgument("//foo:bar") shouldBe "//foo:bar"
  }

  @Test
  fun `an at sign after the user info part of a URL is not user info`() {
    maskArgument("--url=https://host/path@v1") shouldBe "--url=https://host/path@v1"
    maskArgument("--url=https://host?owner=jane@corp.com") shouldBe "--url=https://host?owner=jane@corp.com"
    maskArgument("--url=https://host#jane@corp.com") shouldBe "--url=https://host#jane@corp.com"
  }

  @Test
  fun `a fully redacted flag masks its whole value`() {
    maskArgument("--remote_header=Authorization=Bearer s3cr3t") shouldBe "--remote_header=******"
    maskArgument("--bes_header=x-buildbuddy-api-key=s3cr3t") shouldBe "--bes_header=******"
    maskArgument("--remote_cache_header=x-token=s3cr3t") shouldBe "--remote_cache_header=******"
    maskArgument("--remote_exec_header=x-token=s3cr3t") shouldBe "--remote_exec_header=******"
    maskArgument("--remote_downloader_header=x-token=s3cr3t") shouldBe "--remote_downloader_header=******"
    maskArgument("--google_credentials=/home/jane/key.json") shouldBe "--google_credentials=******"
    maskArgument("--tls_client_key=/home/jane/client.key") shouldBe "--tls_client_key=******"
  }

  @Test
  fun `a flag with its value in the next argument masks that value`() {
    BazelLogMasking.maskArguments(listOf("build", "--action_env", "GITHUB_TOKEN=s3cr3t", "//foo:bar"))
      .shouldBe(listOf("build", "--action_env", "GITHUB_TOKEN=******", "//foo:bar"))
    BazelLogMasking.maskArguments(listOf("--remote_header", "Authorization=Bearer s3cr3t", "--action_env", "PATH"))
      .shouldBe(listOf("--remote_header", "******", "--action_env", "PATH"))
    BazelLogMasking.maskArguments(listOf("--google_credentials", "/home/jane/key.json"))
      .shouldBe(listOf("--google_credentials", "******"))
  }

  @Test
  fun `a flag as the last argument stays unchanged`() {
    BazelLogMasking.maskArguments(listOf("build", "--action_env")) shouldBe listOf("build", "--action_env")
    BazelLogMasking.maskArguments(listOf("--remote_header")) shouldBe listOf("--remote_header")
  }

  @Test
  fun `only the first equals sign ends the name`() {
    maskArgument("--action_env=FOO=a=b") shouldBe "--action_env=FOO=a=b"
    maskArgument("--action_env=MY_TOKEN=a=b") shouldBe "--action_env=MY_TOKEN=******"
  }

  @Test
  fun `a name that only contains a part of a pattern keeps its value`() {
    for (name in listOf("KEYBOARD_LAYOUT", "MONKEY", "AUTHOR_NAME", "PWD", "JAVA_HOME")) {
      BazelLogMasking.maskEnvValue(name, "value") shouldBe "value"
    }
  }

  @Test
  fun `a value without user info stays unchanged`() {
    for (value in listOf("https://proxy.golang.org", "http://proxy.corp:3128", "file:///tmp/foo", "git@github.com:org/repo.git", "jane@corp.com")) {
      BazelLogMasking.maskEnvValue("SOME_URL", value) shouldBe value
    }
    BazelLogMasking.maskEnvValue("HTTPS_PROXY", "http://proxy.corp:3128") shouldBe "http://proxy.corp:3128"
    maskArgument("--remote_cache=grpcs://cache.corp") shouldBe "--remote_cache=grpcs://cache.corp"
  }

  private fun maskArgument(argument: String): String = BazelLogMasking.maskArguments(listOf(argument)).single()
}
