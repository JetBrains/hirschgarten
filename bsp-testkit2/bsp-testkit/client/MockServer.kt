package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.*

interface MockServer : BuildServer, ScalaBuildServer, JavaBuildServer, JvmBuildServer, CppBuildServer, PythonBuildServer