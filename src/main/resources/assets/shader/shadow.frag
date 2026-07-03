#version 330 core

uniform bool pointShadowPass;
uniform vec3 pointLightPosition;
uniform float pointShadowFarPlane;

layout(location = 0) in vec3 worldPosition;

void main() {
    if (pointShadowPass) {
        float lightDistance = length(worldPosition - pointLightPosition);
        gl_FragDepth = clamp(lightDistance / pointShadowFarPlane, 0.0, 1.0);
    }
}
