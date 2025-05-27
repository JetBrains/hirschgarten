package configurations

object DockerParams {
  val volumes = """
    -v %system.agent.persistent.cache%:/home/hirschuser/.cache
    -v %system.agent.persistent.cache%/.netrc:/home/hirschuser/.netrc""".trimIndent()

  fun get(
    imageName: String = CommonParams.DockerE2eImage
  ): Map<String, String> =
    mapOf(
      "plugin.docker.imagePlatform" to "linux",
      "plugin.docker.pull.enabled" to "true",
      "plugin.docker.imageId" to imageName,
      "plugin.docker.run.parameters" to volumes,
    )
}

object CommonParams {
  val BazelTestlogsArtifactRules: String = "+:%system.teamcity.build.checkoutDir%/testlogs/** => testlogs.zip"

  //  val BazelCiSpecificArgs: String = "--config=remotecache --config=nocacheupload --test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
  val BazelCiSpecificArgs: String = "--test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"

  //  val BazelCiBuildSpecificArgs: String = "--config=remotecache --config=bes --test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
  val BazelCiBuildSpecificArgs: String =
    "--test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"

  val DockerE2eImage: String = "registry.jetbrains.team/p/bazel/docker/hirschgarten-e2e:latest"
  val DockerQodanaImage: String = "registry.jetbrains.team/p/bazel/docker-private/hirschgarten-qodana"
  val DockerQodanaAndroidImage: String = "registry.jetbrains.team/p/bazel/docker-private/hirschgarten-qodana-android"

  val QodanaArtifactRules: String = "+:plugin-*.zip=>%system.teamcity.build.checkoutDir%/tc-artifacts"

  val CrossBuildPlatforms: List<String> = listOf("243", "251", "252")

  val BazelVersion = "7.4.1"
}

object CredentialsStore {
  val GitHubPassword = "credentialsJSON:5bc345d4-e38f-4428-95e1-b6e4121aadf6"
  val SpaceToken = "credentialsJSON:4efcb75d-2f9b-47fd-a63b-fc2969a334f5"
  val BazelTeamcityToken = "credentialsJSON:f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
