package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildClient

interface JoinedBuildClient : BuildClient, BazelBuildClient
