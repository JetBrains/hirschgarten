# Demo project: resolution of generated and provider-forwarded code

Minimal reproduction of the problems fixed in this PR and in
JetBrains/intellij-aspect#143. Open this directory as a Bazel project, run a
plain sync (no build) and check the four scenarios. Each maps to commits in
the PR table.

| Package | Scenario | Broken behavior without the fixes |
|---|---|---|
| `genscala`, `genpy` | generated sources | `Data`/`data.D` in the consumers do not resolve at all after a plain sync: the generated files are in no output group and never exist on disk (aspect side) |
| `genscala`, `genpy` | multi-hop navigation | with only the aspect fixes, go to definition opens the generated file, but inside it `Part`/`part.P` (another generated library) does not resolve: Python navigation lands in hard-link copies outside all content roots, Scala dead-ends in srcjar views because the library-shadows-module index never matches |
| `thrift` | scrooge sources | `Row` resolves to a decompiled class file; attaching sources manually has no effect. `.srcjar` is not an archive type, its library root is a `file://` URL, and the srcjar is never materialized on sync |
| `fwdtest` | test classification | `Lib` does not resolve in `Prod.scala` but resolves fine from any test: the library's only visible executable is the same-package sourceless test the codegen macro emits, so it is classified as test sources; the forwarding rule contributes no dependency edges (aspect side), so the production binary is invisible to reachability |

All four resolve correctly with this PR plus intellij-aspect#143.
