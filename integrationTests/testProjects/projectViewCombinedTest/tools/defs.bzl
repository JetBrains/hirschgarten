load("@rules_java//java:java_binary.bzl", "java_binary")
load("@rules_java//java:java_test.bzl", "java_test")

def _custom_java_lib_impl(ctx):
    compilation = java_common.compile(
        ctx,
        source_files = ctx.files.srcs,
        deps = [dep[JavaInfo] for dep in ctx.attr.deps],
        output = ctx.outputs.jar,
        java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    )
    return [
        DefaultInfo(files = depset([ctx.outputs.jar])),
        compilation,
    ]

custom_java_lib = rule(
    implementation = _custom_java_lib_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = [".java"]),
        "deps": attr.label_list(providers = [JavaInfo]),
        "_java_toolchain": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_toolchain"),
        ),
    },
    outputs = {
        "jar": "lib%{name}.jar",
    },
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    fragments = ["java"],
    provides = [JavaInfo],
)

def custom_binary(name, **kwargs):
    java_binary(name = name, **kwargs)

def custom_test(name, **kwargs):
    java_test(name = name, **kwargs)
