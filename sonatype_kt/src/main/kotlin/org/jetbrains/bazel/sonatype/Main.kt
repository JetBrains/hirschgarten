package org.jetbrains.bazel.sonatype

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.system.exitProcess

object Main {
  @JvmStatic
  fun main(args: Array<String>) {
    val logger: Logger = LogManager.getLogger("SonatypeCLI")

    try {
      val config = SonatypeKeys.parseArguments(args)

      val operations = SonatypeOperations(config)

      operations.bundleRelease()
      logger.info("Bundle published successfully.")
    } catch (e: IllegalArgumentException) {
      logger.error("Error: ${e.message}")
      exitProcess(1)
    } catch (e: SonatypeException) {
      logger.error("Sonatype Error: ${e.message}")
      exitProcess(1)
    } catch (e: Exception) {
      logger.error("Unexpected Error: ${e.message}", e)
      exitProcess(1)
    }
  }
}
