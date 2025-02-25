package org.jetbrains.bazel.server.bsp

import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectSyncService

class BazelServices(val projectSyncService: ProjectSyncService, val executeService: ExecuteService)
