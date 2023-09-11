package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension

import ch.epfl.scala.bsp4j.PackageFeatures
import org.jetbrains.magicmetamodel.impl.ConvertableFromState

public class FeaturesServiceValue(
    public val enabled: MutableMap<String, MutableSet<String>>,
    private var packageFeatures: List<PackageFeatures>,
    // Maps to flattened enabling map, which allows to quickly obtain information, which features particular feature enables
    private val packageIdToFlattenedEnablingMap: MutableMap<String, FlattenedFeaturesDependenciesMap> = mutableMapOf(),
    // Maps to flattened disabling map, which works similarly to enabling one
    private val packageIdToFlattenedDisablingMap: MutableMap<String, FlattenedFeaturesDependenciesMap> = mutableMapOf(),
) {
    public fun toState(): FeaturesServiceValueState {
        return FeaturesServiceValueState(
            enabled,
            packageFeatures.map { it.toState() },
            packageIdToFlattenedEnablingMap.map { it.key to it.value.toState() }.toMap(),
            packageIdToFlattenedDisablingMap.map { it.key to it.value.toState() }.toMap(),
        )
    }

    // Constructor for converting from cached state
    public constructor(
        state: FeaturesServiceValueState,
    ) : this(
        state.enabledFeatures,
        state.packageFeatures.map { it.fromState() },
        state.packageIdToFlattenedEnablingMap.map { it.key to it.value.fromState() }.toMap().toMutableMap(),
        state.packageIdToFlattenedDisablingMap.map { it.key to it.value.fromState() }.toMap().toMutableMap(),
    )

    public fun updatePackageFeatures(newPackageFeatures: List<PackageFeatures>) {
        packageFeatures = newPackageFeatures
    }

    public fun calculateFlattenedFeaturesMaps() {
        packageIdToFlattenedEnablingMap.clear()
        packageIdToFlattenedDisablingMap.clear()
        packageFeatures.forEach { p ->
            val flattenedEnablingMap =
                FlattenedFeaturesDependenciesMap.enablingForGivenPackage(p)
            packageIdToFlattenedEnablingMap[p.packageId] = flattenedEnablingMap
            val flattenedDisablingMap =
                FlattenedFeaturesDependenciesMap.disablingFromGivenEnabling(
                    flattenedEnablingMap
                )
            packageIdToFlattenedDisablingMap[p.packageId] = flattenedDisablingMap
        }
    }

    public fun getEnabledFeaturesForPackage(packageId: String): Set<String> {
        return enabled.getOrPut(packageId) {
            val defaultState = getFlattenedEnablingMap(packageId).getOrDefault("default", emptySet()).toMutableSet()
            defaultState.add("default")
            defaultState
        }
    }

    public fun getPackageFeatures(): Set<PackageFeatures> = packageFeatures.toSet()

    public fun getFlattenedEnablingMap(packageId: String): FlattenedFeaturesDependenciesMap =
        packageIdToFlattenedEnablingMap[packageId] ?: FlattenedFeaturesDependenciesMap(emptyMap())

    public fun getFlattenedDisablingMap(packageId: String): FlattenedFeaturesDependenciesMap =
        packageIdToFlattenedDisablingMap[packageId] ?: FlattenedFeaturesDependenciesMap(emptyMap())
}

public data class FeaturesServiceValueState(
    var enabledFeatures: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var packageFeatures: List<PackageFeaturesState> = emptyList(),
    var packageIdToFlattenedEnablingMap: Map<String, FlattenedFeaturesDependenciesMapState> = mutableMapOf(),
    var packageIdToFlattenedDisablingMap: Map<String, FlattenedFeaturesDependenciesMapState> = mutableMapOf(),
)

public data class PackageFeaturesState(
    var packageId: String = "",
    var availableFeatures: Map<String, List<String>> = emptyMap(),
): ConvertableFromState<PackageFeatures> {

    override fun fromState(): PackageFeatures = PackageFeatures(
        packageId,
        emptyList(), // empty list as the targets are not stored
        availableFeatures,
        emptyList(), // empty set as the server's enabled features are not stored
    )
}

/// We don't store the targets and enabled features in the state, as for now they are not used
public fun PackageFeatures.toState(): PackageFeaturesState = PackageFeaturesState(
    this.packageId,
    this.availableFeatures,
)