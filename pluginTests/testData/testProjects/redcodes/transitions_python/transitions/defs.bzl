load("@rules_python//python:defs.bzl", "PyInfo")

def _mode_transition_impl(settings, attr):
    return {"//settings:mode": attr.mode}

_mode_transition = transition(
    implementation = _mode_transition_impl,
    inputs = [],
    outputs = ["//settings:mode"],
)

def _mode_py_library_impl(ctx):
    transitive_sources = depset(
        direct = ctx.files.srcs,
        transitive = [d[PyInfo].transitive_sources for d in ctx.attr.deps],
    )
    return [
        DefaultInfo(files = depset(ctx.files.srcs)),
        PyInfo(transitive_sources = transitive_sources),
    ]

mode_py_library = rule(
    implementation = _mode_py_library_impl,
    attrs = {
        "srcs": attr.label_list(allow_files = [".py"]),
        "mode": attr.string(mandatory = True, values = ["a", "b"]),
        "deps": attr.label_list(cfg = _mode_transition, providers = [[PyInfo]]),
    },
    provides = [PyInfo],
)
