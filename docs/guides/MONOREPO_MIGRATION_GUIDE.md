# Hirschgarten Migration Guide

This guide will help you migrate your unpublished branches from the old repositories (bazel-bsp, intellij-bsp, or intellij-bazel) to the new Hirschgarten monorepo developed by the JetBrains Bazel team.

## Prerequisites

1. Python 3.6 or higher installed on your system
2. Ensure you have Git installed and configured on your system.
3. Install `git-filter-repo`. You can do this by running:
   - On macOS: `brew install git-filter-repo`
   - On other systems or without Homebrew:
     ```
     pip3 install git-filter-repo
     ```
   Make sure the installation directory is in your PATH.
4. Fork the Hirschgarten repository on GitHub if you haven't already.
5. Ensure you have a fork of your old repository (bazel-bsp, intellij-bsp, or intellij-bazel) on GitHub.
6. Copy the migration script ([migration.py](../../tools/infra_scripts/migration/migration.py)) to your local machine.

## Migration Steps

1. **Prepare Your Repository Information**  
   You have two options for specifying the repositories:

   a) Using Git URLs:
   - Have the URL of your Hirschgarten fork ready. It should look like:
     ```
     https://github.com/YOUR_USERNAME/hirschgarten.git
     ```
   - Have the URL of your old repository fork ready. It should look like:
     ```
     https://github.com/YOUR_USERNAME/bazel-bsp.git
     ```
     (Replace 'bazel-bsp' with 'intellij-bsp' or 'intellij-bazel' as appropriate)

   b) Using Local Repositories:
   - If you prefer, you can clone the repositories locally before running the script:
   - Note the paths to these local repositories.

2. **Identify Your Branch**
   - Identify the name of the unpublished branch in your old repository fork that you want to migrate.

3. **Run the Migration Script**
   - Open a terminal and navigate to the directory containing the `migration.py` script.
   - Run the script using the following format:
     ```
     python migration.py <monorepo> <old_repo> <branch_name>
     ```
   - Replace the placeholders as follows:
     - `<monorepo>`: URL of your Hirschgarten fork OR path to your local Hirschgarten repository
     - `<old_repo>`: URL of your old repository fork OR path to your local old repository
     - `<branch_name>`: Name of the branch you want to migrate

   Examples:
   Using Git URLs:
   ```
   python migration.py https://github.com/YOUR_USERNAME/hirschgarten.git https://github.com/YOUR_USERNAME/bazel-bsp.git my-feature-branch
   ```
   Using local repositories:
   ```
   python migration.py ./hirschgarten ./bazel-bsp my-feature-branch
   ```

4. **Review the Results**
   - The script will create a new branch in your Hirschgarten fork with your migrated changes.
   - The new branch will be named `<subdirectory>/<your-branch-name>`, where:
     - For bazel-bsp: subdirectory is "server"
     - For intellij-bsp: subdirectory is "plugin-bsp"
     - For intellij-bazel: subdirectory is "plugin-bazel"

5. **Push Your Migrated Branch**
   - If you used Git URLs, the script will create a local copy of your Hirschgarten fork with the new branch.
   - Navigate to the Hirschgarten directory and push the new branch:
     ```
     cd hirschgarten
     git push origin <subdirectory>/<your-branch-name>
     ```

6. **Create a Pull Request**
   - Go to the main Hirschgarten repository on GitHub (https://github.com/JetBrains/hirschgarten).
   - Click on "Pull requests" and then "New pull request".
   - Click on "compare across forks" and select your fork and the newly pushed branch.
   - Review the changes and create the pull request with a descriptive title and description.

## Troubleshooting

- If you encounter any errors during the migration process, carefully read the error message provided by the script.
- Ensure you have the necessary permissions to access both your forks.
- For any persistent issues, reach out to the JetBrains Bazel team for support.

## Notes

- This script preserves the history of your branch while moving it into the appropriate subdirectory in the Hirschgarten monorepo.
- After successful migration and creating the pull request, you can safely delete any temporary local directories created during the migration process.
- Always ensure you have a backup of your work before running migration scripts.

By following this guide, you should be able to successfully migrate your unpublished branches from your old repository forks to the new Hirschgarten monorepo structure and create a pull request for integration. If you have multiple branches to migrate, repeat the process for each branch.
