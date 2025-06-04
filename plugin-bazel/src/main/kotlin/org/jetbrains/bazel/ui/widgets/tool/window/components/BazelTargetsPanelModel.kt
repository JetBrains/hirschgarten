package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.application.runInEdt
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
    val filteredTargets = if (targetFilter == TargetFilter.OFF) targets else targets.filter { targetFilter.predicate(it.value) }

    // Then, apply the search query
    val searchResults =
      if (searchQuery.isEmpty()) {
        searchRegex = null
        filteredTargets
      } else {
        val regex = searchQuery.toRegex(options)
        searchRegex = regex
        filteredTargets
          .filterKeys { target ->
            regex.containsMatchIn(target.toString()) || regex.containsMatchIn(target.toShortString(project))
          }
      }

    // Finally, sort the results
    visibleTargets = searchResults.keys.sortedBy { it.toShortString(project) }

    targetsPanel?.let {
      runInEdt {
        it.update()
      }
    }
  }

  var searchRegex: Regex? = null
    private set

  val isSearchActive: Boolean
    get() = searchQuery.isNotEmpty()

  internal var targetFilter = TargetFilter.OFF
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
      targetsPanel?.let {
        runInEdt {
          it.update()
        }
      }
    }

  val hasAnyTargets: Boolean
    get() = targets.isNotEmpty()

  fun updateTargets(newTargets: Map<Label, BuildTarget>) {
    targets = newTargets

    updateVisibleTargets()
  }
}
