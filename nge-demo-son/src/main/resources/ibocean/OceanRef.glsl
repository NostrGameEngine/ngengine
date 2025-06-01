 

vec4 sampleReflection(
    in sampler2D tex,
    in mat4 viewProjMatrix,
    in vec3 wpos, 
    in vec3 normal, 
    float displace
){
    vec4 reflectionProj = viewProjMatrix * vec4(wpos, 1.0);
    reflectionProj.x = reflectionProj.x + displace * normal.x;
    reflectionProj.z = reflectionProj.z + displace * normal.z;
    reflectionProj /= reflectionProj.w;
    vec2 refUV = reflectionProj.xy * 0.5 + 0.5;
    vec4 reflection = texture(tex, refUV).rgba;
    
    float mask = reflection.a > 0.0 ? 1.0 : 0.0;
    float reflectionFactor = reflection.a - wpos.y;
    float edgeFactor = min(1.0-refUV.x, 1.0-refUV.y);
    edgeFactor = smoothstep(0.0, 0.9, edgeFactor);
    
    reflectionFactor = clamp(reflectionFactor*mask, 0.0, 1.0) * edgeFactor;
    reflection.a=reflectionFactor;

    return reflection;
}
