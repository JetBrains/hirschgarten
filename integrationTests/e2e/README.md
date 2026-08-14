# OS × Bazel-version matrix for e2e IDE-Starter tests

The e2e IDE-Starter tests run their fixture projects across an explicit OS × Bazel-version
matrix. Each test owns a package here (`e2e/<test-name>/BUILD.bazel`) that declares which
(OS, Bazel version) cells exist; `e2e/BUILD.bazel` aggregates the per-OS suites that CI runs.

Fixture projects are bundled under `plugins/bazel/integrationTests/testProjects/<fixture>`. Tests copy the selected
fixture from the current Ultimate checkout to a disposable workspace. The source fixture stays
unchanged; only the copy receives generated `user.bazelrc` and project-view files. For local Bazel
runs, pass `--nocache_test_results` so fixture changes rerun the test. TeamCity already supplies
this flag.

## Why the declarations live here and not next to the test code

The natural expectation is `e2e/basic-scala/BUILD.bazel` sitting next to `BasicScalaIdeStarterTest.kt`,
or next to the fixture project. Neither location works:

- **Not next to the test sources.** All test classes belong to the single JPS module
  `intellij.bazel.integrationTests`, whose `BUILD.bazel` is generated from the `.iml` and collects
  classes with `glob(["test/**/*.kt"])`. A `BUILD.bazel` file makes its directory a separate Bazel
  package, and globs never cross package boundaries — so a BUILD file next to a test class would
  silently drop that class from `integrationTests_test_lib` (and from the monolithic Linux run).
- **Not next to the fixture.** Directories under `plugins/bazel/integrationTests/testProjects` are test data and are
  excluded from Ultimate's Bazel graph. Matrix declarations must stay reachable from the Ultimate
  build.
- **One package per test, not one shared file.** Target names must be unique within a package, and
  every test declares a `test_suite` named after its OS. Collapsing all tests into a single
  `e2e/BUILD.bazel` would therefore require prefixing every target
  (`//…/e2e:basic-scala_windows` instead of `//…/e2e/basic-scala:windows`).
  That trade — 35 small packages versus one large file with prefixed names
  and a shared merge-conflict surface — was discussed in review and settled in favour of keeping the
  per-test packages and the short `:windows` / `:linux` labels.

Per-test jars (one library per test class instead of the shared `integrationTests_test_lib`) require
splitting the JPS module, which is deferred until the move off JPS.

## Declaring a test's matrix

`ide_starter_matrix.bzl` provides two macros:

```starlark
ide_starter_e2e_matrix(
    test_class = "org.jetbrains.bazel.tests.java.StrictDepsTest",
    bazel_versions = ["8.3.1", "9.0.0"],
    default_bazel_version = "9.0.0",
    os_list = ["linux", "windows"],
)
```

- One `jps_test` cell is generated per (OS, version) pair, named `<os>_bazel_<version>`
  (dots/dashes become underscores): `:windows_bazel_8_3_1`, `:windows_bazel_9_0_0`, …
- A `test_suite` named `<os>` (`:windows`, `:linux`) points at that OS's
  `default_bazel_version` cell — this is what the OS suites in `e2e/BUILD.bazel` and CI use.
- Each cell sets `target_compatible_with` for its OS, filters the class via `JB_TEST_FILTER`,
  and exports `USE_BAZEL_VERSION=<version>`.
- Extra keyword arguments are forwarded to every generated `jps_test`.
- `os_list` **defaults to all three OSes** (`["linux", "macos", "windows"]`). Omitting it therefore
  opts a test into the full OS axis — which is why `protobuf-resolve` and `simple-java-combined`
  carry no `os_list`: they are the two tests that genuinely need Linux/macOS cells of their own
  (an OS-specific hermetic C++ toolchain), so they are declared per OS here.
- Every other test passes `os_list = ["windows"]` on purpose. On Linux those tests already run
  through the monolithic `//plugins/bazel/integrationTests:integrationTests_test` target, so adding
  Linux cells for them would run the same test twice in CI. Folding that monolithic Linux run onto
  the matrix is a follow-up; until then **new tests should declare `os_list = ["windows"]`** unless
  they need an OS-specific fixture setup.
- Cells are tagged `exclusive`, because these tests are not isolated from each other: tests that use
  the same fixture share its copied-project destination (which each test replaces), teardown
  kills any Bazel/JCEF process under `/ide-tests/` machine-wide, the IDE-Starter cache root is
  shared, and on Linux/macOS the nested Bazel output base is keyed by that shared workspace path.
  Per-cell isolation, after which the tag can be dropped, is
  [BAZEL-3393](https://youtrack.jetbrains.com/issue/BAZEL-3393).

`ide_starter_e2e_host_test(test_class, os_list)` is the variant for tests whose project is
supplied dynamically at runtime (hirschgarten-style, via IDE-Starter system properties): it
generates one OS-named test per entry and does not select a Bazel version.

## How the Bazel-version axis works

`USE_BAZEL_VERSION` is read natively by bazelisk inside the launched IDE and wins over the
fixture's committed `.bazelversion`. Nothing rewrites the source fixture:

- The committed `.bazelversion` is simply the default — it is what you get when running a test
  from the IDE without any selection.
- The committed `MODULE.bazel.lock` matches that default. When a matrix cell selects another
  version, Bazel regenerates whatever is stale in the disposable copy
  (default `--lockfile_mode=update`).
- On Windows, the nested Bazel invocations get short isolated roots
  (`startup --output_user_root/--install_base/--output_base`) keyed by project root and
  selected version, so cells never share server state (see `BazelProjectConfigurer`).

## How fixtures stay cross-platform

OS differences live in standard Bazel mechanisms inside each bundled fixture, never in per-OS file
variants:

- **`MODULE.bazel` is identical on every OS** and declares all deps/extensions unconditionally
  (bzlmod is lazy — Windows never fetches the zig SDK unless something requests it). What it
  must *not* do is `register_toolchains()` for OS-specific toolchains.
- **Toolchain registration moved to the committed `.bazelrc`** via
  `common --enable_platform_specific_config` plus `build:<os> --extra_toolchains=...` sections.
  Windows usually has no section and falls through to MSVC autodetection.
- **Per-OS deps use `select()`** on `@platforms//os:*` in BUILD files (select keys resolve
  eagerly, so fixtures declare `bazel_dep(name = "platforms", ...)`).
- **`load()` cannot be select()ed** — targets loading OS-problematic rules move to their own
  package outside the affected OS's sync scope (e.g. the `scala_proto_library` split in
  `protobufStrictDepsTest`).
- Beware rules that self-register toolchains from their *own* module file (e.g. `rules_scala`'s
  protoc toolchains): laziness does not protect against those, so their registration also moves
  into the fixture's `.bazelrc` configs.

Host-specific settings (cache locations, JDK pins, Windows nested-server startup options) are
generated by `BazelProjectConfigurer` into `user.bazelrc`; fixtures commit a trailing
`try-import %workspace%/user.bazelrc` in `.bazelrc` to pick it up. Project-view files that only
one OS needs are written by the test's own `configureProject` step (see `StrictDepsTest`).

## Adding a test to the matrix

1. Add or update the cross-platform fixture under `plugins/bazel/integrationTests/testProjects/<fixture>` (see the
   contract above), then reference that fixture in the test declaration.
2. Create `e2e/<test-name>/BUILD.bazel` (lowercase kebab-case, conventionally matching the test
   name) with an `ide_starter_e2e_matrix` call — normally `os_list = ["windows"]`, see above — and
   add the new package's OS suites to the aggregating suites in `e2e/BUILD.bazel`.
3. Mirror the OS in CI: add a `WindowsRun`/target entry to the test's `TestDef` in UTC's
   `.teamcity/src/plugins/bazel/buildTypes/BazelPluginIdeStarterTest.kt`.
4. Run the config check:
   `bazel test --nocache_test_results //plugins/bazel/integrationTests:fixture_config_tests`.
5. Run the real cell, e.g.
   `bazel test --nocache_test_results //plugins/bazel/integrationTests/e2e/strict-deps:windows_bazel_9_0_0`
   (or the `:windows` suite for the default version).

## Caveats

- A cell existing here does not schedule it. CI wiring is currently manual and default-only:
  UTC's `TestDef` catalog references each test's per-OS suite (e.g. `:windows`), and that suite
  runs only the `default_bazel_version` cell — non-default version cells are generated but not
  scheduled by anything.
  TODO: rework CI to discover matrix targets automatically (bucketing) so every declared cell
  gets scheduled without a manual UTC entry; until then, declare only the default version.
- Cases that call `BazelProjectConfigurer.addHermeticCcToolchain` git-reset `MODULE.bazel`
  before appending to it; only dynamic-project (hirschgarten/performance) paths do this, and
  they must stay on `ide_starter_e2e_host_test`.
