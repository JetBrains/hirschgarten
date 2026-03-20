@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.h2.mvstore.WriteBuffer
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ClassDiscriminator
import java.util.IdentityHashMap

internal data class ClassInfo(
  @JvmField val aClass: Class<out BuildTargetData>,
  @JvmField val id: Byte,
)

internal object BuildDataTargetTypeRegistry {
  private val classToInfo: IdentityHashMap<Class<out BuildTargetData>, ClassInfo>
  private val idToInfo: Int2ObjectOpenHashMap<ClassInfo>

  init {
    val sealedSubclasses = BuildTargetData::class.sealedSubclasses

    val classToInfo = IdentityHashMap<Class<out BuildTargetData>, ClassInfo>(sealedSubclasses.size)
    val idToInfo = Int2ObjectOpenHashMap<ClassInfo>(sealedSubclasses.size)

    fun registerClass(aClass: Class<out BuildTargetData>, id: Byte) {
      val info = ClassInfo(aClass = aClass, id = id)
      classToInfo.put(aClass, info)
      idToInfo.put(info.id.toInt(), info)
    }

    for (klass in sealedSubclasses) {
      val classDiscriminator =
        requireNotNull(klass.annotations.filterIsInstance<ClassDiscriminator>().firstOrNull()) {
          "Class discriminator annotation is missing for ${klass.qualifiedName}"
        }

      val idAsShort = classDiscriminator.id
      require(idAsShort in 1..255) { "Class id must be in range 1..255" }
      registerClass(klass.java, idAsShort.toByte())
    }

    this.classToInfo = classToInfo
    this.idToInfo = idToInfo
  }

  fun writeClassId(aClass: Class<*>, out: WriteBuffer): ClassInfo {
    val info = requireNotNull(classToInfo.get(aClass)) { "Class $aClass is not registered" }
    out.put(info.id)
    return info
  }

  fun getClass(id: Int): Class<out BuildTargetData> = requireNotNull(idToInfo.get(id)) { "Class with id $id is not registered" }.aClass
}
