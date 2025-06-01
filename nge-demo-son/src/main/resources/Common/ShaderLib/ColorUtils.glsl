// #define SRGB_FAST_APPROXIMATION 1

#ifndef PI
    #define PI 3.141592653589793
#endif

#ifndef PI2
    #define PI2 6.283185307179586
#endif

#ifndef PI0_5
    #define PI0_5 1.5707963267948966
#endif

#define saturate(x) clamp(x,0.,1.)



// Colors
vec4 sRGBtoLinear(vec4 srgbIn){
    #ifdef SRGB_FAST_APPROXIMATION
       vec3 linOut = pow(srgbIn.xyz,vec3(2.2));
    #else
       vec3 i = step(vec3(0.04045),srgbIn.xyz);
       vec3 linOut = mix( srgbIn.xyz/vec3(12.92), pow((srgbIn.xyz+vec3(0.055))/vec3(1.055),vec3(2.4)), i );
    #endif
    return vec4(linOut,srgbIn.w);
}

vec4 linearToSRGB(vec4 linearIn){
    vec3 S1 = sqrt(linearIn.rgb);
    vec3 S2 = sqrt(S1);
    vec3 S3 = sqrt(S2);
    vec3 sRGB = 0.662002687 * S1 + 0.684122060 * S2 - 0.323583601 * S3 - 0.0225411470 * linearIn.rgb;
    return vec4(sRGB,linearIn.a);
}

vec3 rgb2hsv(vec3 c){
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c){
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

