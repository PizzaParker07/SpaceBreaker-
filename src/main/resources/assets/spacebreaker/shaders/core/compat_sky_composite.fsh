#version 150

// Our sky over a shader pack's frame with Distant Horizons: Minecraft's depth is tested by the fixed-function depth
// test (depth range 1..1, LEQUAL); here the LOD depth must be at its far value too, or LOD terrain is kept.

uniform sampler2D Sampler0; // our sky
uniform sampler2D Sampler1; // Distant Horizons depth
uniform float LodFar;

in vec2 screenUv;

out vec4 fragColor;

void main() {
    float lod = texture(Sampler1, screenUv).r;
    if (abs(lod - LodFar) > 1.0e-6) {
        discard;
    }
    fragColor = vec4(texture(Sampler0, screenUv).rgb, 1.0);
}
