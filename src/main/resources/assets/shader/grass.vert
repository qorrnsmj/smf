#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in float bendWeight;
layout(location = 2) in vec3 vertexColor;
layout(location = 3) in vec3 instancePosition;
layout(location = 4) in vec4 instanceParams;
layout(location = 5) in vec2 texCoord;

uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;
uniform vec3 cameraPosition;
uniform vec2 windDirection;
uniform float windStrength;
uniform float windSpeed;
uniform float timeSeconds;
uniform float drawDistance;

out vec3 worldPosition;
out float tint;
out vec4 lightSpacePosition;
out vec3 foliageColor;
out vec2 foliageTexCoord;

void main() {
    float scale = instanceParams.x;
    float angle = instanceParams.y;
    float phase = instanceParams.z;
    float distanceToCamera = length(cameraPosition.xz - instancePosition.xz);
    float visibility = 1.0 - smoothstep(drawDistance * 0.75, drawDistance, distanceToCamera);
    vec3 local = position * scale;
    local.xz = mat2(cos(angle), sin(angle), -sin(angle), cos(angle)) * local.xz;

    // Geometry stores bend weight directly: roots stay fixed, tips bend freely.
    float weight = bendWeight;
    vec2 direction = windDirection / max(length(windDirection), 0.0001);
    float time = timeSeconds * windSpeed;
    float gust = sin(dot(instancePosition.xz, vec2(0.31, 0.19)) - time);
    float flutter = sin(time * 2.3 + phase + position.x * 3.0);
    local.xz += direction * (0.6 + gust * 0.65 + flutter * 0.2) * windStrength * scale * weight;
    local *= visibility;
    worldPosition = instancePosition + local;
    tint = instanceParams.w;
    foliageColor = vertexColor;
    foliageTexCoord = texCoord;
    lightSpacePosition = lightSpaceMatrix * vec4(worldPosition, 1.0);
    gl_Position = projection * view * vec4(worldPosition, 1.0);
}
