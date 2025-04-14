package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.ui.widgets.tool.window.components.BuildTargetTree
import org.jetbrains.bazel.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.bsp.protocol.BuildTarget

@Service(Service.Level.PROJECT)
class BazelToolwindowModel(private val project: Project) {
  var buildTargetTree: BuildTargetTree? = null

  fun update() = {}

  var matchCase: Boolean = false
    set(value) {
      field = value
      updateRegex()
    }

  var regexMode: Boolean = false
    set(value) {
      field = value
      updateRegex()
    }

  private fun updateRegex() {
    val options =
      setOfNotNull(
        if (!matchCase) RegexOption.IGNORE_CASE else null,
        if (!regexMode) RegexOption.LITERAL else null,
      )
    regex = searchQuery.toRegex(options)
  }

  var regex: Regex = Regex("")
    set(value) {
      field = value
      update()
    }

  var targetFilter = TargetFilter.FILTER.OFF

  // Model state
  var targets: Map<Label, BuildTarget> = emptyMap()
    private set

  var invalidTargets: List<Label> = emptyList()
    private set

  var searchQuery: String = ""
    set(value) {
      field = value
      if (value.isEmpty()) {
        searchResults = emptyList()
        searchInProgress = false
      } else {
        searchInProgress = true
      }
      updateRegex()
    }

  var searchResults: List<Label> = emptyList()

  var displayAsTree: Boolean = false

  var searchInProgress: Boolean = false

  // Computed properties
  val isEmpty: Boolean
    get() = targets.isEmpty()

  val isSearchActive: Boolean
    get() = searchQuery.isNotEmpty()

  fun updateTargets(newTargets: Map<Label, BuildTarget>, newInvalidTargets: List<Label> = emptyList()) {
    targets = newTargets
    invalidTargets = newInvalidTargets
    // Reset search results when targets change
    searchResults = emptyList()
    searchInProgress = false
    buildTargetTree?.updateTree()
  }

  fun updateSearchResults(results: List<Label>) {
    searchResults = results
    searchInProgress = false
  }
}
