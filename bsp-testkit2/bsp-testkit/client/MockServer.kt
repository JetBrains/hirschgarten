package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.CppBuildServer
import ch.epfl.scala.bsp4j.JavaBuildServer
import ch.epfl.scala.bsp4j.JvmBuildServer
import ch.epfl.scala.bsp4j.PythonBuildServer
import ch.epfl.scala.bsp4j.RustBuildServer
import ch.epfl.scala.bsp4j.ScalaBuildServer

interface MockServer : BuildServer, ScalaBuildServer, JavaBuildServer, JvmBuildServer, CppBuildServer,
  PythonBuildServer, RustBuildServer