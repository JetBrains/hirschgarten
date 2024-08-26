package configurations

object DockerParams {
    fun get(): Map<String, String> {
        return mapOf(
            "plugin.docker.imagePlatform" to "linux",
            "plugin.docker.pull.enabled" to "true",
            "plugin.docker.imageId" to "registry.jetbrains.team/p/bazel/docker/hirschgarten-base:latest"
        )
    }
}