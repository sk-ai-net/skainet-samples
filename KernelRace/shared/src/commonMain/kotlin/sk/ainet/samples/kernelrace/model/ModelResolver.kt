package sk.ainet.samples.kernelrace.model

/** What to do to get the GGUF onto local storage, decided without touching any IO. */
sealed interface ResolutionPlan {
    data class UseCached(val path: String) : ResolutionPlan
    data class UseAsset(val path: String) : ResolutionPlan
    data object Download : ResolutionPlan
}

/**
 * Pure resolution policy, ported from the original Android-only `ModelSource.resolve`:
 * cache hit short-circuits, otherwise a bundled offline asset wins over downloading.
 * Takes plain booleans/paths so it needs no filesystem access — the file-based platform
 * providers ([AndroidModelProvider], [DesktopModelProvider]) do the actual IO and defer the
 * decision here.
 */
object ModelResolver {
    fun plan(targetPath: String, targetExists: Boolean, assetAvailable: Boolean): ResolutionPlan = when {
        targetExists -> ResolutionPlan.UseCached(targetPath)
        assetAvailable -> ResolutionPlan.UseAsset(targetPath)
        else -> ResolutionPlan.Download
    }

    /** Streamed downloads land here first; an interrupted download leaves this behind,
     *  never a truncated [targetPath], so a retry doesn't mistake it for a finished file. */
    fun partPath(targetPath: String): String = "$targetPath.part"
}
