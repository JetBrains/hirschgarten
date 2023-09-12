package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension

import ch.epfl.scala.bsp4j.PackageFeatures
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.services.ValueServiceWhichNeedsToBeInitialized

@State(
  name = "FeaturesStateService",
  storages = [Storage("featuresState.xml")],
)
@Service(Service.Level.PROJECT)
public class FeaturesService :
  ValueServiceWhichNeedsToBeInitialized<FeaturesServiceValue>(),
  PersistentStateComponent<FeaturesServiceValueState> {
  override fun getState(): FeaturesServiceValueState =
    if (hasInitializedValue()) value.toState() else FeaturesServiceValueState()

  override fun loadState(state: FeaturesServiceValueState) {
    init(FeaturesServiceValue(state))
  }

  public fun setFeaturesState(packageFeatures: List<PackageFeatures>) {
    if (!hasInitializedValue()) {
      init(FeaturesServiceValue(mutableMapOf(), packageFeatures))
    } else {
      value.updatePackageFeatures(packageFeatures)
    }
    value.calculateFlattenedFeaturesMaps()
  }

  public fun areCargoFeaturesInitialized(): Boolean = hasInitializedValue()

  public companion object {
    public fun getInstance(project: Project): FeaturesService =
      project.getService(
        FeaturesService::class.java
      )
  }
}
