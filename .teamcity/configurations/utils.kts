package configurations

object DockerParams {
  fun get(): Map<String, String> =
    mapOf(
      "plugin.docker.imagePlatform" to "linux",
      "plugin.docker.pull.enabled" to "true",
      "plugin.docker.imageId" to "registry.jetbrains.team/p/bazel/docker/hirschgarten-e2e:latest",
      "plugin.docker.run.parameters" to "-v %system.agent.persistent.cache%:/home/hirschuser/.cache",
    )
}

object CommonParams {
  val BazelTestlogsArtifactRules: String = "+:%system.teamcity.build.checkoutDir%/testlogs/** => testlogs.zip"
}
