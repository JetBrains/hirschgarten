# TargetUtils Storage Optimization (Nov 2024)

## Problem
The `db.save()` operation was causing IntelliJ to freeze, particularly in remote desktop scenarios. The database file was reaching 200MB+ in size, and saves were taking significant time.

## Root Causes Identified

1. **Inefficient JSON Serialization** (MAJOR)
   - All `BuildTargetData` objects (JvmBuildTarget, KotlinBuildTarget, etc.) were serialized using Gson (JSON)
   - JSON is a text format with ~5-10x overhead compared to binary formats
   - Field names, quotes, brackets, and escaping all add significant bloat
   
2. **No Save Throttling**
   - Every sync would trigger an immediate save
   - No protection against rapid successive saves
   
3. **Blocking Save Operation**
   - Save was synchronous within the IO coroutine
   - 200MB write blocks the IO thread pool

## Optimizations Applied

### 1. Binary Serialization (5-10x size reduction)
- Created `BinaryBuildTargetDataSerializer.kt` with custom binary serializers for all BuildTargetData types
- Replaced JSON serialization in `Serialization.kt`
- Uses compact variable-length integer encoding
- No field name overhead
- Expected reduction: **200MB → 20-40MB**

### 2. Save Throttling (prevents UI freezes)
- Added throttling to limit saves to once per minute
- Cancels pending saves when new data arrives
- Prevents redundant writes during rapid sync operations

### 3. Path Optimization
- Reuses existing path compression from `writePath()`
- Relative paths for files under project root
- Reduces storage for file paths significantly

## Expected Improvements

- **File size**: 80-90% reduction (200MB → 20-40MB)
- **Save time**: 80-90% faster due to less data + no JSON overhead
- **UI freezes**: Eliminated or greatly reduced due to throttling and faster saves
- **Disk I/O**: Much less data written to disk

## Compatibility Notes

- Database version remains the same (`bazel-targets-v2.db` / `bazel-targets-v2-243.db`)
- Old databases will be automatically migrated when first opened
- Binary format is more efficient but still portable across platforms

## Future Improvements (if needed)

1. **Incremental saves**: Only write changed targets instead of full reset
2. **Compression**: MVStore supports compression, but this adds CPU overhead during write
3. **Split databases**: Separate hot data (frequently changing) from cold data (stable)
4. **String interning**: Deduplicate repeated strings (tags, paths) across targets

## Related Issues

- https://youtrack.jetbrains.com/issue/BAZEL-2058/Optimize-storage-of-imported-target-info

