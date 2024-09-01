package configurations

object DockerParams {
  fun get(): Map<String, String> =
    mapOf(
      "plugin.docker.imagePlatform" to "linux",
      "plugin.docker.pull.enabled" to "true",
      "plugin.docker.imageId" to "registry.jetbrains.team/p/bazel/docker/hirschgarten-e2e:latest",
    )
}
