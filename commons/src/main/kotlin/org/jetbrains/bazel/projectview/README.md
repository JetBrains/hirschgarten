# Project View

The project view file (*.bazelproject) is used to import a subset of bazel targets into the IDE, configure a project,
and specify how the bsp server will be started.

This is our adaptation of the project view mechanism known
from [Bazel Plugin for Intellij](https://ij.bazel.build/docs/project-views.html)

> The project view file uses a python-like format with 2 spaces indentation and # comments. You can share the
> *.bazelproject file between projects, use your own copy, or both.
>
> In general, you can start with just ~~directories and~~ targets and add more sections as you want to further tweak
> your IDE workspace.

## Usage

**Note:** We will be changing this mechanism in future releases.

`$ cs launch org.jetbrains.bsp:bazel-bsp:<version> -M org.jetbrains.bazel.install.Install -- -p <path/to/projectview_file>`.
For more details, see `--help`.

## Available sections

---

#### import

Imports another project view.

You may use multiple imports in any project view. Any list type sections (e.g. `targets`) compose. Single-value
sections (e.g. `bazel_binary`) override and use the last one encountered, depth-first parse order (i.e. imports are
evaluated as they are encountered).

##### example:

```
import path/to/another/projectview.bazelproject
```

---

#### try_import

Try importing another project view.

Similar to `import` but no errors will be thrown if the declared project view file does not exist.

##### example:

```
try_import path/to/another/projectview.bazelproject
```

---

#### targets

A list of bazel target expressions, they support `/...` notation.

Targets are built during the server lifetime, so the more targets you have, the slower your IDE experience might be. You
can use negative targets to have server ignore certain targets (
e.g. `-//executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/parser/...`).

##### example:

```
targets:
  //install/src/main/kotlin/org/jetbrains/bazel/install
  //executioncontext/projectview/...
  -//executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/parser/...
```

##### default:

No target is included:

```
targets:
```

---

#### bazel_binary

Path to bazel which will be used to invoke bazel from the server (e.g. to build a project, or query bazel).

##### example:

```
bazel_binary: /usr/local/bin/bazel
```

##### default:

The server will deduct bazel path from `$PATH`

---

#### directories

A list of directories to be mapped into bazel targets.

You can use negative directories to have server ignore certain directories (
e.g. `-executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/parser/...`).

##### example:

```
directories:
  install/src/main/kotlin/org/jetbrains/bazel/install
  executioncontext/projectview/
  -executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/parser
```

##### default:

No directories included.

---

#### derive_targets_from_directories

A flag specifying if targets should be derived from list of directories in directories section.

Flag is boolean value, so it can take either true or false. In the first case targets will be derived from directories,
in the second they won't.

##### example:

```
derive_targets_from_directories: true
```

##### default:

Targets will not be derived from directories.

---

#### import_depth

A numerical value that specifies how many levels of bazel targets dependencies should be imported as modules.
Only the targets that are present in workspace are imported.

You can use negative value to import all transitive dependencies.

##### example:

```
import_depth: 1
```

##### default:

The default value is -1, meaning that all transitive dependencies will be imported.

---

#### shard_sync

enable shard sync, split and build targets in batches to avoid Bazel OOM.

##### default

default to `false` due to potential [memory leak issue](https://github.com/bazelbuild/bazel/issues/19412) with Bazel.

---

#### target_shard_size

Used alongside with `shard_sync`. It decides the number of targets to be built in each shard.

##### default

default to `1000`

---

#### shard_approach

Used alongside with `shard_sync`. It decides the sharding strategy used to shard the list of original targets.

There are three options to use:

- `EXPAND_AND_SHARD` : expand wildcard targets to package targets, query single targets, and then shard to batches
- `QUERY_AND_SHARD` : query single targets from the given list of targets without expanding, and then shard to batches
- `SHARD_ONLY` : split unexpanded wildcard targets into batches

##### default

default to `QUERY_AND_SHARD`

---

#### exclude_library

_We are working on it, you can expect support for this section in future releases._

---

#### build_flags

A set of bazel flags added to **all** bazel command invocations.

##### example:

```
build_flags:
  --define=ij_product=intellij-latest
```

##### default:

No flags.

---

#### sync_flags

A set of bazel flags added only to bazel calls during sync.

##### example:

```
sync_flags:
  --define=ij_product=intellij-latest
```

##### default:

No flags.

---

#### test_flags

_We are working on it, you can expect support for this section in future releases._

---

#### import_run_configurations

A list of XML files which will be imported as run configurations during Bazel sync.

Run configurations from Google's Bazel plugin are converted to the new format automatically upon import.

Run configurations can be exported to XML by checking the "Store as project file" checkbox in the run configuration settings UI.

##### example:

```
import_run_configurations:
  tools/intellij/run_application.xml
  tools/intellij/run_tests.xml
```
---

#### allow_manual_targets_sync

A flag specifying if targets with `manual` tag should be built.
Flag is boolean value, so it can take either true or false. In the first case targets with `manual` tag will be build,
otherwise they will not.

##### example:

allow_manual_targets_sync: true

##### default:

allow_manual_targets_sync: false

#### gazelle_target

_IntelliJ and GoLand only_

Points to the gazelle target to be used by the plugin during a sync. The plugin will run this target on the contents of directories at the beginning of the sync operation.

#### index_all_files_in_directories

- When `true`, all files inside directories (as specified in the `directories` project view section) will be indexed.
- When `false`, only files belonging to a Bazel target will be indexed (to speed up indexing).

##### example:

```
index_all_files_in_directories: true
```

##### default:

```
index_all_files_in_directories: false
```
