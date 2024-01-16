@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.bsp.workspace

import BspSourcesTask
import com.intellij.ide.impl.isTrusted
import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifyEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.getInstance
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformerHelper.calculateRawPackagePrefix
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.server.tasks.InverseSourcesTask
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.workspacemodel.storage.BspEntitySource
import java.net.URI

public class AssignFileToModuleListener(private val project: Project) : BulkFileListener {
    override fun after(events: MutableList<out VFileEvent>) {
        if (!project.isTrusted())
            return
        if (!project.isBspProject)
            return
        val logger = Logger.getInstance(AssignFileToModuleListener::class.java)
        val workspaceModel = WorkspaceModel.getInstance(project)
        val builder = workspaceModel.getBuilderSnapshot()
        val modules = workspaceModel.currentSnapshot.entities(ModuleEntity::class.java)
        val moduleNameProvider = project.findModuleNameProvider()
        if (moduleNameProvider == null) {
            logger.error("Could not find moduleNameProvider")
            return
        }
        events
            .filterIsInstance<VFileCreateEvent>()
            .forEach { event ->
                val file = event.file ?: return@forEach
                if (file.isDirectory) return@forEach
                val url = VirtualFileUrlManager.getInstance(project).fromUrl(file.url)
                val repositoryRoot =
                    project.rootDir.toNioPath() // TODO check if it should be project root or base path
                if (file.path.toNioPathOrNull()?.startsWith(repositoryRoot) != true) {
                    return@forEach
                }

                val target = InverseSourcesTask(project)
                    .getInverseSources(url.url)
                    ?.targets?.singleOrNull()
                if (target == null) {
                    logger.debug("Source file can potentially belong to more than one target")
                    return@forEach
                }

                val sourceDir = BspSourcesTask(project).connectAndExecute(target)
                if (sourceDir == null) {
                    logger.debug("Could not retrieve soruces for the target")
                    return@forEach
                }
                val packagePrefix = calculateRawPackagePrefix(
                    url.toPath().parent.toUri(),
                    sourceDir.items.single().roots.orEmpty()
                        .map { URI.create(it) }) // TODO unify the code with org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.SourcesItemToJavaSourceRootTransformer.getSourceRootsAsURIs
                val moduleName = target //TODO throw if here is more than one target here
                    .uri
                    ?.let(moduleNameProvider)
                if (moduleName == null) {
                    logger.debug("Could not find target matching the source file")
                    return@forEach
                }
                val module = modules.find { m -> m.name == moduleName }
                if (module == null) {
                    logger.debug("Could not find module matching the source target")
                    return@forEach
                }
                val contentRoot = builder.builder.addEntity(
                    ContentRootEntity(
                        url = url,
                        excludedPatterns = emptyList(),
                        entitySource = BspEntitySource
                    ) {
                        this.module = module
                    }
                )
                val sourceRoot = builder.builder.addEntity(
                    SourceRootEntity(
                        url = url,
                        rootType = "java-source", //TODO java-test
                        entitySource = BspEntitySource,
                    ) {
                        this.contentRoot = contentRoot
                    })
                builder.builder.addEntity(
                    JavaSourceRootPropertiesEntity(
                        generated = false,
                        packagePrefix = packagePrefix,
                        entitySource = BspEntitySource,
                    ) { this.sourceRoot = sourceRoot },
                )
                builder.builder.modifyEntity(module) {
                    this.contentRoots += listOf(contentRoot)
                }
            }
        workspaceModel.replaceProjectModel(builder.getStorageReplacement())
    }
}