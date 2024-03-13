package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bsp.utils.extractGoBuildTarget
import org.jetbrains.magicmetamodel.ModuleNameProvider
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.*
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
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
                val goBuildInfo = extractGoBuildTarget(buildTarget) ?: error("extract nie działa")
                GoModuleDependency(
                    importPath = goBuildInfo.importPath ?: error("nie ma importPatha"),
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
