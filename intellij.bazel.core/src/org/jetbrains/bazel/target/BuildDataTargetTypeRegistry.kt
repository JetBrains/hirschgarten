@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.intellij.openapi.components.Service
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineScope
import org.h2.mvstore.WriteBuffer
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ClassDiscriminator
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicReference

internal data class ClassInfo(
  @JvmField val aClass: Class<out BuildTargetData>,
  @JvmField val id: Byte,
)

@Service(Service.Level.APP)
internal class BuildDataTargetTypeRegistry(val coroutineScope: CoroutineScope) {
  private class Container(
    val classToInfo: IdentityHashMap<Class<out BuildTargetData>, ClassInfo>,
    val idToInfo: Int2ObjectOpenHashMap<ClassInfo>,
  )

  private val container: AtomicReference<Container> = AtomicReference(null)

  init {
    rebuild()
    LanguagePlugin.EP_NAME.addChangeListener(coroutineScope) { rebuild() }
  }

  private fun rebuild() {
    val registeredSubClasses = LanguagePlugin.EP_NAME.extensionList.flatMap { it.providedBuildTargetTypes }.toSet()

    val classToInfo = IdentityHashMap<Class<out BuildTargetData>, ClassInfo>(registeredSubClasses.size)
    val idToInfo = Int2ObjectOpenHashMap<ClassInfo>(registeredSubClasses.size)

    fun registerClass(aClass: Class<out BuildTargetData>, id: Byte) {
      val info = ClassInfo(aClass = aClass, id = id)
      classToInfo.put(aClass, info)
      idToInfo.put(info.id.toInt(), info)
    }

    for (klass in registeredSubClasses) {
      val classDiscriminator =
        requireNotNull(klass.annotations.filterIsInstance<ClassDiscriminator>().firstOrNull()) {
          "Class discriminator annotation is missing for ${klass}"
        }

      val idAsShort = classDiscriminator.id
      require(idAsShort in 1..255) { "Class id must be in range 1..255" }
      registerClass(klass.java, idAsShort.toByte())
    }

    container.set(Container(classToInfo, idToInfo))
  }

  fun writeClassId(aClass: Class<*>, out: WriteBuffer): ClassInfo {
    val classToInfo = container.get()?.classToInfo ?: error("container not initialized")
    val info = requireNotNull(classToInfo.get(aClass)) { "Class $aClass is not registered" }
    out.put(info.id)
    return info
  }

  fun getClass(id: Int): Class<out BuildTargetData> {
    val idToInfo = container.get()?.idToInfo ?: error("container not initialized")
    return requireNotNull(idToInfo.get(id)) { "Class with id $id is not registered" }.aClass
  }
}
