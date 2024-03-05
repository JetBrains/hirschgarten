package org.jetbrains.plugins.bsp.android

import com.android.ide.common.repository.GradleCoordinate
import com.android.projectmodel.ExternalAndroidLibrary
import com.android.projectmodel.ExternalLibraryImpl
import com.android.projectmodel.RecursiveResourceFolder
import com.android.tools.idea.project.ModuleBasedClassFileFinder
import com.android.tools.idea.project.getPackageName
import com.android.tools.idea.projectsystem.AndroidModuleSystem
import com.android.tools.idea.projectsystem.CapabilityNotSupported
import com.android.tools.idea.projectsystem.CapabilityStatus
import com.android.tools.idea.projectsystem.ClassFileFinder
import com.android.tools.idea.projectsystem.DependencyScopeType
import com.android.tools.idea.projectsystem.DependencyType
import com.android.tools.idea.projectsystem.ManifestOverrides
import com.android.tools.idea.projectsystem.NamedModuleTemplate
import com.android.tools.idea.projectsystem.SampleDataDirectoryProvider
import com.android.tools.idea.projectsystem.ScopeType
import com.android.tools.idea.res.MainContentRootSampleDataDirectoryProvider
import com.android.tools.idea.util.toPathString
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.nio.file.Path

public class BspAndroidModuleSystem(override val module: Module) : AndroidModuleSystem,
  SampleDataDirectoryProvider by MainContentRootSampleDataDirectoryProvider(module) {
  override val moduleClassFileFinder: ClassFileFinder = ModuleBasedClassFileFinder(module)

  override fun getModuleTemplates(targetDirectory: VirtualFile?): List<NamedModuleTemplate> = emptyList()

  override fun analyzeDependencyCompatibility(
    dependenciesToAdd: List<GradleCoordinate>,
  ): Triple<List<GradleCoordinate>, List<GradleCoordinate>, String> = Triple(emptyList(), emptyList(), "")

  override fun getRegisteredDependency(coordinate: GradleCoordinate): GradleCoordinate? = null

  override fun getResolvedDependency(coordinate: GradleCoordinate, scope: DependencyScopeType): GradleCoordinate? = null

  override fun getDependencyPath(coordinate: GradleCoordinate): Path? = null

  override fun canRegisterDependency(type: DependencyType): CapabilityStatus = CapabilityNotSupported()

  override fun registerDependency(coordinate: GradleCoordinate): Unit = throw UnsupportedOperationException()

  override fun registerDependency(coordinate: GradleCoordinate, type: DependencyType): Unit =
    throw UnsupportedOperationException()

  override fun getAndroidLibraryDependencies(scope: DependencyScopeType): Collection<ExternalAndroidLibrary> {
    val result = mutableListOf<ExternalAndroidLibrary>()

    OrderEnumerator.orderEntries(module).withoutSdk().recursively().exportedOnly().forEachLibrary { library ->
      createExternalAndroidLibrary(library)?.let { result += it }
      true
    }

    return result
  }

  private fun createExternalAndroidLibrary(library: Library): ExternalAndroidLibrary? {
    val name = library.name ?: return null

    val roots = library.getFiles(OrderRootType.CLASSES)
    val manifestFile = roots.firstOrNull { it.name == "AndroidManifest.xml" } ?: return null
    val resFolder = roots.firstOrNull { it.name == "res" }
    val symbolsFile = roots.firstOrNull { it.name == "R.txt" }

    return ExternalLibraryImpl(
      address = name,
      manifestFile = manifestFile.toPathString(),
      resFolder = resFolder?.let { RecursiveResourceFolder(it.toPathString()) },
      symbolFile = symbolsFile?.toPathString(),
    )
  }

  override fun getResourceModuleDependencies(): List<Module> = emptyList()

  override fun getDirectResourceModuleDependents(): List<Module> = emptyList()

  override fun canGeneratePngFromVectorGraphics(): CapabilityStatus = CapabilityNotSupported()

  override fun getManifestOverrides(): ManifestOverrides = ManifestOverrides()

  override fun getPackageName(): String? = getPackageName(module)

  override fun getResolveScope(scopeType: ScopeType): GlobalSearchScope =
    module.getModuleWithDependenciesAndLibrariesScope(false)
}
