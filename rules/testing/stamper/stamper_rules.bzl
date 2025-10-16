load("@rules_java//java:defs.bzl", "JavaInfo")
load("@rules_java//java/common:java_common.bzl", "java_common")

def _jvm_test_stamper(ctx):
    output_jar = ctx.actions.declare_file(ctx.attr.name + "_test_marker.jar")

    args = ctx.actions.args()
    args.add("--output", output_jar.path)

    inputs = []
    java_infos = []
    for target in ctx.attr.targets:
        java_info = target[JavaInfo]
        java_infos.append(java_info)
        for output in java_info.java_outputs:
            for my_attr in ["class_jar", "compile_jar"]:
                if hasattr(output, my_attr):
                    jar = getattr(output, my_attr)
                    inputs.append(jar)
                    args.add(jar.path)

    ctx.actions.run(
        inputs = inputs,
        outputs = [output_jar],
        executable = ctx.executable._stamper,
        arguments = [args],
    )

    return [
        JavaInfo(
            output_jar = output_jar,
            compile_jar = output_jar,
        ),
    ]

#     my_java_info = JavaInfo(
#         output_jar = output_jar,
#         compile_jar = output_jar,
#     )
#     return [
#         java_common.merge(java_infos + [my_java_info]),
#     ]

jvm_test_stamper = rule(
    implementation = _jvm_test_stamper,
    attrs = {
        "targets": attr.label_list(
            providers = [JavaInfo],
        ),
        "_stamper": attr.label(
            executable = True,
            cfg = "exec",
            default = "//rules/testing/stamper:test_stamper_tool",
        ),
        "_java_toolchain": attr.label(
            default = "@@bazel_tools//tools/jdk:current_java_toolchain",
        ),
    },
    fragments = ["java"],
    provides = [JavaInfo],
    toolchains = ["@@bazel_tools//tools/jdk:toolchain_type"],
)
