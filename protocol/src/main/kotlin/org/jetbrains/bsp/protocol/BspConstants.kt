package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildClientCapabilities

const val BSP_CONNECTION_DIR: String = ".bsp"

const val BSP_DISPLAY_NAME: String = "BSP"

const val BSP_CLIENT_NAME: String = "IntelliJ-BSP"

const val BSP_CLIENT_VERSION: String = "2024.1-EAP"

const val BSP_VERSION: String = "2.1.0"

val CLIENT_CAPABILITIES: BuildClientCapabilities =
  BuildClientCapabilities(listOf("java", "kotlin", "scala", "python")).apply {
    jvmCompileClasspathReceiver = true
  }
