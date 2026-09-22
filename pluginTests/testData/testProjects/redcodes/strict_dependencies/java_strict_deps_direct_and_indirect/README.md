# java_strict_deps_direct_and_indirect

Reproduction for [BAZEL-3569](https://youtrack.jetbrains.com/issue/BAZEL-3569).

`B.java` belongs to two targets: `//:lib_b_direct`, which `//:main` depends on
directly, and `//:lib_b_indirect`, which is reachable only through `//:lib_a`. This
mirrors the reported setup, where the same maven artifact came both from the
workspace's own `maven.install()` and from a second one bundled by another ruleset.

Under strict deps, `B` is legal in `Main.java` because the directly declared
`//:lib_b_direct` provides it. Any red code on the `B` reference in `Main.java` is
therefore a false positive.
