"""
Simple test for provider propagation using a custom aspect
"""
load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _schema_impl(ctx):
    return [DefaultInfo()]

schema = rule(
    implementation = _schema_impl,
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
)

def _gen_java_aspect_impl(target, ctx):
    generated = ctx.actions.declare_file("Generated.java")
    ctx.actions.write(
        output = generated,
        content = "package gen;\n\npublic final class Generated {\n    public static String name() {\n        return \"%s\";\n    }\n}\n" % ctx.label.name,
    )
    output_jar = ctx.actions.declare_file("lib" + ctx.label.name + "_gen.jar")
    java_info = java_common.compile(
        ctx,
        source_files = [generated],
        output = output_jar,
        java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    )
    return [java_info]

gen_java_aspect = aspect(
    implementation = _gen_java_aspect_impl,
    attrs = {
        "_java_toolchain": attr.label(default = Label("@rules_java//toolchains:current_java_toolchain")),
    },
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    fragments = ["java"],
)

def _gen_java_library_impl(ctx):
    return [java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])]

gen_java_library = rule(
    implementation = _gen_java_library_impl,
    attrs = {
        "deps": attr.label_list(aspects = [gen_java_aspect]),
    },
    provides = [JavaInfo],
)
