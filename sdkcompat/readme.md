When adding a new file to sdkcompat (or changing an existing one to accomodate for new changes),
add a comment specifying what the change is and the sdk version it was introduced in.

Example:
```
// v243: fun isApplicable() is changed to fun isApplicable(Project)
```

This way we know that if we drop support for sdk v242, we can remove the compatibility code introduced in v243.
