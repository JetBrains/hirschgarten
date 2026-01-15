package org.jetbrains.bazel.sync_new.codec.kryo

import com.intellij.openapi.extensions.ExtensionPointName
import io.github.classgraph.ClassGraph
import org.jetbrains.annotations.TestOnly

class KryoRegistryService {
  companion object {
    @TestOnly
    var disableClassDiscovery: Boolean = false
  }

  val registeredTypes: MutableList<RegisteredType> = mutableListOf()

  init {
    if (!disableClassDiscovery) {
      registeredTypes.addAll(discoverRegisteredTypes())
    }
  }

  fun discoverRegisteredTypes(): List<RegisteredType> {
    val extraClassLoaders = KryoRegistryExtension.ep.extensionList
      .flatMap { it.extraClassLoaders }
    val thisClassLoader = KryoRegistryService::class.java.classLoader
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

  fun registerType(type: Class<*>) {
    val serialId = type.getAnnotation(ClassTag::class.java)?.serialId ?: -1
    registeredTypes += RegisteredType(type = type, serialId = serialId)
  }
}

data class RegisteredType(val type: Class<*>, val serialId: Int)

interface KryoRegistryExtension {
  /**
   * Specify additional class loaders for classpath scanning
   */
  val extraClassLoaders: List<ClassLoader>

  companion object {
    val ep: ExtensionPointName<KryoRegistryExtension> = ExtensionPointName("org.jetbrains.bazel.kryoRegistryExtension")
  }
}
