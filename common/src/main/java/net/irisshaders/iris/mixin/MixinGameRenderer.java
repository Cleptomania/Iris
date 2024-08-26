package net.irisshaders.iris.mixin;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gl.program.IrisProgramTypes;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
	@Shadow
	private boolean renderHand;

	@Inject(method = "getPositionShader", at = @At("HEAD"), cancellable = true)
	private static void iris$overridePositionShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (isSky()) {
			override(ShaderKey.SKY_BASIC, cir);
		} else if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_BASIC, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.BASIC, cir);
		}
	}

	@Inject(method = "getPositionColorShader", at = @At("HEAD"), cancellable = true)
	private static void iris$overridePositionColorShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (isSky()) {
			override(ShaderKey.SKY_BASIC_COLOR, cir);
		} else if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_BASIC_COLOR, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.BASIC_COLOR, cir);
		}
	}

	@Inject(method = "getPositionTexShader", at = @At("HEAD"), cancellable = true)
	private static void iris$overridePositionTexShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (isSky()) {
			override(ShaderKey.SKY_TEXTURED, cir);
		} else if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_TEX, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.TEXTURED, cir);
		}
	}

	@Inject(method = {"getPositionTexColorShader"}, at = @At("HEAD"), cancellable = true)
	private static void iris$overridePositionTexColorShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (isSky()) {
			override(ShaderKey.SKY_TEXTURED_COLOR, cir);
		} else if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_TEX_COLOR, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.TEXTURED_COLOR, cir);
		}
	}

	//TODO: check cloud phase

	@Inject(method = {
		"getParticleShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideParticleShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (isPhase(WorldRenderingPhase.RAIN_SNOW)) {
			override(ShaderKey.WEATHER, cir);
		} else if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_PARTICLES, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.PARTICLES, cir);
		}
	}

	@Inject(method = "getRendertypeCloudsShader", at = @At("HEAD"), cancellable = true)
	private static void iris$overridePositionTexColorNormalShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_CLOUDS, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.CLOUDS, cir);
		}
	}

	@Inject(method = "getRendertypeSolidShader", at = @At("HEAD"), cancellable = true)
	private static void iris$overrideSolidShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			// TODO: Wrong program
			override(ShaderKey.SHADOW_TERRAIN_CUTOUT, cir);
		} else if (isBlockEntities() || isEntities()) {
			override(ShaderKey.MOVING_BLOCK, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.TERRAIN_SOLID, cir);
		}
	}

	@Inject(method = {
		"getRendertypeCutoutShader",
		"getRendertypeCutoutMippedShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideCutoutShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_TERRAIN_CUTOUT, cir);
		} else if (isBlockEntities() || isEntities()) {
			override(ShaderKey.MOVING_BLOCK, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.TERRAIN_CUTOUT, cir);
		}
	}

	@Inject(method = {
		"getRendertypeTranslucentShader",
		"getRendertypeTranslucentMovingBlockShader",
		"getRendertypeTripwireShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideTranslucentShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_TRANSLUCENT, cir);
		} else if (isBlockEntities() || isEntities()) {
			override(ShaderKey.MOVING_BLOCK, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.TERRAIN_TRANSLUCENT, cir);
		}
	}

	// TODO: getPositionColorLightmapShader

	@Inject(method = {
		"getRendertypeEntityCutoutShader",
		"getRendertypeEntityCutoutNoCullShader",
		"getRendertypeEntityCutoutNoCullZOffsetShader",
		"getRendertypeEntityDecalShader",
		"getRendertypeEntitySmoothCutoutShader",
		"getRendertypeArmorCutoutNoCullShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideEntityCutoutShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			if (isBlockEntities()) {
				override(ShaderKey.SHADOW_BLOCK, cir);
				return;
			}
			override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
		} else if (HandRenderer.INSTANCE.isActive()) {
			override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.BLOCK_ENTITY_DIFFUSE, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.ENTITIES_CUTOUT_DIFFUSE, cir);
		}
	}

	// TODO: getPositionTexLightmapColorShader

	@Inject(method = {
		"getRendertypeEntityTranslucentShader",
		"getRendertypeEntityTranslucentCullShader",
		"getRendertypeItemEntityTranslucentCullShader",
		"getRendertypeBreezeWindShader",
		"getRendertypeEntityNoOutlineShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideEntityTranslucentShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			if (isBlockEntities()) {
				override(ShaderKey.SHADOW_BLOCK, cir);
				return;
			}
			override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
		} else if (HandRenderer.INSTANCE.isActive()) {
			override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.BE_TRANSLUCENT, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.ENTITIES_TRANSLUCENT, cir);
		}
	}

	@Inject(method = {
		"getRendertypeEnergySwirlShader",
		"getRendertypeEntityShadowShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideEnergySwirlShadowShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
		} else if (HandRenderer.INSTANCE.isActive()) {
			override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT : ShaderKey.HAND_TRANSLUCENT, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.BLOCK_ENTITY, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.ENTITIES_CUTOUT, cir);
		}
	}

	@Inject(method = {
		"getRendertypeGlintShader",
		"getRendertypeGlintDirectShader",
		"getRendertypeGlintTranslucentShader",
		"getRendertypeArmorGlintShader",
		"getRendertypeEntityGlintDirectShader",
		"getRendertypeEntityGlintShader",
		"getRendertypeArmorEntityGlintShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideGlintShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (shouldOverrideShaders()) {
			override(ShaderKey.GLINT, cir);
		}
	}

	@Inject(method = {
		"getRendertypeEntitySolidShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideEntitySolidDiffuseShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			if (isBlockEntities()) {
				override(ShaderKey.SHADOW_BLOCK, cir);
				return;
			}
			override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
		} else if (HandRenderer.INSTANCE.isActive()) {
			override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.BLOCK_ENTITY_DIFFUSE, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.ENTITIES_SOLID_DIFFUSE, cir);
		}
	}

	@Inject(method = {
		"getRendertypeWaterMaskShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideEntitySolidShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
		} else if (HandRenderer.INSTANCE.isActive()) {
			override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT : ShaderKey.HAND_TRANSLUCENT, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.BLOCK_ENTITY, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.ENTITIES_SOLID, cir);
		}
	}

	@Inject(method = "getRendertypeBeaconBeamShader", at = @At("HEAD"), cancellable = true)
	private static void iris$overrideBeaconBeamShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_BEACON_BEAM, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.BEACON, cir);
		}
	}

	@Inject(method = "getRendertypeEntityAlphaShader", at = @At("HEAD"), cancellable = true)
	private static void iris$overrideEntityAlphaShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (!ShadowRenderer.ACTIVE) {
			override(ShaderKey.ENTITIES_ALPHA, cir);
		}
	}

	@Inject(method = {
		"getRendertypeEyesShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideEntityEyesShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.BLOCK_ENTITY, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.ENTITIES_EYES, cir);
		}
	}

	@Inject(method = {
		"getRendertypeEntityTranslucentEmissiveShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideEntityTranslucentEmissiveShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			if (isBlockEntities()) {
				override(ShaderKey.SHADOW_BLOCK, cir);
				return;
			}
			override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.BLOCK_ENTITY, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.ENTITIES_EYES_TRANS, cir);
		}
	}

	@Inject(method = {
		"getRendertypeLeashShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideLeashShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_LEASH, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.LEASH, cir);
		}
	}

	@Inject(method = {
		"getRendertypeLightningShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideLightningShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_LIGHTNING, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.LIGHTNING, cir);
		}
	}
	// NOTE: getRenderTypeOutlineShader should not be overriden.

	@Inject(method = {
		"getRendertypeCrumblingShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideCrumblingShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (shouldOverrideShaders() && !ShadowRenderer.ACTIVE) {
			override(ShaderKey.CRUMBLING, cir);
		}
	}

	@Inject(method = {
		"getRendertypeTextShader",
		"getRendertypeTextSeeThroughShader",
		"getPositionColorTexLightmapShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideTextShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_TEXT, cir);
		} else if (HandRenderer.INSTANCE.isActive()) {
			override(ShaderKey.HAND_TEXT, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.TEXT_BE, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.TEXT, cir);
		}
	}

	@Inject(method = {
		"getRendertypeTextBackgroundShader",
		"getRendertypeTextBackgroundSeeThroughShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideTextBackgroundShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_TEXT_BG, cir);
		} else {
			override(ShaderKey.TEXT_BG, cir);
		}
	}

	@Inject(method = {
		"getRendertypeTextIntensityShader",
		"getRendertypeTextIntensitySeeThroughShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideTextIntensityShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_TEXT_INTENSITY, cir);
		} else if (HandRenderer.INSTANCE.isActive()) {
			override(ShaderKey.HAND_TEXT_INTENSITY, cir);
		} else if (isBlockEntities()) {
			override(ShaderKey.TEXT_INTENSITY_BE, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.TEXT_INTENSITY, cir);
		}
	}

	@Inject(method = {
		"getRendertypeLinesShader"
	}, at = @At("HEAD"), cancellable = true)
	private static void iris$overrideLinesShader(CallbackInfoReturnable<ShaderInstance> cir) {
		if (ShadowRenderer.ACTIVE) {
			override(ShaderKey.SHADOW_LINES, cir);
		} else if (shouldOverrideShaders()) {
			override(ShaderKey.LINES, cir);
		}
	}

	@Unique
	private static boolean isBlockEntities() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		return pipeline != null && pipeline.getPhase() == WorldRenderingPhase.BLOCK_ENTITIES;
	}

	@Unique
	private static boolean isEntities() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		return pipeline != null && pipeline.getPhase() == WorldRenderingPhase.ENTITIES;
	}

	@Unique
	private static boolean isSky() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null) {
			return switch (pipeline.getPhase()) {
				case CUSTOM_SKY, SKY, SUNSET, SUN, STARS, VOID, MOON -> true;
				default -> false;
			};
		} else {
			return false;
		}
	}

	@Unique
	private static boolean isPhase(WorldRenderingPhase phase) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline != null) {
			return pipeline.getPhase() == phase;
		} else {
			return false;
		}
	}

	// ignored: getRendertypeEndGatewayShader (we replace the end portal rendering for shaders)
	// ignored: getRendertypeEndPortalShader (we replace the end portal rendering for shaders)

	@Unique
	private static boolean shouldOverrideShaders() {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline instanceof ShaderRenderingPipeline) {
			return ((ShaderRenderingPipeline) pipeline).shouldOverrideShaders();
		} else {
			return false;
		}
	}

	@Unique
	private static void override(ShaderKey key, CallbackInfoReturnable<ShaderInstance> cir) {
		WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();

		if (pipeline instanceof ShaderRenderingPipeline) {
			ShaderInstance override = ((ShaderRenderingPipeline) pipeline).getShaderMap().getShader(key);

			if (override != null) {
				cir.setReturnValue(override);
			}
		}
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void iris$startFrame(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
		// This allows certain functions like float smoothing to function outside a world.
		CapturedRenderingState.INSTANCE.setRealTickDelta(deltaTracker.getGameTimeDeltaPartialTick(true));
		SystemTimeUniforms.COUNTER.beginFrame();
		SystemTimeUniforms.TIMER.beginFrame(Util.getNanos());
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void iris$logSystem(Minecraft arg, ItemInHandRenderer arg2, ResourceManager arg3, RenderBuffers arg4, CallbackInfo ci) {
		Iris.logger.info("Hardware information:");
		Iris.logger.info("CPU: " + GlUtil.getCpuInfo());
		Iris.logger.info("GPU: " + GlUtil.getRenderer() + " (Supports OpenGL " + GlUtil.getOpenGLVersion() + ")");
		Iris.logger.info("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")");
	}

	@Redirect(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
	private void iris$disableVanillaHandRendering(ItemInHandRenderer itemInHandRenderer, float tickDelta, PoseStack poseStack, BufferSource bufferSource, LocalPlayer localPlayer, int light) {
		if (Iris.isPackInUseQuick()) {
			return;
		}

		itemInHandRenderer.renderHandsWithItems(tickDelta, poseStack, bufferSource, localPlayer, light);
	}

	@Inject(method = "renderLevel", at = @At("TAIL"))
	private void iris$runColorSpace(DeltaTracker deltaTracker, CallbackInfo ci) {
		Iris.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::finalizeGameRendering);
	}

	@Redirect(method = "reloadShaders", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false))
	private ArrayList<Program> iris$reloadGeometryShaders() {
		ArrayList<Program> programs = Lists.newArrayList();
		programs.addAll(IrisProgramTypes.GEOMETRY.getPrograms().values());
		programs.addAll(IrisProgramTypes.TESS_CONTROL.getPrograms().values());
		programs.addAll(IrisProgramTypes.TESS_EVAL.getPrograms().values());
		return programs;
	}
}
