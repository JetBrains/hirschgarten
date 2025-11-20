package org.jetbrains.bazel.sync_new.connector

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class BazelConnectorService {

  fun ofLegacyTask(taskId: String): BazelConnector {

  }
}
