package org.jetbrains.bazel.sync_new.lang

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class SyncClassTag(val serialId: Long)

interface SyncTargetData {

}
