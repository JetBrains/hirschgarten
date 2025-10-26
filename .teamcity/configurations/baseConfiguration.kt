package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.Dependencies
import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.Requirements
import jetbrains.buildServer.configs.kotlin.v2019_2.VcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ant


open class BaseBuildType(
  name: String,
  steps: BuildSteps.() -> Unit,
  artifactRules: String = "",
  failureConditions: FailureConditions.() -> Unit = {},
  requirements: (Requirements.() -> Unit)? = null,
  params: ParametrizedWithType.() -> Unit = {},
  dockerSupport: DockerSupportFeature.() -> Unit = {},
  dependencies: Dependencies.() -> Unit = {},
  customVcsRoot: VcsRoot? = VcsRootHirschgarten,
  idNamespace: String? = null,
) : BuildType({
    val isGitHub = (customVcsRoot?.id == VcsRootHirschgarten.id)
    val isSpace = (customVcsRoot?.id == VcsRootHirschgartenSpace.id)

    this.name = name
    this.artifactRules = artifactRules
    this.failureConditions(failureConditions)
    this.params {
      params()
      param("env.CONTAINER_UID", "")
      param("env.CONTAINER_GID", "")
    }

    this.dependencies(dependencies)

    failureConditions {
      executionTimeoutMin = 60
    }

    vcs {
      customVcsRoot?.let { root(it) }
    }

    if (idNamespace != null) {
      id("${idNamespace}$name".toExtId())
    } else {
      if (isGitHub) {
        id("GitHub$name".toExtId())
      } else if (isSpace) {
        id("Space$name".toExtId())
      } else {
        id(name.toExtId())
      }
    }

    if (requirements == null) {
      requirements {
        if (isGitHub) {
          endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Medium")
        } else {
          endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-XLarge")
        }
        equals("container.engine.osType", "linux")
      }
    } else {
      this.requirements(requirements)
    }

    this.features.dockerSupport(dockerSupport)

    features {
      perfmon { }
      // Determine feature set by VCS root type (GitHub-like vs Space), not by id namespace
      val isGitHubLike = listOf(
        VcsRootHirschgarten.id,
        VcsRootBazelQodana.id,
        VcsRootBuildBuddyQodana.id
      ).contains(customVcsRoot?.id)
      val isSpaceLike = (customVcsRoot?.id == VcsRootHirschgartenSpace.id)

      if (isGitHubLike) {
        commitStatusPublisher {
          publisher =
            github {
              githubUrl = "https://api.github.com"
              authType = personalToken { token = CredentialsStore.GitHubPassword }
            }
          param("github_oauth_user", "hb-man")
        }
        pullRequests {
          vcsRootExtId = "${VcsRootHirschgarten.id}"
          provider = github {
            authType = token { token = CredentialsStore.GitHubPassword }
            filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
          }
        }
      } else if (isSpaceLike) {
        commitStatusPublisher {
          vcsRootExtId = "${VcsRootHirschgartenSpace.id}"
          publisher = space {
            authType = connection { connectionId = "PROJECT_EXT_12" }
            displayName = "BazelTeamCityCloud"
          }
        }
      }
    }
    this.steps {
      ant {
        this.name = "Get UID and GID"
        mode = antScript { // language=Ant
          content = """
                    <project>
                      <exec executable="id" outputproperty="uid.output">
                        <arg value="-u"/>
                      </exec>
                      <exec executable="id" outputproperty="gid.output">
                        <arg value="-g"/>
                      </exec>
                      <echo message="Current UID: ${'$'}{uid.output}"/>
                      <echo message="Current GID: ${'$'}{gid.output}"/>
                      <property name="teamcity.buildConfName" value="dummy"/>
                      <echo message="##teamcity[setParameter name='env.CONTAINER_UID' value='${'$'}{uid.output}']"/>
                      <echo message="##teamcity[setParameter name='env.CONTAINER_GID' value='${'$'}{gid.output}']"/>
                    </project>
                """.trimIndent()
        }
      }
      steps()
    }
  })
