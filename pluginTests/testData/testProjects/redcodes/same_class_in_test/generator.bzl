load("@rules_java//java/common:java_common.bzl", "java_common")

def _generated_source(ctx):
    toolchain = ctx.toolchains["@bazel_tools//tools/jdk:toolchain_type"]
    generated_java = ctx.actions.declare_file("Generated.java")
    ctx.actions.write(generated_java, """
                      package com.example;
                      public class Generated { void foo() {} }
                      """)
    src_jar = ctx.actions.declare_file("generated.srcjar")
    java_common.pack_sources(ctx.actions, sources = [generated_java], java_toolchain = toolchain.java, output_source_jar = src_jar)
    return [DefaultInfo(files = depset([src_jar]))]

generated_source = rule(
    implementation = _generated_source,
    fragments = ["java"],
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    attrs = {
        "_java_toolchain": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_toolchain"),
        ),
    },
)
