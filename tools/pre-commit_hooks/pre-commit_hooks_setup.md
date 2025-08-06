# Git pre-commit hooks for Bazel plugin formatting

Ready-to-use Git pre-commit hooks that help keep code under `plugins/bazel/` properly formatted when committing changes.

You can choose between:
- pre-commit_format — auto-formats changed files before commit.
- pre-commit_check_format — only checks formatting and fails the commit if formatting is required.

Both hooks only run when your staged changes include files under `plugins/bazel/`.

## Quick setup (choose one of the two)
1) Decide which behavior you want locally:
   - Auto-fix on commit: use `pre-commit_format`
   - Enforce/verify only: use `pre-commit_check_format`

2) Copy the chosen script into your local Git hooks as `.git/hooks/pre-commit` at the repository root:
   - From repository root:
     - Auto-fix variant:
       - `cp plugins/bazel/tools/pre-commit_hooks/pre-commit_format .git/hooks/pre-commit`
     - Check-only variant:
       - `cp plugins/bazel/tools/pre-commit_hooks/pre-commit_check_format .git/hooks/pre-commit`

3) Make it executable:
   - chmod +x .git/hooks/pre-commit

That’s it. The hook will now run automatically on every `git commit` if staged files live under `plugins/bazel/`.

## What exactly the hooks do
Both scripts contain this guard:
- `if git diff --cached --name-only | grep -q '^plugins/bazel/'` — the hook only runs when you stage changes under `plugins/bazel/`.

Then they differ in what they execute:
- pre-commit_format:
  - Prints: `formating plugins/bazel`
  - Runs: `( cd plugins/bazel && bazel run :format )`
  - Auto-formats files. If the formatter rewrites files, it restages them: `git add $(git ls-files -m | grep '^plugins/bazel/')`
  - Fails the commit only if Bazel itself returns an error.

- pre-commit_check_format:
  - Prints: `checking formating plugins/bazel`
  - Runs: `( cd plugins/bazel && bazel run //tools/format:format.check )`
  - Verifies formatting without changing files. If formatting is not compliant, the Bazel target is expected to exit with a non-zero code and the commit will fail.
  - Also includes the same restaging line for modified files; however, in check mode no changes should be produced. This is mainly a no-op unless the check target modifies files (which it shouldn’t).

## Troubleshooting
- Hook does not run: ensure the file is at `.git/hooks/pre-commit` (not committed to the repo by default), and is executable (`chmod +x .git/hooks/pre-commit`).
- Commit fails due to formatting: either let the auto-format hook fix it, or run `bazel run :format` manually and restage files.
- Uninstall the hook: simply remove `.git/hooks/pre-commit` or replace it with another hook.