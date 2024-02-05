package org.jetbrains.plugins.bsp.android

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.xml.XmlSchemaProvider
import org.jetbrains.android.AndroidXmlSchemaProvider
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.isBspProject

/**
 * This is a hack because [XmlSchemaProvider.findSchema] takes the parent directory when resolving the module.
 * Therefore, we have to resolve the module ourselves if we add the AndroidManifest.xml as a single-file source.
 * ```java
 * final PsiDirectory directory = baseFile.getParent();
 * final Module module = ModuleUtilCore.findModuleForPsiElement(directory == null ? baseFile : directory);
 * ```
 */
public class SingleFileAndroidXmlSchemaProvider : XmlSchemaProvider() {
  private val androidXmlSchemaProvider = AndroidXmlSchemaProvider()

  override fun getSchema(url: String, module: Module?, baseFile: PsiFile): XmlFile? {
    // When the module isn't resolved, actually use the single-file source for finding it instead of failing.
    val correctModule = module ?: ModuleUtilCore.findModuleForPsiElement(baseFile) ?: return null
    return androidXmlSchemaProvider.getSchema(url, correctModule, baseFile)
  }

  override fun isAvailable(file: XmlFile): Boolean =
    BspFeatureFlags.isAndroidSupportEnabled && file.project.isBspProject && androidXmlSchemaProvider.isAvailable(file)

  override fun getAvailableNamespaces(file: XmlFile, tagName: String?): Set<String> =
    androidXmlSchemaProvider.getAvailableNamespaces(file, tagName)

  override fun getDefaultPrefix(namespace: String, context: XmlFile): String? =
    androidXmlSchemaProvider.getDefaultPrefix(namespace, context)

  override fun getLocations(namespace: String, context: XmlFile): Set<String>? =
    androidXmlSchemaProvider.getLocations(namespace, context)
}
