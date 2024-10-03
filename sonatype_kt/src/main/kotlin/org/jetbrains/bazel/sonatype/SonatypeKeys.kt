package org.jetbrains.bazel.sonatype

import org.apache.commons.cli.*
import kotlin.system.exitProcess

object SonatypeKeys {
  fun parseArguments(args: Array<String>): SonatypeConfig {
    val options = Options()

    options.addOption(Option.builder("u")
      .longOpt("username")
      .hasArg()
      .desc("Username for the Sonatype repository")
      .required(false)
      .build())

    options.addOption(Option.builder("p")
      .longOpt("password")
      .hasArg()
      .desc("Password for the Sonatype repository")
      .required(false)
      .build())

    options.addOption(Option.builder("r")
      .longOpt("repository")
      .hasArg()
      .desc("Sonatype repository URL: e.g. https://oss.sonatype.org/service/local")
      .required(true)
      .build())

    options.addOption(Option.builder("pn")
      .longOpt("profile-name")
      .hasArg()
      .desc("Profile name at Sonatype: e.g. org.xerial")
      .required(true)
      .build())

    options.addOption(Option.builder("s")
      .longOpt("session")
      .hasArg()
      .desc("Used for identifying a Sonatype staging repository")
      .required(false)
      .build())

    options.addOption(Option.builder("c")
      .longOpt("coordinates")
      .hasArg()
      .desc("Coordinates at Sonatype: e.g. org.xerial.sbt-sonatype")
      .required(true)
      .build())

    options.addOption(Option.builder("t")
      .longOpt("timeout")
      .hasArg()
      .desc("Milliseconds before giving up Sonatype API requests")
      .required(false)
      .build())

    options.addOption(Option.builder("l")
      .longOpt("log-level")
      .hasArg()
      .desc("Log level: trace, debug, info, warn, error")
      .required(false)
      .build())

    // Publish-specific options
    options.addOption(Option.builder("pj")
      .longOpt("project-jar")
      .hasArg()
      .desc("Path to project jar")
      .required(true)
      .build())

    options.addOption(Option.builder("psj")
      .longOpt("project-sources-jar")
      .hasArg()
      .desc("Path to project sources jar")
      .required(true)
      .build())

    options.addOption(Option.builder("pdj")
      .longOpt("project-docs-jar")
      .hasArg()
      .desc("Path to project docs jar")
      .required(true)
      .build())

    options.addOption(Option.builder("ppom")
      .longOpt("project-pom")
      .hasArg()
      .desc("Path to project pom file")
      .required(true)
      .build())

    val parser: CommandLineParser = DefaultParser()
    val formatter = HelpFormatter()
    val cmd: CommandLine

    try {
      cmd = parser.parse(options, args)
    } catch (e: ParseException) {
      println("Args: ${args.joinToString(" ")}")
      println(e.message)
      formatter.printHelp("sonatype-cli", options)
      exitProcess(1)
    }

    val username = cmd.getOptionValue("username") ?: System.getenv("SONATYPE_USERNAME")
    ?: throw IllegalArgumentException("SONATYPE_USERNAME is not defined. Provide via --username or environment variable.")

    val password = cmd.getOptionValue("password") ?: System.getenv("SONATYPE_PASSWORD")
    ?: throw IllegalArgumentException("SONATYPE_PASSWORD is not defined. Provide via --password or environment variable.")

    val repositoryUrl = cmd.getOptionValue("repository")
    val profileName = cmd.getOptionValue("profile-name")
    val sessionName = cmd.getOptionValue("session")
      ?: "[bazel-sonatype] ${SonatypeCoordinates.fromString(cmd.getOptionValue("coordinates")).sonatypeArtifactId} ${SonatypeCoordinates.fromString(cmd.getOptionValue("coordinates")).sonatypeVersion}"
    val coordinates = SonatypeCoordinates.fromString(cmd.getOptionValue("coordinates"))
    val timeoutMillis = cmd.getOptionValue("timeout")?.toIntOrNull() ?: 60 * 60 * 1000
    val logLevel = cmd.getOptionValue("log-level") ?: "info"

    val projectJar = cmd.getOptionValue("project-jar")
    val projectSourcesJar = cmd.getOptionValue("project-sources-jar")
    val projectDocsJar = cmd.getOptionValue("project-docs-jar")
    val projectPom = cmd.getOptionValue("project-pom")

    return SonatypeConfig(
      username = username,
      password = password,
      repositoryUrl = repositoryUrl,
      profileName = profileName,
      sessionName = sessionName,
      coordinates = coordinates,
      timeoutMillis = timeoutMillis,
      logLevel = logLevel,
      projectJar = projectJar,
      projectSourcesJar = projectSourcesJar,
      projectDocsJar = projectDocsJar,
      projectPom = projectPom
    )
  }
}
