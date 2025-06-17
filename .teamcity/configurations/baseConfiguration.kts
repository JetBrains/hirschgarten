package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.Dependencies
import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.ParametrizedWithType
import jetbrains.buildServer.configs.kotlin.v2019_2.Requirements
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot


open class BaseBuildType(
  name: String,
  vcsRoot: GitVcsRoot,
  steps: BuildSteps.() -> Unit,
  artifactRules: String = "",
  failureConditions: FailureConditions.() -> Unit = {},
  requirements: (Requirements.() -> Unit)? = null,
  params: ParametrizedWithType.() -> Unit = {},
  dockerSupport: DockerSupportFeature.() -> Unit = {},
  dependencies: Dependencies.() -> Unit = {},
) : BuildType({

    this.name = name
    this.artifactRules = artifactRules
    this.failureConditions(failureConditions)
    this.params {
      params()
      password("remote.cache.netrc", "credentialsJSON:8f47c6e1-d7b2-4d3a-9b5a-12c8f3d45e92", label = "dotnetrc", description = "nenrc credentials for remote cache", display = ParameterDisplay.HIDDEN)
      param("env.CONTAINER_UID", "")
      param("env.CONTAINER_GID", "")
    }

    this.dependencies(dependencies)

    failureConditions {
      executionTimeoutMin = 60
    }

    vcs {
      root(vcsRoot)
    }

    if (vcsRoot.name == "hirschgarten-github") {
      id("GitHub$name".toExtId())
      if (requirements == null) {
        requirements {
          endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Medium")
          equals("container.engine.osType", "linux")
        }
      } else {
        this.requirements(requirements)
      }
    } else {
      id("Space$name".toExtId())
      requirements {
        endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-XLarge")
        equals("container.engine.osType", "linux")
      }
    }

    this.features.dockerSupport(dockerSupport)

    features {
      perfmon {
      }
      if (vcsRoot.name == "hirschgarten-github") {
        commitStatusPublisher {
          publisher =
            github {
              githubUrl = "https://api.github.com"
              authType =
                personalToken {
                  token = Utils.CredentialsStore.GitHubPassword
                }
            }
          param("github_oauth_user", "hb-man")
        }
        pullRequests {
          vcsRootExtId = "${vcsRoot.id}"
          provider =
            github {
              authType =
                token {
                  token = Utils.CredentialsStore.GitHubPassword
                }
              filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
            }
        }
      } else {
        commitStatusPublisher {
          vcsRootExtId = "${vcsRoot.id}"
          publisher =
            space {
              authType =
                connection {
                  connectionId = "PROJECT_EXT_12"
                }
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

object GitHubVcs : GitVcsRoot({
  name = "hirschgarten-github"
  url = "https://github.com/JetBrains/hirschgarten.git"
  branch = "main"
  branchSpec = "+:refs/heads/*"
  authMethod =
    password {
      userName = "hb-man"
      password = Utils.CredentialsStore.GitHubPassword
    }
  param("oauthProviderId", "tc-cloud-github-connection")
  param("tokenType", "permanent")
})

object SpaceVcs : GitVcsRoot({
  name = "hirschgarten-space"
  url = "https://git.jetbrains.team/bazel/hirschgarten.git"
  branch = "main"
  branchSpec = """
    +:refs/heads/*
    +:(refs/merge/*)
""".trimIndent()
  authMethod =
    password {
      userName = "x-oauth-basic"
      password = Utils.CredentialsStore.SpaceToken
    }
  param("oauthProviderId", "PROJECT_EXT_15")
  param("tokenType", "permanent")
})
