load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _mode_transition_impl(settings, attr):
    return {"//settings:mode": attr.mode}

_mode_transition = transition(
    implementation = _mode_transition_impl,
    inputs = [],
    outputs = ["//settings:mode"],
)

def _mode_java_library_impl(ctx):
    output_jar = ctx.actions.declare_file("lib" + ctx.label.name + ".jar")
    java_info = java_common.compile(
        ctx,
        source_files = ctx.files.srcs,
        output = output_jar,
        deps = [d[JavaInfo] for d in ctx.attr.deps],
        java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    )
    return [java_info, DefaultInfo(files = depset([output_jar]))]

mode_java_library = rule(
    implementation = _mode_java_library_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = [".java"]),
        "mode": attr.string(mandatory = True, values = ["a", "b"]),
        "deps": attr.label_list(cfg = _mode_transition, providers = [[JavaInfo]]),
        "_java_toolchain": attr.label(default = Label("@rules_java//toolchains:current_java_toolchain")),
    },
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    fragments = ["java"],
    provides = [JavaInfo],
)
