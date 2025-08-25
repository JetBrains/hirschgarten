package configurations

object DockerParams {
  private val DOCKER_RUN_PARAMS: String = listOf(
    "-v %system.agent.persistent.cache%:/home/hirschuser/.cache",
    "-u %env.CONTAINER_UID%:%env.CONTAINER_GID%",
  ).joinToString("\n")

  fun get(
    imageName: String = CommonParams.DockerE2eImage
  ): Map<String, String> =
    mapOf(
      "plugin.docker.imagePlatform" to "linux",
      "plugin.docker.pull.enabled" to "true",
      "plugin.docker.imageId" to imageName,
      "plugin.docker.run.parameters" to DOCKER_RUN_PARAMS,
    )
}

object CommonParams {
  val BazelTestlogsArtifactRules: String = "+:%system.teamcity.build.checkoutDir%/testlogs/** => testlogs.zip"

  //  val BazelCiSpecificArgs: String = "--config=remotecache --config=nocacheupload --test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
  val BazelCiSpecificArgs: String = "--test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"

  //  val BazelCiBuildSpecificArgs: String = "--config=remotecache --config=bes --test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"
  val BazelCiBuildSpecificArgs: String =
    "--test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"

  val DockerE2eImage: String = "registry.jetbrains.team/p/ij/containers-public/bazel-plugin-e2e:latest"
  val DockerQodanaImage: String = "registry.jetbrains.team/p/ij/bazel-private/bazel-plugin-qodana"
  val DockerQodanaAndroidImage: String = "registry.jetbrains.team/p/ij/bazel-private/bazel-plugin-qodana-android"
  val DockerQodanaGoImage: String = "registry.jetbrains.team/p/ij/bazel-private/bazel-plugin-qodana-go"

  val QodanaArtifactRules: String = "+:**/plugin-bazel.zip=>%system.teamcity.build.checkoutDir%/tc-artifacts"

  val CrossBuildPlatforms: List<String> = listOf("251", "252")

  val BazelVersion = "7.4.1"
}

object CredentialsStore {
  val GitHubPassword = "credentialsJSON:5bc345d4-e38f-4428-95e1-b6e4121aadf6"
  val BazelTeamcityToken = "credentialsJSON:f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
