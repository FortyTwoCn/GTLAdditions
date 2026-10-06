package com.gtladd.gtladditions.migration;

import com.gregtechceu.gtceu.client.bloom.BloomRenderer;
import com.gregtechceu.gtceu.client.bloom.BloomShaderManager;
import com.gregtechceu.gtceu.core.mixins.client.bloom.BufferBuilderAccessor;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import net.minecraft.core.SectionPos;

/** Native empty meshes and stale builders, followed by the actual working elevator chunk renderer. */
final class NativeProductionBloomProbe {
    static void verify() {
        if (!BloomShaderManager.isBloomActive())
            throw new AssertionError("Native bloom must remain enabled during lifecycle validation");
        var section = SectionPos.of(-100_000, 6, -100_000);
        var first = BloomRenderer.SafeMode.getOrStartBloomBuffer(section);
        BloomRenderer.SafeMode.bakeBloomChunkBuffers(section, VertexSorting.ORTHOGRAPHIC_Z);
        if (BloomRenderer.SafeMode.BLOOM_BUFFER_BUILDERS.containsKey(section)
                || BloomRenderer.SafeMode.BLOOM_BYTE_BUFFERS.containsKey(section))
            throw new AssertionError("Native empty bloom mesh retained its ended buffer");
        var second = BloomRenderer.SafeMode.getOrStartBloomBuffer(section);
        if (second == first || second.build() != null)
            throw new AssertionError("Native empty bloom rebuild did not start a fresh builder");
        var third = BloomRenderer.SafeMode.getOrStartBloomBuffer(section);
        if (third == second || !((BufferBuilderAccessor) third).isBuilding())
            throw new AssertionError("Native bloom reused a finished cached builder");
        BloomRenderer.SafeMode.bakeBloomChunkBuffers(section, VertexSorting.ORTHOGRAPHIC_Z);
        if (BloomRenderer.SafeMode.BLOOM_BUFFER_BUILDERS.containsKey(section)
                || BloomRenderer.SafeMode.BLOOM_BYTE_BUFFERS.containsKey(section))
            throw new AssertionError("Native bloom lifecycle regression left native allocations cached");
        LogUtils.getLogger().info("GTL_NATIVE_PRODUCTION_BLOOM=PASS enabled=true native_empty_mesh_retired=true finished_cache_replaced=true native_allocations_released=true reload={}",
                NativeProductionDedicatedState.reload());
    }
}
