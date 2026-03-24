[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Build plugin with Bazel](https://github.com/JetBrains/hirschgarten/actions/workflows/bazel.yml/badge.svg?branch=253)](https://github.com/JetBrains/hirschgarten/actions/workflows/bazel.yml?query=branch%3A253)
# Hirschgarten Project

The Hirschgarten project aims to bring a new approach for Bazel support to IntelliJ IDEA users.

The primary product is the **[Bazel plugin](https://plugins.jetbrains.com/plugin/22977)**, 
a new Bazel plugin for IntelliJ IDEA developed by JetBrains. and published to the JetBrains marketplace.

## Features

Work with Bazel in IntelliJ IDEA:

- import Bazel projects with multiple languages
- run and debug targets and tests through Bazel or directly from the IDE
- Starlark highlighting, navigation, and debugging
- configure project subsets for import

See also:
[Overview of the new Bazel plugin features](https://jb.gg/new-bazel-feature)

## Contributing

We welcome contributions to Hirschgarten!  
Please refer to our [Contribution Guidelines](CONTRIBUTING.md) for more information on how to get involved.

For bug reports and feature requests, please use our [YouTrack project](https://youtrack.jetbrains.com/issues/BAZEL).

Tasks which are rather small and do not require a lot of context can be found in the [Contributions Welcome](https://youtrack.jetbrains.com/issues?q=tag:%20%7Bcontributions-welcome%7D%20project:Bazel%20) list.

## Development

For developers interested in contributing to or working on Hirschgarten:

- [Guide to start developing the plugin](docs/dev/development_setup.md)
- Each component has its own README file in corresponding folders
- [Guide for adding new components from existing repositories](docs/dev/add_components.md)

## Installation

For information on installing and using the Bazel plugin for IntelliJ IDEA, please refer to the [plugin page](https://lp.jetbrains.com/new-bazel-plugin/).

## License

Hirschgarten is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.

---

For more detailed information about each component, please refer to their respective directories within this repository.
