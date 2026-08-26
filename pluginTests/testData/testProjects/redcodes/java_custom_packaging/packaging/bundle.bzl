"""
A source-less "packaging" rule

`bundle` has no sources. It merges one generated jar with the JavaInfo of the
source module `//lib:leaf`. So its output-jars library holds two jars:
`leaf`'s own jar and the generated `Extra` jar. `leaf` comes through a private
attribute, so the sync aspect reports no module dependency edge for it.
"""

load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _bundle_impl(ctx):
    generated = ctx.actions.declare_file("Extra.java")
    ctx.actions.write(
        output = generated,
        content = "package gen;\n\npublic final class Extra {\n" +
                  "    public static String tag() {\n        return \"x\";\n    }\n}\n",
    )
    output_jar = ctx.actions.declare_file("lib" + ctx.label.name + "_extra.jar")
    gen_info = java_common.compile(
        ctx,
        source_files = [generated],
        output = output_jar,
        java_toolchain = ctx.attr._java_toolchain[java_common.JavaToolchainInfo],
    )

    # The merged JavaInfo carries both jars: leaf's jar and the generated jar.
    return [java_common.merge([gen_info, ctx.attr._leaf[JavaInfo]])]

bundle = rule(
    implementation = _bundle_impl,
    attrs = {
        "_leaf": attr.label(
            default = Label("//lib:leaf"),
            providers = [JavaInfo],
        ),
        "_java_toolchain": attr.label(
            default = Label("@rules_java//toolchains:current_java_toolchain"),
        ),
    },
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    fragments = ["java"],
    provides = [JavaInfo],
)
