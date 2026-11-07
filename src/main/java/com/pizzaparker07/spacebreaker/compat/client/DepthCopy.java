package com.pizzaparker07.spacebreaker.compat.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/**
 * A copy of the frame's depth in a texture of our own, so it can be sampled while the frame is drawn to (no
 * feedback loop) or kept from an earlier point of the frame. glCopyTexSubImage2D converts between depth formats,
 * unlike a blit. Only the read framebuffer binding is touched, and it is restored: this also runs in the middle of a
 * shader pack's frame, whose own framebuffer must stay bound.
 */
final class DepthCopy {
    private int texture = -1;
    private int width;
    private int height;

    int texture() {
        return texture;
    }

    void copyFrom(RenderTarget main) {
        if (texture < 0 || width != main.width || height != main.height) {
            if (texture < 0) {
                texture = GlStateManager._genTexture();
            }
            width = main.width;
            height = main.height;
            GlStateManager._bindTexture(texture);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, width, height, 0,
                    GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null);
        }
        int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._bindTexture(texture);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        GlStateManager._bindTexture(0);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
    }

    void release() {
        if (texture >= 0) {
            RenderSystem.deleteTexture(texture);
            texture = -1;
        }
    }
}
