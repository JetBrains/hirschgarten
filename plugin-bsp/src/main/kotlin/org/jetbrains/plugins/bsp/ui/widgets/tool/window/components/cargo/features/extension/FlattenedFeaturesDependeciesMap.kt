package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension

import ch.epfl.scala.bsp4j.PackageFeatures
import org.jetbrains.magicmetamodel.impl.ConvertableFromState

public class FlattenedFeaturesDependenciesMap(private val map: Map<String, Set<String>>): Map<String, Set<String>> by map {
    public fun toState(): FlattenedFeaturesDependenciesMapState = FlattenedFeaturesDependenciesMapState(map)

    public companion object {
        public fun enablingForGivenPackage(`package`: PackageFeatures): FlattenedFeaturesDependenciesMap {
            val enablementMap: MutableMap<String, Set<String>> = mutableMapOf()
            val availableFeatures = `package`.availableFeatures
            availableFeatures.keys.forEach {
                createFeatureSetEnabledByGivenFeature(
                    availableFeatures,
                    enablementMap,
                    it
                )
            }
            return FlattenedFeaturesDependenciesMap(enablementMap)
        }

        public fun disablingFromGivenEnabling(flattenedEnablingMap: Map<String, Set<String>>): FlattenedFeaturesDependenciesMap {
            val disablementMap = mutableMapOf<String, Set<String>>()
            flattenedEnablingMap.keys.forEach {
                disablementMap[it] = createFeatureSetDisabledByGivenFeature(
                    flattenedEnablingMap,
                    it
                )
            }
            return FlattenedFeaturesDependenciesMap(disablementMap)
        }

        private fun createFeatureSetEnabledByGivenFeature(
            allFeatures: Map<String, List<String>>,
            enablementMap: MutableMap<String, Set<String>>,
            feature: String
        ): Set<String> = enablementMap.getOrPut(feature) {
            val enablementSet = allFeatures.getValue(feature).toMutableSet()
            enablementSet += enablementSet.flatMap {
                createFeatureSetEnabledByGivenFeature(
                    allFeatures,
                    enablementMap,
                    it
                )
            }
            enablementSet
        }

        private fun createFeatureSetDisabledByGivenFeature(
            enablementMap: Map<String, Set<String>>,
            feature: String
        ): Set<String> {
            val disablementSet = mutableSetOf<String>()
            enablementMap.forEach { (key, value) ->
                if (value.contains(feature)) {
                    disablementSet.add(key)
                }
            }
            return disablementSet
        }
    }
}


public data class FlattenedFeaturesDependenciesMapState(
    var map: Map<String, Set<String>> = emptyMap(),
): ConvertableFromState<FlattenedFeaturesDependenciesMap> {
    override fun fromState(): FlattenedFeaturesDependenciesMap = FlattenedFeaturesDependenciesMap(map)
}
