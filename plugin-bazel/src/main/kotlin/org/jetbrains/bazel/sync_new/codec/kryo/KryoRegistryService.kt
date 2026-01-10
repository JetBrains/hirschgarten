package org.jetbrains.bazel.sync_new.codec.kryo

import com.intellij.ide.ApplicationActivity
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import io.github.classgraph.ClassGraph

class KryoRegistryService {
  val registeredTypes: List<RegisteredType>

  init {
    registeredTypes = discoverRegisteredTypes()
  }

  fun discoverRegisteredTypes(): List<RegisteredType> {
    val extraClassLoaders = KryoRegistryExtension.ep.extensionList
      .flatMap { it.extraClassLoaders }
    val thisClassLoader = KryoStartupActivity::class.java.classLoader
    val classGraph = ClassGraph()
      .enableAnnotationInfo()
      .ignoreParentClassLoaders()
      .overrideClassLoaders(*(extraClassLoaders + listOf(thisClassLoader)).toTypedArray())
      .scan()

    return classGraph.use { scanResult ->
      val result = mutableListOf<RegisteredType>()
      for (classInfo in scanResult.getClassesWithAnnotation(ClassTag::class.java)) {
        val parameters = classInfo.getAnnotationInfo(ClassTag::class.java)
          .getParameterValues(true)
        val serialId = parameters.getValue(ClassTag::serialId.name) as? Int ?: error("serialId is not specified for ${classInfo.name}")
        val registeredType = RegisteredType(
          type = classInfo.loadClass(),
          serialId = serialId,
        )
        result += registeredType
      }
      result
    }
  }
}

data class RegisteredType(val type: Class<*>, val serialId: Int)

class KryoStartupActivity : ApplicationActivity {
  override suspend fun execute() {

  }
}

interface KryoRegistryExtension {
  /**
   * Specify additional class loaders for classpath scanning
   */
  val extraClassLoaders: List<ClassLoader>

  companion object {
    val ep: ExtensionPointName<KryoRegistryExtension> = ExtensionPointName("org.jetbrains.bazel.kryoRegistryExtension")
  }
}
