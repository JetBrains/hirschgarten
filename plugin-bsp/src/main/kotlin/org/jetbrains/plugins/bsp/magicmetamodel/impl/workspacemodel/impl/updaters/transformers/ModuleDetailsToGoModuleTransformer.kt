package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.plugins.bsp.magicmetamodel.ModuleNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GoModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GoModuleDependency
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal class ModuleDetailsToGoModuleTransformer(
    private val targetsMap: Map<BuildTargetId, BuildTargetInfo>,
    private val projectDetails: ProjectDetails,
    moduleNameProvider: ModuleNameProvider,
    projectBasePath: Path,
) : ModuleDetailsToModuleTransformer<GoModule>(targetsMap, moduleNameProvider) {
    override val type = "GO_MODULE"

    override fun transform(inputEntity: ModuleDetails): GoModule {
        val goBuildInfo = extractGoBuildTarget(inputEntity.target) ?: error("extract nie działa")

        return GoModule(
            module = toGenericModuleInfo(inputEntity),
            importPath = goBuildInfo.importPath ?: error("cos nie dziala"),
            root = URI.create(inputEntity.target.baseDirectory).toPath(),
            goDependencies = inputEntity.moduleDependencies.mapNotNull { targetsMap[it] }.map { buildTargetInfo ->
                val buildTarget = projectDetails.targets.find { it.id == buildTargetInfo.id.toBsp4JTargetIdentifier() }
                    ?: error("find nie działa")
                val goBuildInfoo = extractGoBuildTarget(buildTarget) ?: error("extract nie działa")
                GoModuleDependency(
                    importPath = goBuildInfoo.importPath ?: error("nie ma importPatha"),
                    root = URI.create(buildTarget.baseDirectory).toPath()
                )
            }
        )
    }

    override fun toGenericModuleInfo(inputEntity: ModuleDetails): GenericModuleInfo {
        val bspModuleDetails = BspModuleDetails(
            target = inputEntity.target,
            dependencySources = inputEntity.dependenciesSources,
            type = type,
            javacOptions = null,
            pythonOptions = inputEntity.pythonOptions,
            libraryDependencies = inputEntity.libraryDependencies,
            moduleDependencies = inputEntity.moduleDependencies,
            scalacOptions = inputEntity.scalacOptions,
        )

        return bspModuleDetailsToModuleTransformer.transform(bspModuleDetails)
    }
}
