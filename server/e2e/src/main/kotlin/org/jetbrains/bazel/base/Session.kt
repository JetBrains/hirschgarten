package org.jetbrains.bazel.base

import org.jetbrains.bsp.protocol.JoinedBuildServer

class Session(val client: MockClient, val server: JoinedBuildServer)
