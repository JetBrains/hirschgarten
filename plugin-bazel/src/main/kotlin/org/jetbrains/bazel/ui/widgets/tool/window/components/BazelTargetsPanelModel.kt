package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.BuildTarget

// It is a service, because we want to update the state from a sync hook
@Service(Service.Level.PROJECT)
class BazelTargetsPanelModel(private val project: Project) {
  var targetsPanel: BazelTargetsPanel? = null

  // All targets in the project, set by the sync hook
  private var targets: Map<Label, BuildTarget> = emptyMap()

  fun getTargetData(target: Label): BuildTarget? = targets[target]

  var visibleTargets: List<Label> = emptyList()
    private set

  // Invalid targets display logic is currently disabled
  var invalidTargets: List<Label> = emptyList()
    private set

  var matchCase: Boolean = false
    set(value) {
      field = value
      updateVisibleTargets()
    }

  var regexMode: Boolean = false
    set(value) {
      field = value
      updateVisibleTargets()
    }

  private fun updateVisibleTargets() {
    val options =
      setOfNotNull(
        if (!matchCase) RegexOption.IGNORE_CASE else null,
        if (!regexMode) RegexOption.LITERAL else null,
      )

    // First, apply the filter
    val filteredTargets = targets.filter { targetFilter.predicate(it.value) }

    // Then, apply the search query
    val searchResults =
      if (!searchQuery.isEmpty()) {
        val regex = searchQuery.toRegex(options)
        searchRegex = regex
        filteredTargets
          .filterKeys { target ->
            regex.containsMatchIn(target.toString()) || regex.containsMatchIn(target.toShortString(project))
          }
      } else {
        searchRegex = null
        filteredTargets
      }

    // Finally, sort the results
    visibleTargets = searchResults.keys.sortedBy { it.toShortString(project) }

    targetsPanel?.update()
  }

  var searchRegex: Regex? = null
    private set

  val isSearchActive: Boolean
    get() = searchQuery.isNotEmpty()

  var targetFilter = TargetFilter.OFF
    set(value) {
      field = value
      updateVisibleTargets()
    }

  var searchQuery: String = ""
    set(value) {
      field = value
      updateVisibleTargets()
    }

  var displayAsTree: Boolean = true
    set(value) {
      field = value
      targetsPanel?.update()
    }

  val hasAnyTargets: Boolean
    get() = targets.isNotEmpty()

  fun updateTargets(newTargets: Map<Label, BuildTarget>, newInvalidTargets: List<Label> = emptyList()) {
    targets = newTargets.filterKeys { it.isMainWorkspace }
    invalidTargets = newInvalidTargets.filter { it.isMainWorkspace }

    updateVisibleTargets()
  }
}
