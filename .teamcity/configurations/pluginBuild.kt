package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel

/**
 * Build configuration for building the Bazel plugin for a specific platform.
 * 
 * @param platform The IntelliJ platform version (e.g., "251", "252")
 */
class PluginBuild(private val platform: String) : BaseBuildType(
    name = "[build] build project - $platform",
    artifactRules = "+:%system.agent.persistent.cache%/bazel/_bazel_hirschuser/*/execroot/_main/bazel-out/k8-fastbuild/bin/plugin-bazel/plugin-bazel.zip",
    steps = {
        val platformDot = "${platform.take(2)}.${platform.last()}"

        bazel {
            name = "build //... $platform"
            id = "build_all_$platform"
            command = "build"
            targets = "//..."
            arguments = "--define=ij_product=intellij-20$platformDot ${CommonParams.BazelCiBuildSpecificArgs}"
            logging = BazelStep.Verbosity.Diagnostic
            DockerParams.get().forEach { (key, value) ->
                param(key, value)
            }
        }
    }
)

/**
 * Factory for creating project build configurations for all platforms.
 */
object PluginBuildFactory {
    /** All project build configurations for different platforms. */
    val ForAllPlatforms: List<BaseBuildType> by lazy { createBuildsForAllPlatforms() }
    
    private fun createBuildsForAllPlatforms(): List<BaseBuildType> =
        CommonParams.CrossBuildPlatforms.map { platform ->
          PluginBuild(platform)
        }
}
