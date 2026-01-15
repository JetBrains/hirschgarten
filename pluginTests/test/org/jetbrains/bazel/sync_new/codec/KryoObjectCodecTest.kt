package org.jetbrains.bazel.sync_new.codec

import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import com.intellij.openapi.components.service
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.TestFixtures
import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.codec.kryo.KryoRegistryService
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.codec.kryo.useKryo
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

@ExtendWith(AvoidDiscoveryExtension::class)
@TestFixtures
@TestApplication
internal class KryoObjectCodecTest {

  val project = projectFixture()

  @Test
  fun `test tagged serialization`() {
    @Tagged
    @ClassTag(100)
    data class TaggedClass(
      @field:Tag(1)
      val myField: String,

      @field:Tag(2)
      val myInt: Int,
    )

    service<KryoRegistryService>().registerType(TaggedClass::class.java)

    val taggedClass = TaggedClass(
      myField = "hello",
      myInt = 354768562,
    )

    val output = Output(/* bufferSize = */ 64, /* maxBufferSize = */ -1)
    useKryo { kryo -> kryo.writeClassAndObject(output, taggedClass) }

    val input = Input(output.toBytes())
    val result = useKryo { kryo -> kryo.readClassAndObject(input) }
    result.shouldBe(taggedClass)
  }

  @Test
  fun `test same schema different definitions serialization`() {
    @Tagged
    data class Class1(
      @field:Tag(1)
      val myField: String,

      @field:Tag(2)
      val myList: List<String>,

      @field:Tag(3)
      val myInt: Int,
    )

    @Tagged
    data class Class2(
      @field:Tag(1)
      val myOtherField: String,

      @field:Tag(2)
      val myOtherList: List<String>,

      @field:Tag(3)
      val myOtherInt: Int,
    )

    val kryoService = service<KryoRegistryService>()
    kryoService.registerType(Class1::class.java)
    kryoService.registerType(Class2::class.java)

    val class1 = Class1(
      myField = "1",
      myList = listOf("2"),
      myInt = 3,
    )

    val buffer = UnsafeByteBufferCodecBuffer.allocateHeap();
    val codec1 = codecBuilderOf().ofKryo<Class1>()
    codec1.encode(UnsafeCodecContext, buffer, class1)

    buffer.flip()
    val codec2 = codecBuilderOf().ofKryo<Class2>()
    val class2 = codec2.decode(UnsafeCodecContext, buffer)

    class1.myField.shouldBe(class2.myOtherField)
    class1.myList.shouldBe(class2.myOtherList)
    class1.myInt.shouldBe(class2.myOtherInt)
  }

  @Test
  fun `test sealed interface serialization`() {
    data class MyClass(
      val myList: List<SealedClass>
    )

    service<KryoRegistryService>().registerType(MyClass::class.java)
    service<KryoRegistryService>().registerType(SealedClass::class.java)

    val myClass = MyClass(
      myList = listOf(SealedClass.Data1("hello"), SealedClass.Data2(123), SealedClass.Data3(true))
    )

    val output = Output(/* bufferSize = */ 64, /* maxBufferSize = */ -1)
    useKryo { kryo -> kryo.writeClassAndObject(output, myClass) }

    val input = Input(output.toBytes())
    val result = useKryo { kryo -> kryo.readClassAndObject(input) }
    result.shouldBe(myClass)
  }

  @Test
  fun `test tagged enum serialization`() {
    data class MyClass(
      val myEnum: List<MyEnum>
    )

    service<KryoRegistryService>().registerType(MyClass::class.java)
    service<KryoRegistryService>().registerType(MyEnum::class.java)

    val myClass = MyClass(myEnum = listOf(MyEnum.CONST_1, MyEnum.CONST_2, MyEnum.CONST_3))

    val output = Output(/* bufferSize = */ 64, /* maxBufferSize = */ -1)
    useKryo { kryo -> kryo.writeClassAndObject(output, myClass) }

    val input = Input(output.toBytes())
    val result = useKryo { kryo -> kryo.readClassAndObject(input) }
    result.shouldBe(myClass)
  }

}

private class AvoidDiscoveryExtension : BeforeAllCallback {
  override fun beforeAll(context: ExtensionContext?) {
    KryoRegistryService.disableClassDiscovery = true
  }

}

@Tagged
@ClassTag(100)
@SealedTagged
private sealed interface SealedClass {
  @SealedTag(1)
  @Tagged
  data class Data1(
    @field:Tag(1)
    val myString: String,
  ) : SealedClass

  @SealedTag(2)
  @Tagged
  data class Data2(
    @field:Tag(1)
    val myInt: Int,
  ) : SealedClass

  @SealedTag(3)
  @Tagged
  data class Data3(
    @field:Tag(1)
    val myBoolean: Boolean,
  ) : SealedClass
}

@ClassTag(100)
@EnumTagged
private enum class MyEnum {
  @EnumTag(1)
  CONST_1,

  @EnumTag(2)
  CONST_2,

  @EnumTag(3)
  CONST_3,
}
