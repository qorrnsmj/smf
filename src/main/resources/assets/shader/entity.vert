#version 330 core

const float DENSITY = 0.007;
const float GRADIENT = 1.5;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform bool fakeLighting;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec4 tangent;

layout(location = 0) out vec2 outTexCoord;
layout(location = 1) out vec4 outTangent;
layout(location = 2) out vec3 worldPosition;
layout(location = 3) out vec3 worldNormal;
layout(location = 4) out vec3 viewPosition;
layout(location = 5) out float visibility;

void main() {
    gl_Position = projection * view * model * vec4(position, 1.0);

    vec3 n = normalize(normal);
    if (fakeLighting) {
        n = vec3(0.0, 1.0, 0.0);
    }

    // outputs to fragment shader
    outTexCoord = texCoord;
    outTangent = tangent;
    worldPosition = vec3(model * vec4(position, 1.0));
    worldNormal = mat3(transpose(inverse(model))) * n;
    viewPosition = vec3(inverse(view)[3]);

    // fog visibility
    float distance = length(viewPosition - worldPosition);
    visibility = exp(-pow(distance * DENSITY, GRADIENT));
    visibility = clamp(visibility, 0.0, 1.0);
}
