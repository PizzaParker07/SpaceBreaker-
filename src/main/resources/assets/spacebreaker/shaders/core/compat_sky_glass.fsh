#version 150

// Our sky behind glass, water and other translucent surfaces over a shader pack's frame. The fixed-function depth
// test (depth range 1..1, GREATER) keeps pixels where something was drawn; here the depth from before the translucent
// pass must still be far: only translucent geometry is in front of the sky there. The pack has blended the
// translucent surface over its own sky; that colour is kept at GlassKeep (blend), the rest becomes our sky.

uniform sampler2D Sampler0; // our sky
uniform sampler2D Sampler1; // depth before the translucent pass
uniform float GlassKeep;

in vec2 screenUv;

out vec4 fragColor;

void main() {
    if (texture(Sampler1, screenUv).r < 1.0) {
        discard;
    }
    fragColor = vec4(texture(Sampler0, screenUv).rgb, 1.0 - GlassKeep);
}
