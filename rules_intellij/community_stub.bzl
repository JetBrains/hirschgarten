"""Provides a no-op stub of the monorepo-only @community repository."""

def _community_stub_impl(repository_ctx):
    repository_ctx.file("REPO.bazel", "")
    repository_ctx.file("platform/build-scripts/bazel-rules/BUILD.bazel", "")
    repository_ctx.file(
        "platform/build-scripts/bazel-rules/content_module_jar.bzl",
        "def content_module_jar(**kwargs):\n    pass\n",
    )
    repository_ctx.file("build/BUILD.bazel", "")
    repository_ctx.file(
        "build/tests-options.bzl",
        "def jps_test(**kwargs):\n    pass\n",
    )

community_stub = repository_rule(
    implementation = _community_stub_impl,
)
