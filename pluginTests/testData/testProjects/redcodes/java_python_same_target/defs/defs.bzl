load("@rules_java//java/common:java_info.bzl", "JavaInfo")
load("@rules_python//python:defs.bzl", "PyInfo")

def _java_and_py_library_impl(ctx):
    return [
        DefaultInfo(files = depset(ctx.files.srcs)),
        ctx.attr.jvm[JavaInfo],
        PyInfo(transitive_sources = depset(direct = ctx.files.srcs)),
    ]

java_and_py_library = rule(
    implementation = _java_and_py_library_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = [".py"]),
        "jvm": attr.label(mandatory = True, providers = [[JavaInfo]]),
    },
    provides = [JavaInfo, PyInfo],
)
