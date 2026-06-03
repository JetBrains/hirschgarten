load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _custom_java_library(ctx):
    dep = ctx.attr.deps[0]
    original_java_info = dep[JavaInfo]
    java_info = JavaInfo(
        output_jar = original_java_info.java_outputs[0].class_jar,
        compile_jar = original_java_info.java_outputs[0].compile_jar,
        deps = [d[JavaInfo] for d in ctx.attr.deps if JavaInfo in d],
    )
    return [dep[DefaultInfo], java_info]

custom_java_library = rule(
    implementation = _custom_java_library,
    attrs = {
        "deps": attr.label_list(),
    },
    provides = [JavaInfo],
)
