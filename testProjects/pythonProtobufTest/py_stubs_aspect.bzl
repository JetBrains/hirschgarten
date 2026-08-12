load("@rules_python//python:defs.bzl", "PyInfo")

def _py_stubs_aspect_impl(target, ctx):
    """Extract Python stubs from PyInfo and expose them through an output group, so that they can be materialized in the
    bazel output base and picked up by the IDE.

    This is especially useful for `py_proto_library` since proto-generated Python source files are totally worthless to
    IDEs for import resolution.

    This logic should probably be integrated into the Bazel plugin's own aspect.
    """
    stubs = getattr(target[PyInfo], "transitive_pyi_files", None)
    if not stubs:
        return []
    return [OutputGroupInfo(python_stubs = stubs)]

py_stubs_aspect = aspect(
    implementation = _py_stubs_aspect_impl,
    attr_aspects = ["deps"],
    required_providers = [[PyInfo]],
)
