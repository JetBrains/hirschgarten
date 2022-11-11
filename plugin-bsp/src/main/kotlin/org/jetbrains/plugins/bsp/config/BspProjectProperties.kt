package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.ConvertableToState
import org.jetbrains.plugins.bsp.services.ValueServiceWhichNeedsToBeInitialized

public data class BspProjectPropertiesState(
  public var isBspProject: Boolean = false,
)

public data class BspProjectProperties(
  public val isBspProject: Boolean = false,
) : ConvertableToState<BspProjectPropertiesState> {

  override fun toState(): BspProjectPropertiesState =
    BspProjectPropertiesState(
      isBspProject = isBspProject,
    )

  public companion object {
    public fun fromState(state: BspProjectPropertiesState): BspProjectProperties =
      BspProjectProperties(
        isBspProject = state.isBspProject,
      )
  }
}

@State(
  name = "BspProjectPropertiesService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true
)
public class BspProjectPropertiesService :
  ValueServiceWhichNeedsToBeInitialized<BspProjectProperties>(BspProjectProperties()),
  PersistentStateComponent<BspProjectPropertiesState> {

  override fun getState(): BspProjectPropertiesState =
    value.toState()

  override fun loadState(state: BspProjectPropertiesState) {
    value = BspProjectProperties.fromState(state)
  }

  public companion object {
    public fun getInstance(project: Project): BspProjectPropertiesService =
      project.getService(BspProjectPropertiesService::class.java)
  }
}
