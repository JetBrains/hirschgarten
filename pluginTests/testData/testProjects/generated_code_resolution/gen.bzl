"""Minimal code generators mimicking dataset/partition style codegen macros."""

load("@rules_java//java:defs.bzl", "JavaInfo")
load("@rules_python//python:defs.bzl", "PyInfo")

def _gen_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.file)
    ctx.actions.write(out, ctx.attr.text)
    return [DefaultInfo(files = depset([out]))]

gen = rule(
    implementation = _gen_impl,
    attrs = {
        "file": attr.string(mandatory = True),
        "text": attr.string(mandatory = True),
    },
)

def _fwd_impl(ctx):
    # forwards the providers of the wrapped library through a custom attribute,
    # deliberately without DefaultInfo, like multi-language dataset rules do
    providers = []
    if ctx.attr.jvm:
        providers.append(ctx.attr.jvm[JavaInfo])
    if ctx.attr.py:
        providers.append(ctx.attr.py[PyInfo])
    return providers

fwd = rule(
    implementation = _fwd_impl,
    attrs = {
        "jvm": attr.label(providers = [JavaInfo]),
        "py": attr.label(providers = [PyInfo]),
    },
)
