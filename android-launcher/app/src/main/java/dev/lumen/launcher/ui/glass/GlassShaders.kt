package dev.lumen.launcher.ui.glass

/**
 * AGSL (Android Graphics Shading Language) programs. AGSL runtime shaders need API 33; every call
 * site degrades gracefully below that (see [LiquidGlass] and the wallpaper layer).
 */
internal object GlassShaders {

    /**
     * The liquid-glass lens.
     *
     * `content` arrives pre-blurred (the blur is chained ahead of this shader), and this pass adds
     * the parts that make glass read as a physical object rather than a translucent rectangle:
     *
     *  * a signed-distance field of the panel's own superellipse silhouette,
     *  * edge refraction — the backdrop is pulled along the SDF normal, strongest at the rim, so
     *    content bends around the bevel the way it does through a thick pane,
     *  * chromatic dispersion — R and B sample at slightly different displacements,
     *  * a specular rim keyed to a virtual light direction, with a matching shadow on the far edge,
     *  * an optional travelling sheen, and film grain to break up banding.
     *
     * Colour maths happens un-premultiplied so brightening never over-shoots the alpha channel.
     */
    const val LIQUID_GLASS = """
        uniform shader content;
        uniform float2 uSize;
        uniform float uRadius;
        uniform float uExp;
        uniform float uBand;
        uniform float uRefract;
        uniform float uDisp;
        uniform float uSpec;
        uniform float uShade;
        uniform float2 uLight;
        uniform float uGrain;
        uniform float uBright;
        uniform float uSat;
        uniform float uShimmer;
        uniform float uPhase;

        float pnorm(float2 v, float n) {
            float x = max(v.x, 0.00001);
            float y = max(v.y, 0.00001);
            return pow(pow(x, n) + pow(y, n), 1.0 / n);
        }

        float sdShape(float2 p, float2 hs, float r, float n) {
            float2 q = abs(p) - hs + r;
            return min(max(q.x, q.y), 0.0) + pnorm(max(q, float2(0.0)), n) - r;
        }

        half4 main(float2 coord) {
            float2 hs = uSize * 0.5;
            float r = min(uRadius, min(hs.x, hs.y));
            float2 p = coord - hs;
            float d = sdShape(p, hs, r, uExp);

            float band = max(uBand, 1.0);
            float t = clamp(1.0 + d / band, 0.0, 1.0);
            float lens = pow(t, 2.4);

            float e = 1.5;
            float gx = sdShape(p + float2(e, 0.0), hs, r, uExp) - sdShape(p - float2(e, 0.0), hs, r, uExp);
            float gy = sdShape(p + float2(0.0, e), hs, r, uExp) - sdShape(p - float2(0.0, e), hs, r, uExp);
            float2 n = float2(gx, gy);
            float nl = length(n);
            n = nl > 0.0001 ? n / nl : float2(0.0, 0.0);

            float2 offset = n * (uRefract * lens);

            float4 col;
            if (uDisp > 0.001) {
                half4 cr = content.eval(coord - offset * (1.0 + 0.12 * uDisp));
                half4 cg = content.eval(coord - offset);
                half4 cb = content.eval(coord - offset * (1.0 - 0.12 * uDisp));
                col = float4(float(cr.r), float(cg.g), float(cb.b),
                    float(max(max(cr.a, cg.a), cb.a)));
            } else {
                col = float4(content.eval(coord - offset));
            }

            float a = col.a;
            float3 rgb = a > 0.0001 ? col.rgb / a : float3(0.0);

            float rim = pow(t, 5.0);
            float facing = clamp(dot(n, uLight), 0.0, 1.0);
            float away = clamp(-dot(n, uLight), 0.0, 1.0);
            float spec = pow(facing, 2.5) * rim * uSpec;
            float shade = pow(away, 2.5) * rim * uShade;

            if (uShimmer > 0.001) {
                float axis = (coord.x + coord.y) / max(uSize.x + uSize.y, 1.0);
                float s = fract(axis - uPhase) - 0.5;
                spec += exp(-s * s * 42.0) * uShimmer * (0.35 + 0.65 * rim);
            }

            rgb += spec;
            rgb -= shade;
            rgb += uBright;

            float lum = dot(rgb, float3(0.2126, 0.7152, 0.0722));
            rgb = mix(float3(lum), rgb, uSat);

            if (uGrain > 0.001) {
                float g = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453);
                rgb += (g - 0.5) * uGrain;
            }

            rgb = clamp(rgb, 0.0, 1.0);
            return half4(half3(rgb * a), half(a));
        }
    """

    /**
     * Bundled animated wallpaper: four drifting colour wells summed with Gaussian weights, warped
     * by a slow flow field. It exists so the glass always has something worth refracting even when
     * the platform refuses to hand a third-party launcher the real wallpaper bitmap.
     */
    const val LIQUID_MESH = """
        uniform float2 uSize;
        uniform float uTime;
        uniform float uComplexity;
        uniform float uSat;
        layout(color) uniform float4 uC0;
        layout(color) uniform float4 uC1;
        layout(color) uniform float4 uC2;
        layout(color) uniform float4 uC3;
        layout(color) uniform float4 uBase;

        float2 flow(float2 p, float t) {
            float amp = 0.18 * uComplexity;
            return p + amp * float2(
                sin(p.y * 2.1 + t * 0.7) + 0.5 * sin(p.x * 1.3 - t * 0.5),
                cos(p.x * 1.9 - t * 0.6) + 0.5 * cos(p.y * 1.7 + t * 0.4)
            );
        }

        float well(float2 p, float2 c, float spread) {
            float2 d = p - c;
            return exp(-dot(d, d) * spread);
        }

        half4 main(float2 coord) {
            float aspect = uSize.x / max(uSize.y, 1.0);
            float2 p = (coord / uSize) * 2.0 - 1.0;
            p.x *= aspect;
            float t = uTime;
            p = flow(p, t);

            float2 c0 = float2(sin(t * 0.31) * 0.75 * aspect, cos(t * 0.27) * 0.65);
            float2 c1 = float2(cos(t * 0.23 + 1.7) * 0.85 * aspect, sin(t * 0.19 + 0.9) * 0.75);
            float2 c2 = float2(sin(t * 0.17 + 3.1) * 0.70 * aspect, cos(t * 0.29 + 2.2) * 0.80);
            float2 c3 = float2(cos(t * 0.13 + 4.4) * 0.90 * aspect, sin(t * 0.21 + 3.8) * 0.60);

            float w0 = well(p, c0, 1.35);
            float w1 = well(p, c1, 1.15);
            float w2 = well(p, c2, 1.55);
            float w3 = well(p, c3, 1.05);
            float total = w0 + w1 + w2 + w3 + 0.55;

            float3 rgb = (uBase.rgb * 0.55
                + uC0.rgb * w0 + uC1.rgb * w1 + uC2.rgb * w2 + uC3.rgb * w3) / total;

            float lum = dot(rgb, float3(0.2126, 0.7152, 0.0722));
            rgb = mix(float3(lum), rgb, uSat);

            // Fine dither keeps wide gradients from banding on 8-bit panels.
            float dither = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453) - 0.5;
            rgb += dither * 0.006;

            return half4(half3(clamp(rgb, 0.0, 1.0)), 1.0);
        }
    """

    /** Aurora ribbons: layered sine sheets with height-falloff, for the second bundled wallpaper. */
    const val AURORA = """
        uniform float2 uSize;
        uniform float uTime;
        uniform float uComplexity;
        uniform float uSat;
        layout(color) uniform float4 uC0;
        layout(color) uniform float4 uC1;
        layout(color) uniform float4 uC2;
        layout(color) uniform float4 uBase;

        float ribbon(float2 p, float phase, float freq, float thickness) {
            float y = sin(p.x * freq + phase) * 0.25 + sin(p.x * freq * 0.53 - phase * 0.7) * 0.12;
            float d = abs(p.y - y);
            return exp(-d * d / max(thickness, 0.0001));
        }

        half4 main(float2 coord) {
            float aspect = uSize.x / max(uSize.y, 1.0);
            float2 p = (coord / uSize) * 2.0 - 1.0;
            p.x *= aspect;
            float t = uTime;
            float k = 1.0 + uComplexity * 2.5;

            float r0 = ribbon(p, t * 0.6, 1.7 * k, 0.05);
            float r1 = ribbon(p + float2(0.0, 0.35), t * 0.45 + 2.0, 1.1 * k, 0.09);
            float r2 = ribbon(p - float2(0.0, 0.45), t * 0.33 + 4.0, 2.3 * k, 0.03);

            float vignette = 1.0 - 0.35 * dot(p * 0.55, p * 0.55);
            float3 rgb = uBase.rgb
                + uC0.rgb * r0 * 0.9
                + uC1.rgb * r1 * 0.7
                + uC2.rgb * r2 * 0.55;
            rgb *= vignette;

            float lum = dot(rgb, float3(0.2126, 0.7152, 0.0722));
            rgb = mix(float3(lum), rgb, uSat);

            float dither = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453) - 0.5;
            rgb += dither * 0.006;

            return half4(half3(clamp(rgb, 0.0, 1.0)), 1.0);
        }
    """
}
