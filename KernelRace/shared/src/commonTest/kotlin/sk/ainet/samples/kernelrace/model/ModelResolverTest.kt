package sk.ainet.samples.kernelrace.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ModelResolverTest {

    @Test
    fun cacheHitShortCircuitsBeforeCheckingTheAsset() {
        val plan = ModelResolver.plan(targetPath = "/models/x.gguf", targetExists = true, assetAvailable = true)

        assertIs<ResolutionPlan.UseCached>(plan)
        assertEquals("/models/x.gguf", plan.path)
    }

    @Test
    fun assetWinsOverDownloadWhenNotCached() {
        val plan = ModelResolver.plan(targetPath = "/models/x.gguf", targetExists = false, assetAvailable = true)

        assertIs<ResolutionPlan.UseAsset>(plan)
        assertEquals("/models/x.gguf", plan.path)
    }

    @Test
    fun downloadsWhenNeitherCachedNorBundled() {
        val plan = ModelResolver.plan(targetPath = "/models/x.gguf", targetExists = false, assetAvailable = false)

        assertEquals(ResolutionPlan.Download, plan)
    }

    @Test
    fun partPathNeverCollidesWithTheFinalTarget() {
        assertEquals("/models/x.gguf.part", ModelResolver.partPath("/models/x.gguf"))
    }
}
