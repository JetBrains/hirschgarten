package configurations

object DockerParams {
  fun get(imageName: String = CommonParams.DockerE2eImage
  ): Map<String, String> =
    mapOf(
      "plugin.docker.imagePlatform" to "linux",
      "plugin.docker.pull.enabled" to "true",
      "plugin.docker.imageId" to imageName,
      "plugin.docker.run.parameters" to "-v %system.agent.persistent.cache%:/home/hirschuser/.cache",
    )
}

object CommonParams {
  val BazelTestlogsArtifactRules: String = "+:%system.teamcity.build.checkoutDir%/testlogs/** => testlogs.zip"
  val BazelCiSpecificArgs: String = "--test_output=errors --announce_rc --show_progress_rate_limit=30 --curses=yes --terminal_columns=140"

  val DockerE2eImage: String = "registry.jetbrains.team/p/bazel/docker/hirschgarten-e2e:latest"
  val DockerQodanaImage: String = "registry.jetbrains.team/p/bazel/docker-private/hirschgarten-qodana"
  val DockerQodanaAndroidImage: String = "registry.jetbrains.team/p/bazel/docker-private/hirschgarten-qodana-android"

  val QodanaArtifactRules: String = "+:intellij-*.zip=>%system.teamcity.build.checkoutDir%/tc-artifacts"

  val CrossBuildPlatforms: List<String> = listOf("243", "251")

  val BazelVersion = "7.4.1"
}
