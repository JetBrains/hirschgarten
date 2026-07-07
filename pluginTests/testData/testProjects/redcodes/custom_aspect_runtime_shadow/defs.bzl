load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")

GenInfo = provider(fields = {"marker": "gates the codegen aspect"})

def _schema_impl(ctx):
    return [GenInfo(marker = True)]

schema = rule(
    implementation = _schema_impl,
)

def _gen_jvm_aspect_impl(target, ctx):
    if GenInfo not in target:
        return []

    generated = ctx.actions.declare_file("{}_gen/Generated.java".format(target.label.name))
    ctx.actions.write(
        output = generated,
        content = "package gen;\n\npublic final class Generated {\n    public static String name() {\n        return \"%s\";\n    }\n}\n" % target.label.name,
    )

    java_toolchain = ctx.toolchains["@bazel_tools//tools/jdk:toolchain_type"].java
    runtime_java_info = ctx.attr._runtime[JavaInfo]
    output_jar = ctx.actions.declare_file("{}_gen.jar".format(target.label.name))

    java_info = java_common.compile(
        ctx,
        source_files = [generated],
        deps = [runtime_java_info],
        exports = [runtime_java_info],
        java_toolchain = java_toolchain,
        output = output_jar,
    )
    return [java_info]

gen_jvm_aspect = aspect(
    implementation = _gen_jvm_aspect_impl,
    attr_aspects = ["deps"],
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"],
    fragments = ["java"],
    attrs = {
        "_runtime": attr.label(
            default = Label("//rt:rt"),
            providers = [JavaInfo],
            cfg = "target",
        ),
    },
)

def _aggregator_impl(ctx):
    java_infos = [dep[JavaInfo] for dep in ctx.attr.deps if JavaInfo in dep]
    return [java_common.merge(java_infos)]

aggregator = rule(
    implementation = _aggregator_impl,
    attrs = {
        "deps": attr.label_list(
            providers = [GenInfo],
            aspects = [gen_jvm_aspect],
        ),
    },
    provides = [JavaInfo],
)
