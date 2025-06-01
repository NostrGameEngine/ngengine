#ifndef PI2
    #define PI2 6.283185307179586
#endif

#ifndef PI0_5
    #define PI0_5 1.5707963267948966
#endif

// Normals
/*
*
*   vec3 normal=texture(m_NormalMap,texCoord).xyz;
*   #ifndef OPENGL_NORMAL
*       convertDirectXNormal(normal);
*   #endif
*   #ifdef RG_NORMAL
*      reconstructNormalZ(normal);
*   #endif
*
*/

void unpackNormal(inout vec3 normal){ 
    normal = normalize(normal.xyz * vec3(2.) - vec3(1.));
}

void reconstructNormalZ(inout vec3 normal){
    normal.z = sqrt(1. - clamp(dot(normal.xy, normal.xy),0.,1.));
}

void convertDirectXNormal(inout vec3 normal){
    normal.y = -directXNormal.y;
}

// http://www.thetenthplanet.de/archives/1180
mat3 cotangent_frame(vec3 N, vec3 p, vec2 uv){
	// get edge vectors of the pixel triangle
	vec3 dp1 = dFdx( p );
	vec3 dp2 = dFdy( p );
	vec2 duv1 = dFdx( uv );
	vec2 duv2 = dFdy( uv );
	 
	// solve the linear system
	vec3 dp2perp = cross( dp2, N );
	vec3 dp1perp = cross( N, dp1 );
	vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
	vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
	 
	// construct a scale-invariant frame 
	float invmax = 1.0/sqrt( max( dot(T,T), dot(B,B) ) );
	return mat3( T * invmax, B * invmax, N );
}
 
mat3 approximateTBN(vec3 N, vec3 V, vec2 texcoord ){
    mat3 TBN = cotangent_frame( N, -V, texcoord );
    return TBN;
}