package com.pizzaparker07.spacebreaker.client.blueprint;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Passes vertices through, tinting them hologram-cyan and making them translucent. */
final class GhostVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float r, g, b;
    private final int alpha;

    GhostVertexConsumer(VertexConsumer delegate, float r, float g, float b, float alpha) {
        this.delegate = delegate;
        this.r = r;
        this.g = g;
        this.b = b;
        this.alpha = Math.round(alpha * 255);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int a) {
        // keep a hint of the block's own shading, otherwise clearly the tint colour
        int cr = Math.round((red * 0.15F + 255 * 0.85F) * r);
        int cg = Math.round((green * 0.15F + 255 * 0.85F) * g);
        int cb = Math.round((blue * 0.15F + 255 * 0.85F) * b);
        delegate.setColor(Math.min(255, cr), Math.min(255, cg), Math.min(255, cb), alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }
}
