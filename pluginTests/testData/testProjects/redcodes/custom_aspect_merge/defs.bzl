"""
This test aims to mirror proto + grpc generators (merging aspect providers)
"""
load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _schema_impl(ctx):
    return [DefaultInfo()]

schema = rule(
    implementation = _schema_impl,
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
)

def _generate_and_compile(ctx, class_name, value):
    generated = ctx.actions.declare_file(class_name + ".java")
    ctx.actions.write(
        output = generated,
        content = "package gen;\n\npublic final class " + class_name + " {\n" +
                  "    public static String value() {\n        return \"" + value + "\";\n    }\n}\n",
    )
    output_jar = ctx.actions.declare_file("lib" + ctx.label.name + "_" + class_name + ".jar")
    return java_common.compile(
        ctx,
        source_files = [generated],
        output = output_jar,
        java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    )

def _message_aspect_impl(target, ctx):
    return [_generate_and_compile(ctx, "Message", "message")]

gen_message_aspect = aspect(
    implementation = _message_aspect_impl,
    attrs = {
        "_java_toolchain": attr.label(default = Label("@rules_java//toolchains:current_java_toolchain")),
    },
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    fragments = ["java"],
    provides = [JavaInfo],
)

def _stub_aspect_impl(target, ctx):
    return [_generate_and_compile(ctx, "Stub", "stub")]

gen_stub_aspect = aspect(
    implementation = _stub_aspect_impl,
    attrs = {
        "_java_toolchain": attr.label(default = Label("@rules_java//toolchains:current_java_toolchain")),
    },
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    fragments = ["java"],
    provides = [JavaInfo],
)

def _message_library_impl(ctx):
    return [java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])]

message_library = rule(
    implementation = _message_library_impl,
    attrs = {"deps": attr.label_list(aspects = [gen_message_aspect])},
    provides = [JavaInfo],
)

def _stub_library_impl(ctx):
    return [java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])]

stub_library = rule(
    implementation = _stub_library_impl,
    attrs = {"deps": attr.label_list(aspects = [gen_stub_aspect])},
    provides = [JavaInfo],
)
