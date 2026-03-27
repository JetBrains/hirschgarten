# Contributing to Hirschgarten

We're excited that you're interested in contributing to Hirschgarten!  
This document outlines the process for contributing to this project.

## Code of Conduct

By participating in this project, you are expected to uphold our [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

1. Set up your development environment, as described in [in this document](docs/dev/development_setup.md).
2. **Follow Coding Standards**:
  - write new files in Kotlin

3. **Commit Your Changes**: Make well-formatted commit messages.

4. **Open a Pull Request**: Submit a pull request from your fork to the main Hirschgarten repository.

## Pull Request Process

1. Update the README.md with details of changes if applicable.
2. Your pull request will be reviewed by maintainers, who may request changes or provide feedback.
3. Once you submit your pull request, our CI pipeline will automatically run on your code.
4. If the CI pipeline reports any issues, you'll need to address them. This may involve fixing failed tests, addressing code style issues, or resolving other automated checks.
5. Once all CI checks pass and the code has been approved by maintainers, your pull request will be merged.

## Continuous Integration

Upon submitting a pull request, our CI pipeline will automatically run a series of checks on your code. These checks may include:

- Running all tests
- Checking code style and formatting
- Static code analysis
- Building the project

If any of these checks fail, you'll see details in the pull request.  
You'll need to address these issues and push new commits to your branch.  
The CI pipeline will automatically re-run on new commits.  
If some test passes locally but fails on CI and you don't understand the reason -
ask maintainers for help in the PR comments.

## Reporting Bugs

We use YouTrack for bug tracking. Please report bugs at our [YouTrack project](https://youtrack.jetbrains.com/issues/BAZEL).  
Before creating a new issue, please check existing issues to avoid duplicates.

## Suggesting Enhancements

We welcome suggestions for improvements and new features.  
Please use our  [YouTrack project](https://youtrack.jetbrains.com/issues/BAZEL) to submit your ideas, clearly labeling them as feature requests/suggestions.

## Getting Help
If you need help with your contribution or have questions about the CI process, feel free to ask questions in comments to your PR.

Thank you for contributing to Hirschgarten!