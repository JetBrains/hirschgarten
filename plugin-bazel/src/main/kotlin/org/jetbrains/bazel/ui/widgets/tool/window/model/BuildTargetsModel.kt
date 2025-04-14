package org.jetbrains.bazel.ui.widgets.tool.window.model

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.bsp.protocol.BuildTarget
import kotlin.properties.Delegates

/**
 * Model class representing the state of the build targets panel.
 * This class follows the observer pattern, allowing UI components to observe and react to model changes.
 */
class BuildTargetsModel(
    private val project: Project,
) {
    // Listeners
    private val listeners = mutableListOf<() -> Unit>()

  fun update() = {}

    var matchCase: Boolean = false
      set(value) {
        field = value
        update()
      }

    var regexMode: Boolean = false
      set(value) {
        field = value
        update()
      }


    var targetFilter = TargetFilter.FILTER.OFF

    // Model state
    var targets: List<BuildTarget> by Delegates.observable(emptyList(),
    ) { _, _, _ -> notifyListeners() }

    var invalidTargets: List<Label> by Delegates.observable(
        emptyList(),
    ) { _, _, _ -> notifyListeners() }

    var searchQuery: String by Delegates.observable("") { _, _, _ -> notifyListeners() }

    var searchResults: List<BuildTarget> by Delegates.observable(emptyList()) { _, _, _ -> notifyListeners() }

    var displayAsTree: Boolean by Delegates.observable(false) { _, _, _ -> notifyListeners() }

    var searchInProgress: Boolean by Delegates.observable(false) { _, _, _ -> notifyListeners() }

    // Computed properties
    val isEmpty: Boolean
        get() = targets.isEmpty()

    val isSearchActive: Boolean
        get() = searchQuery.isNotEmpty()

    // Methods
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    fun updateTargets(newTargets: Collection<BuildTarget>, newInvalidTargets: List<Label> = emptyList()) {
        targets = newTargets.sortedBy { it.id.toShortString(project) }
        invalidTargets = newInvalidTargets
        // Reset search results when targets change
        searchResults = emptyList()
    }

    fun updateSearchQuery(newQuery: Regex) {
        // If the query is empty, reset search results and mark search as not in progress
        if (newQuery.pattern.isEmpty()) {
            searchResults = emptyList()
            searchInProgress = false
            searchQuery = newQuery
        } else {
            // If the query is not empty, set the search query and mark search as in progress
            // This will trigger the listener in BuildTargetSearch to perform the search
            searchQuery = newQuery
            searchInProgress = true
        }
    }

    fun updateSearchResults(results: List<BuildTarget>) {
        searchResults = results
        searchInProgress = false
    }

  private fun getCurrentSearchQuery(): Regex {
    val options =
      setOfNotNull(
        if (!matchCase) RegexOption.IGNORE_CASE else null,
        if (!regexMode) RegexOption.LITERAL else null,
      )
    return textField.text.toRegex(options)
  }

}
