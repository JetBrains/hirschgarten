package org.jetbrains.bazel.sonatype


import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.system.exitProcess

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    println(args.joinToString(" "))
    val logger: Logger = LogManager.getLogger("SonatypeCLI")

    try {
      val (command, config) = SonatypeKeys.parseArguments(args)

      val operations = SonatypeOperations(config)

      when (command) {
        "open" -> {
          operations.openRepo()
          println("Repository opened or created successfully.")
          logger.info("Repository opened or created successfully.")
        }

        "publish" -> {
          operations.bundleRelease()
          println("Bundle published successfully.")
          logger.info("Bundle published successfully.")
        }

        else -> {
          println("Unknown command: $command")
          logger.error("Unknown command: $command")
          exitProcess(1)
        }
      }
    } catch (e: IllegalArgumentException) {
      println("Error: ${e.message}")
      LogManager.getLogger("SonatypeCLI").error("Error: ${e.message}")
      exitProcess(1)
    } catch (e: SonatypeException) {
      println("Sonatype Error: ${e.message}")
      LogManager.getLogger("SonatypeCLI").error("Sonatype Error: ${e.message}")
      exitProcess(1)
    } catch (e: Exception) {
      println("Unexpected Error: ${e.message}")
      LogManager.getLogger("SonatypeCLI").error("Unexpected Error: ${e.message}", e)
      exitProcess(1)
    }
  }
}
