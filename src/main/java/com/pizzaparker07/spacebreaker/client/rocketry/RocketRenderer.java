package com.pizzaparker07.spacebreaker.client.rocketry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pizzaparker07.spacebreaker.Spacebreaker;
import com.pizzaparker07.spacebreaker.content.rocketry.design.RocketGeometry;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.FlightState;
import com.pizzaparker07.spacebreaker.content.rocketry.entity.RocketEntity;
import com.pizzaparker07.spacebreaker.content.rocketry.nav.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

public class RocketRenderer extends EntityRenderer<RocketEntity> {
    public static final ModelResourceLocation CANOPY = ModelResourceLocation.standalone(Spacebreaker.id("item/parachute_canopy"));

    public RocketRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.9F;
    }

    @Override
    public void render(RocketEntity rocket, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (rocket.design().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // in the cockpit the camera sits inside the capsule: draw the rest of the rocket, not the walls around it
        boolean cockpit = mc.options.getCameraType().isFirstPerson() && mc.player != null && mc.player.getVehicle() == rocket;
        RocketGeometry geo = rocket.geometry();
        FlightState state = rocket.state();
        float time = rocket.tickCount + partialTick;
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-rocket.tilt()));
        pose.mulPose(Axis.YP.rotationDegrees(-rocket.getYRot()));
        if (state == FlightState.ORBIT || state == FlightState.TRANSFER) {
            pose.translate(0, Mth.sin(time * 0.03F) * 0.08F, 0);
            pose.mulPose(Axis.YP.rotationDegrees(time * 0.15F));
        }
        // under the main canopy the capsule swings like a pendulum about the canopy (not in the cockpit view)
        float chuteT = rocket.timer() + partialTick;
        float capsuleW = rocket.layout().payload().seats() > 1 ? 1.4F : 1F;
        float top = (float) (geo.height() / 16.0);
        if (state == FlightState.PARACHUTE && !cockpit) {
            float[] sw = ParachuteRenderer.swing(chuteT, time);
            float pivot = top + ParachuteRenderer.lineLength(capsuleW);
            pose.translate(0, pivot, 0);
            pose.mulPose(Axis.XP.rotationDegrees(sw[0]));
            pose.mulPose(Axis.ZP.rotationDegrees(sw[1]));
            pose.translate(0, -pivot, 0);
        }
        float legs = legDeploy(rocket, partialTick);
        int skipFrom = cockpit && rocket.layout().payload().present() ? rocket.layout().payload().firstPart() : Integer.MAX_VALUE;
        RocketStackRenderer.render(rocket.layout(), geo, pose, buffers, light, legs, skipFrom);
        if (rocket.burning() && !(state == FlightState.ASCENT && rocket.ascentProgress() >= 0.955F)) {
            boolean vacuum = !Location.EARTH_SURFACE.equals(rocket.location()) || rocket.getY() > 400;
            float strength = state == FlightState.COUNTDOWN ? Mth.clamp((RocketEntity.IGNITION_TICKS - rocket.timer() + partialTick) / 30F, 0.15F, 1F)
                    : state == FlightState.DESCENT ? 0.65F : 1F;
            if (state == FlightState.ASCENT) {
                // engine cut-off just before the orbit view takes over: the rocket arrives coasting, as it will be there
                strength *= Mth.clamp((0.955F - rocket.ascentProgress()) / 0.025F, 0F, 1F);
            }
            float plume = strength;
            // the plume writes no depth: with a shader pack and our sky it is drawn after the sky (LateEffects)
            var layout = rocket.layout();
            int stage = rocket.activeStage();
            com.pizzaparker07.spacebreaker.compat.client.LateEffects.drawOrDefer(pose, buffers,
                    (p, b) -> RocketStackRenderer.renderPlume(layout, geo, stage, p, b, time, plume, vacuum));
        }
        // parachutes: in flight, and for a while after touchdown as the canopy collapses beside the capsule
        if (state == FlightState.PARACHUTE) {
            rocket.chuteLandedAt = -1;
            if (!cockpit) {
                pose.pushPose();
                pose.translate(0, top, 0);
                ParachuteRenderer.flying(pose, buffers, chuteT, time, capsuleW, light);
                pose.popPose();
            }
        } else if (rocket.chuteSeen && rocket.chuteLandedAt < 0) {
            rocket.chuteLandedAt = rocket.tickCount;
        }
        if (state == FlightState.PARACHUTE) {
            rocket.chuteSeen = true;
        }
        if (state != FlightState.PARACHUTE && rocket.chuteLandedAt >= 0 && state != FlightState.ASCENT && state != FlightState.COUNTDOWN) {
            ParachuteRenderer.landed(pose, buffers, rocket.tickCount - rocket.chuteLandedAt + partialTick, time, top, capsuleW, light);
        } else if (state == FlightState.ASCENT || state == FlightState.COUNTDOWN) {
            rocket.chuteSeen = false;
            rocket.chuteLandedAt = -1;
        }
        pose.popPose();
        LaunchEffects.render(rocket, partialTick, pose, buffers, light, entityRenderDispatcher.camera.getPosition());
        super.render(rocket, yaw, partialTick, pose, buffers, light);
    }

    private static float legDeploy(RocketEntity rocket, float partialTick) {
        return switch (rocket.state()) {
            case DESCENT -> Mth.clamp((rocket.timer() + partialTick - 40) / 40F, 0F, 1F);
            case IDLE -> Location.MOON_SURFACE.equals(rocket.location()) || rocket.activeStage() > 0 ? 1F : 0F;
            case COUNTDOWN, ASCENT -> Location.MOON_SURFACE.equals(rocket.location()) ? Math.max(0F, 1F - (rocket.timer() > 0 && rocket.state() == FlightState.ASCENT ? rocket.timer() / 40F : 0F)) : 0F;
            default -> 0F;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(RocketEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public boolean shouldRender(RocketEntity rocket, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }
}
