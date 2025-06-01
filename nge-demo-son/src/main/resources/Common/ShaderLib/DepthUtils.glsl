
// Depth

float linearizeDepth(in float depth,in vec2 frustumNearFar){
    float f = frustumNearFar.y;
    float n = frustumNearFar.x;
    float d=depth*2.-1.;
    return (2. * n * f) / (f + n - d * (f - n));
}


float linearizeDepth01(in float depth,in vec2 frustumNearFar){
    float d=linearizeDepth(depth,frustumNearFar);
    float f = frustumNearFar.y;
    float n = frustumNearFar.x;
    return (d-n)/(f-n);
}
