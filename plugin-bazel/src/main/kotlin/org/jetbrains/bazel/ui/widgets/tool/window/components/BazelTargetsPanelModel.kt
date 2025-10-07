package org.jetbrains.bazel.ui.widgets.tool.window.components

import kotlinx.coroutines.flow.MutableSharedFlow

internal class BazelTargetsPanelModel(private val updateRequests: MutableSharedFlow<Unit>) {
  var matchCase: Boolean = false
    set(value) {
      field = value
      updateTargets()
    }

  var regexMode: Boolean = false
    set(value) {
      field = value
      updateTargets()
    }

  internal var targetFilter = TargetFilter.OFF
    set(value) {
      field = value
      updateTargets()
    }

  var searchQuery: String = ""
    set(value) {
      field = value
      updateTargets()
    }

  var displayAsTree: Boolean = true
    set(value) {
      field = value
      updateTargets()
    }

  var showHidden: Boolean = false
    set(value) {
      field = value
      updateTargets()
    }

  private fun updateTargets() {
    check(updateRequests.tryEmit(Unit))
  }
}
