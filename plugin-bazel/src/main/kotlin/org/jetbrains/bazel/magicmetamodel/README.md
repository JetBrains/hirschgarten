# MagicMetaModel

The main task of Magic Meta Model is to build an abstract model based on the build targets.

---

The implementation included in the `impl/` directory integrates the model obtained from the BSP with the IntelliJ
Workspace Model, allowing to operate on shared sources.

## Example flow

1. Feed the model with another model - `impl/` implementation requires `WorkspaceModel` and BSP model.

```
MagicMetaModel.create(..)
```

2. Ask to load default targets - after project import user should be able to work with the project without any
   additional steps. So, if project doesn't have any shared sources, why not to load the entire provided project. On the
   other hand, if it contains such sources, why not try to load the targets in such way that targets don't share
   sources.

```
magicMetaModel.loadDefaultTargets()
```

3. Show user loaded and not loaded (e.g. at the sidebar)

```
magicMetaModel.getAllLoadedTargets()
magicMetaModel.getAllNotLoadedTargets()
```

4. Show user currently loaded target and not loaded targets for current text document (e.g. in the widget at the bottom)
   .

```
magicMetaModel.getTargetsDetailsForDocument(documentId)
```

5. Let user switch target for current document and all others included in new target. And, at the same time, unload
   targets that share sources with the currently loaded target.

```
magicMetaModel.loadTarget(targetId)
```

6. Steps 4. and 5. will likely be repeated multiple times by the user.
