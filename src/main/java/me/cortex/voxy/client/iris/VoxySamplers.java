package me.cortex.voxy.client.iris;

import me.cortex.voxy.client.core.IrisVoxyRenderPipeline;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.common.world.WorldEngine;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;

public class VoxySamplers {
    
    /**
     * Gets the pipeline for the current rendering context.
     * For Immersive Portals support, this returns the pipeline for the current dimension being rendered.
     */
    private static IrisVoxyRenderPipeline getCurrentPipeline(IrisVoxyRenderPipelineData pipeData) {
        // First try to use the thread-local current pipeline (set by VoxyRenderSystem before rendering)
        // This is the most reliable method for Immersive Portals support
        IrisVoxyRenderPipeline currentPipeline = IrisVoxyRenderPipeline.getCurrentPipeline();
        if (currentPipeline != null) {
            return currentPipeline;
        }
        
        // Fallback: use the first available pipeline in the map
        if (!pipeData.pipelines.isEmpty()) {
            // If we have multiple pipelines but no current pipeline set, something is wrong
            // Return null to avoid using the wrong pipeline
            if (pipeData.pipelines.size() > 1) {
                me.cortex.voxy.common.Logger.warn("Multiple pipelines exist but no current pipeline set!");
                return null;
            }
            return pipeData.pipelines.values().iterator().next();
        }
        
        // Legacy fallback
        return pipeData.thePipeline;
    }
    
    public static void addSamplers(IrisRenderingPipeline pipeline, SamplerHolder samplers) {
        var patchData = ((IGetVoxyPatchData)pipeline).voxy$getPatchData();
        if (patchData != null) {
            String[] opaqueNames = new String[]{"vxDepthTexOpaque"};
            String[] translucentNames = new String[]{"vxDepthTexTrans"};

            if (IrisShaderPatch.IMPERSONATE_DISTANT_HORIZONS) {
                opaqueNames = new String[]{"vxDepthTexOpaque", "dhDepthTex1"};
                translucentNames = new String[]{"vxDepthTexTrans", "dhDepthTex", "dhDepthTex0"};
            }

            // Dynamic sampler that returns the depth texture for the current dimension being rendered
            samplers.addDynamicSampler(TextureType.TEXTURE_2D, () -> {
                var pipeData = ((IGetIrisVoxyPipelineData)pipeline).voxy$getPipelineData();
                if (pipeData == null) {
                    me.cortex.voxy.common.Logger.info("VoxySamplers: pipeData is null");
                    return 0;
                }
                
                var voxyPipeline = getCurrentPipeline(pipeData);
                if (voxyPipeline == null) {
                    me.cortex.voxy.common.Logger.info("VoxySamplers: voxyPipeline is null, current thread pipeline: " + IrisVoxyRenderPipeline.getCurrentPipeline());
                    return 0;
                }

                //In theory the first frame could be null
                var dt = voxyPipeline.fb.getDepthTex();
                if (dt == null) {
                    me.cortex.voxy.common.Logger.info("VoxySamplers: depth tex is null");
                    return 0;
                }
                me.cortex.voxy.common.Logger.info("VoxySamplers: returning depth tex id: " + dt.id + " for pipeline: " + voxyPipeline);
                return dt.id;
            }, null, opaqueNames);

            samplers.addDynamicSampler(TextureType.TEXTURE_2D, () -> {
                var pipeData = ((IGetIrisVoxyPipelineData)pipeline).voxy$getPipelineData();
                if (pipeData == null) {
                    me.cortex.voxy.common.Logger.info("VoxySamplers translucent: pipeData is null");
                    return 0;
                }
                
                var voxyPipeline = getCurrentPipeline(pipeData);
                if (voxyPipeline == null) {
                    me.cortex.voxy.common.Logger.info("VoxySamplers translucent: voxyPipeline is null, current thread pipeline: " + IrisVoxyRenderPipeline.getCurrentPipeline());
                    return 0;
                }
                
                //In theory the first frame could be null
                var dt = voxyPipeline.fbTranslucent.getDepthTex();
                if (dt == null) {
                    me.cortex.voxy.common.Logger.info("VoxySamplers translucent: depth tex is null");
                    return 0;
                }
                me.cortex.voxy.common.Logger.info("VoxySamplers translucent: returning depth tex id: " + dt.id + " for pipeline: " + voxyPipeline);
                return dt.id;
            }, null, translucentNames);
        }
    }
}
