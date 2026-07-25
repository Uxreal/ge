package dev.lumen.launcher.core.design.surface

/**
 * AGSL for the frosted surface material.
 *
 * This is an approximation of frosted glass built from a signed-distance field, not a reproduction
 * of any platform vendor's material (§1.2). Runtime shaders need API 33; [FrostedSurface] falls back
 * to a plain `RenderEffect` blur below that.
 *
 * Per §1.1 and §3, this material is *not* applied to every surface — only where a sheet has to
 * establish layering over content behind it. Blur everywhere destroys hierarchy.
 */
internal object FrostedShaders {

    /**
     * `content` arrives pre-blurred (the blur is chained ahead of this pass). What this adds is the
     * part that makes a surface read as a physical sheet rather than a translucent rectangle:
     *
     *  * a signed-distance field of the surface's own superellipse silhouette,
     *  * edge refraction — the backdrop is displaced along the SDF normal, strongest at the rim, so
     *    content bends around the bevel the way it does through a real edge,
     *  * a slight chromatic split at the rim, since a physical edge disperses,
     *  * a specular highlight keyed to one virtual light, with a matching shade on the far edge.
     *
     * Colour maths runs un-premultiplied so lifting the rim never overshoots alpha.
     */
    const val FROSTED_LENS = """
        uniform shader content;
        uniform float2 uSize;
        uniform float uRadius;
        uniform float uSmoothness;
        uniform float uBand;
        uniform float uRefract;
        uniform float uDisperse;
        uniform float uSpecular;
        uniform float uShade;
        uniform float2 uLight;
        uniform float uBrighten;
        uniform float uSaturation;

        float pnorm(float2 v, float n) {
            float x = max(v.x, 0.00001);
            float y = max(v.y, 0.00001);
            return pow(pow(x, n) + pow(y, n), 1.0 / n);
        }

        // Superellipse SDF. Shares uSmoothness with the clip path so the rim never drifts from the
        // visible edge at the corners.
        float sdSuperellipse(float2 p, float2 hs, float r, float n) {
            float2 q = abs(p) - hs + r;
            return min(max(q.x, q.y), 0.0) + pnorm(max(q, float2(0.0)), n) - r;
        }

        half4 main(float2 coord) {
            float2 hs = uSize * 0.5;
            float r = min(uRadius, min(hs.x, hs.y));
            float2 p = coord - hs;
            float d = sdSuperellipse(p, hs, r, uSmoothness);

            float band = max(uBand, 1.0);
            float t = clamp(1.0 + d / band, 0.0, 1.0);
            float lens = pow(t, 2.4);

            float e = 1.5;
            float gx = sdSuperellipse(p + float2(e, 0.0), hs, r, uSmoothness)
                     - sdSuperellipse(p - float2(e, 0.0), hs, r, uSmoothness);
            float gy = sdSuperellipse(p + float2(0.0, e), hs, r, uSmoothness)
                     - sdSuperellipse(p - float2(0.0, e), hs, r, uSmoothness);
            float2 n = float2(gx, gy);
            float nl = length(n);
            n = nl > 0.0001 ? n / nl : float2(0.0, 0.0);

            float2 offset = n * (uRefract * lens);

            float4 col;
            if (uDisperse > 0.001) {
                half4 cr = content.eval(coord - offset * (1.0 + 0.12 * uDisperse));
                half4 cg = content.eval(coord - offset);
                half4 cb = content.eval(coord - offset * (1.0 - 0.12 * uDisperse));
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
            rgb += pow(facing, 2.5) * rim * uSpecular;
            rgb -= pow(away, 2.5) * rim * uShade;
            rgb += uBrighten;

            float lum = dot(rgb, float3(0.2126, 0.7152, 0.0722));
            rgb = mix(float3(lum), rgb, uSaturation);

            rgb = clamp(rgb, 0.0, 1.0);
            return half4(half3(rgb * a), half(a));
        }
    """
}
