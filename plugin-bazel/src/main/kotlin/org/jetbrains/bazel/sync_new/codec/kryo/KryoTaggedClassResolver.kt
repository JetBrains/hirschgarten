package org.jetbrains.bazel.sync_new.codec.kryo

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ClassTag(val serialId: Int)
