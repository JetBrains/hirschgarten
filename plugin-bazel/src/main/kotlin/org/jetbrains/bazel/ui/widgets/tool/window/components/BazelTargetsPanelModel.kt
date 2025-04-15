package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.bsp.protocol.BuildTarget

@Service(Service.Level.PROJECT)
class BazelTargetsPanelModel(private val project: Project) {
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
    if (!searchQuery.isEmpty()) {
      performSearch(searchQuery.toRegex(options))
    }
  }

  var searchRegex: Regex? = null
    set(value) {
      field = value
      update()
    }

  val isSearchActive: Boolean
    get() =  searchQuery.isNotEmpty()

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
    private set

  var displayAsTree: Boolean = true
    set(value) {
      field = value
      buildTargetTree?.updateTree()
    }

  var searchInProgress: Boolean = false

  // Computed properties
  val isEmpty: Boolean
    get() = targets.isEmpty()

  fun updateTargets(newTargets: Map<Label, BuildTarget>, newInvalidTargets: List<Label> = emptyList()) {
    targets = newTargets
    invalidTargets = newInvalidTargets
    // Reset search results when targets change
    searchResults = emptyList()
    searchInProgress = false
    buildTargetTree?.updateTree()
  }

  private fun performSearch(regex: Regex) {
    searchResults =
      targets.keys
        .toList()
        .map { Pair(it.toShortString(project), it) }
        .filter {
          // Search in target ID string representation
          val targetString = it.first
          regex.containsMatchIn(targetString)
        }.sortedBy { it.first }
        .map { it.second }
  }
}
