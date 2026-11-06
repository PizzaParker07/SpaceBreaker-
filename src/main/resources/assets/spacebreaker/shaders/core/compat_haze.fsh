#version 150

// Thick-atmosphere haze over a shader pack's finished frame (see compat/client/HazePass.java), in two draws:
//   Mode 0, blend (ZERO, SRC_COLOR): frame *= tint * (1 - f)   (the world's own dim, coloured daylight)
//   Mode 1, blend (ONE, ONE):        frame += haze * f         (the world's own fog colour and distances)

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform mat4 InvProjMat;
uniform mat4 InvViewMat;
uniform float HazeStart;
uniform float HazeEnd;
uniform int HazeShape;
uniform vec4 HazeColor;
uniform vec3 LightTint;
uniform int Mode;

in vec2 screenUv;

out vec4 fragColor;

void main() {
    float depth = texture(Sampler0, screenUv).r;
    if (depth >= 1.0) {
        discard; // sky: already ours
    }
    vec4 view = InvProjMat * vec4(vec3(screenUv, depth) * 2.0 - 1.0, 1.0);
    view.xyz /= view.w;
    // camera-relative, world-aligned position, as vanilla's fog uses it (the cylinder shape needs world up)
    vec3 rel = (InvViewMat * vec4(view.xyz, 0.0)).xyz;
    float dist = fog_distance(rel, HazeShape);
    float f = 0.0;
    if (dist > HazeStart) {
        f = dist < HazeEnd ? smoothstep(HazeStart, HazeEnd, dist) : 1.0;
    }
    f *= HazeColor.a;
    if (Mode == 0) {
        fragColor = vec4(LightTint * (1.0 - f), 1.0);
    } else {
        fragColor = vec4(HazeColor.rgb * f, 1.0);
    }
}
