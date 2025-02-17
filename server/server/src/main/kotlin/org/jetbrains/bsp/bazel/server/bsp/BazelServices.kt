package org.jetbrains.bsp.bazel.server.bsp

import org.jetbrains.bsp.bazel.server.sync.ExecuteService
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService

class BazelServices(val projectSyncService: ProjectSyncService, val executeService: ExecuteService)
