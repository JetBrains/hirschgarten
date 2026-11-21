load("@rules_java//java/common:java_common.bzl", "java_common")

def _bridge_impl_srcjar(ctx):
    toolchain = ctx.toolchains["@bazel_tools//tools/jdk:toolchain_type"]

    bridge_impl = ctx.actions.declare_file("src/com/example/impl/BridgeImpl.kt")
    ctx.actions.write(bridge_impl, """package com.example.impl

import com.example.spec.Bridge

class BridgeImpl : Bridge {
  override fun name(): String = "generated"
}
""")
    src_jar = ctx.actions.declare_file(ctx.label.name + ".srcjar")
    java_common.pack_sources(ctx.actions, sources = [bridge_impl], java_toolchain = toolchain.java, output_source_jar = src_jar)
    return [DefaultInfo(files = depset([src_jar]))]

bridge_impl_srcjar = rule(
    implementation = _bridge_impl_srcjar,
    fragments = ["java"],
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    attrs = {
        "_java_toolchain": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_toolchain"),
        ),
    },
)
