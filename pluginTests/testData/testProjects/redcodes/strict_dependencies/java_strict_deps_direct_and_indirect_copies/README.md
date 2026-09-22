# java_strict_deps_direct_and_indirect_copies

Reproduction for [BAZEL-3569](https://youtrack.jetbrains.com/issue/BAZEL-3569).

The variant of `java_strict_deps_direct_and_indirect` where the two targets providing
the class `B` do not share a source file, but declare their own copy of it:
`direct/B.java` in `//:lib_b_direct`, which `//:main` depends on directly, and
`indirect/B.java` in `//:lib_b_indirect`, which is reachable only through `//:lib_a`.
This is the closest hermetic equivalent of the reported setup, where the same class
came from two distinct jars of two separate maven installs.

Under strict deps, `B` is legal in `Main.java` because the directly declared
`//:lib_b_direct` provides it. Any red code on the `B` reference in `Main.java` is
therefore a false positive.
