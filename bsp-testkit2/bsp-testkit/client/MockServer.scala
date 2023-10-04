package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j._

trait MockServer
  extends BuildServer
    with ScalaBuildServer
    with JavaBuildServer
    with JvmBuildServer
    with CppBuildServer
    with PythonBuildServer
    with RustBuildServer
