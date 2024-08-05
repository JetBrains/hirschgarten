# Adding Components from Existing Repositories into Hirschgarten

This guide will walk you through the process of adding new components from existing repositories into a Hirschgarten. 
This approach allows us to maintain the Git history of existing repositories while consolidating them into monorepo structure.

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
4. Local clone of repositories you want to merge
5. Copy the migration script ([merge-component.py](../../tools/infra_scripts/migration/merge-component.py)) to your local machine.

## Using the Script

The basic syntax for using the script is:

```
python merge-component.py <hirschgarten_path> <local_repo_path> <subfolder>
```

Where:
- `<hirschgarten_path>` is the path to Hirschgarten (local clone path or remote URL)
- `<local_repo_path>` is the path to the local repository you want to merge
- `<subfolder>` is the name of the subfolder in the monorepo where the component will be placed

### Step-by-Step Process

1. **Prepare the local repository:**  
   * Ensure the repository you want to merge is cloned locally and up to date.  
   * All the branches checked out locally will also be added as branches to Hirschgarten.
   * If the new component git history is too large (you can check its `.git` folder size), consider removing the largest objects from it. You can google or ask AI assistant for concrete strategies to reduce `.git` size.

2. **Run the script:**  
   Open a terminal, navigate to the directory containing `merge-component.py`, and run the script with the appropriate arguments. For example:

   ```
   python merge-component.py /path/to/hirchgraten/clone /path/to/local/repo component-name
   ```

3. **Review the output:**  
   The script will provide detailed output about its actions. It will:
    - Create a temporary directory for processing
    - List the branches it detects in the local repository
    - Create new branches in the monorepo clone for each branch in the local repository
    - Attempt to merge the monorepo's main branch into each new branch to consolidate history and make it easier to merge things back into main

4. **Verify the results locally:**  
   After the script completes:  
    - Check the monorepo to ensure all expected branches are present
    - Verify that the component's code is correctly placed in the specified subfolder
    - Review the commit history to ensure it's been preserved as expected

5. **Push branches to remote:**  
    - The script will create new branches in the monorepo with names like `<subfolder>/<original-branch-name>`
    - Push the branches you plan to work on to monorepo's remote once they are ready to be merged

7. **Code Review:**
    - When you are ready to merge the new component, request Code Review normally
    - If you want to preserve commit history of the new component, the usual "Squash and rebase" method won't work since it'll remove all the individual commits
    - Therefore, when ready to merge, ask repo admin to go to Hirschgarten's [Space Code settings](https://code.jetbrains.team/p/bazel/repositories/hirschgarten/edit?tab=info) page and:
      * Open "Protected branches -> +:refs/heads/main"
      * Disable "Enforce linear history" toggle
      * Enable "Allow merge commits" checkbox
      * Merge the branch from Code Review page with "Merge" method
      * Return "Enforce linear history" and "Allow merge commits" options to their default values

8. **CI/CD:**
    - Be default, changes to new folder won't trigger CI
    - To check new component against all CI pipeline - run them manually on the main/unmerged branches
    - If you want particular tests from current CI to trigger - make necessary changes to CI trigger rules
    - Review Space Code Quality Gates settings to make sure the new component's folder will/won't require it
    - Add new pipelines/individual tests for this new component in both Hirschgarten's TC configuration and buildserver configuration repo

9. **Documentation:**
    - Update any relevant documentation to reflect the new monorepo structure and how to work with the newly added component

## Troubleshooting

- If the script fails to run, ensure all prerequisites are installed correctly
- If branches are skipped unexpectedly, check that your local clone of the repository has the branches checked-out locally
- For persistent issues, review the script's output for error messages and/or consult with the script's maintainer