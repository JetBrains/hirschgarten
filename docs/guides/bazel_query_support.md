# Bazel Query Support

Feature flag: `bazel.query.tab.enabled`

## Overview:
- syntax and error highlighting
- completion for functions, operators and targets defined in a project
- quick documentation for functions and flags
- quick query flags selection

## Layout:
To open the query editor, use the Bazel Query button in the main toolbar of the Bazel plugin toolwindow.

<img alt="tab layout" width="700" src="bazel_query_support/opening.png"/>


Bazel Query toolwindow appears at the bottom of the IntelliJ IDEA window.

<img alt="tab layout" width="700" src="bazel_query_support/toolwindow.png"/>

## Features:

### Query text field

Syntax highlighting 

<img alt="tab layout" width="700" src="bazel_query_support/highlighting.png"/>

Error checking

<img alt="tab layout" width="700" src="bazel_query_support/query-textfield-error1.png"/>

<img alt="tab layout" width="700" src="bazel_query_support/query-textfield-error2.png"/>

Completion for functions, operators and targets defined in a project

<img alt="tab layout" width="700" src="bazel_query_support/completion-functions.png"/>

<img alt="tab layout" width="700" src="bazel_query_support/completion-targets.png"/>

Quick documentation for functions

<img alt="tab layout" width="700" src="bazel_query_support/query-textfield-doc.png"/>

### Flags panel
Selecting query flags from the list

<img alt="tab layout" width="700" src="bazel_query_support/flags.png"/>

Quick documentation for query flags in the list

<img alt="tab layout" width="700" src="bazel_query_support/flag-doc.png"/>

Text field for additional query flags, also with syntax highlighting, error checking and quick documentation

<img alt="tab layout" width="700" src="bazel_query_support/flag-textfield-highlighting.png"/>

<img alt="tab layout" width="700" src="bazel_query_support/flag-textfield-doc.png"/>

### Output panel
Result of the query execution with links to source files or build files where the given target is located.

<img alt="tab layout" width="700" src="bazel_query_support/output.png"/>

If `output` flag is set to `graph`, graph visualisation is generated with Graphviz (`dot` command) and displayed in default tool for .svg files.

<img alt="tab layout" width="700" src="bazel_query_support/output-graph.png"/>

<img alt="tab layout" width="300" src="bazel_query_support/graph.png"/>

### Keyboard navigation
Keyboard navigation is supported in the query text field and flags panel.
Enter key executes the query.
Completion suggestions are selected with:
  - Enter - insets current suggestion,
  - Tab - inserts a fragment of the target suggestion to the slash occurrence (corresponding to next subpackage on the path), for example, for the entered prefix "//p", selecting the suggestion "//path/to/my:target" using 'tab' will result in "//path/".