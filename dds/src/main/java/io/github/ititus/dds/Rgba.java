package io.github.ititus.dds;

import java.util.Objects;

public record Rgba(float a, float r, float g, float b) {

    public static final Rgba TRANSPARENT = new Rgba(0.0f, 0.0f, 0.0f, 0.0f);
    public static final Rgba BLACK = new Rgba(1.0f, 0.0f, 0.0f, 0.0f);
    public static final Rgba WHITE = new Rgba(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Rgba RED = new Rgba(1.0f, 1.0f, 0.0f, 0.0f);
    public static final Rgba GREEN = new Rgba(1.0f, 0.0f, 1.0f, 0.0f);
    public static final Rgba BLUE = new Rgba(1.0f, 0.0f, 0.0f, 1.0f);

    public Rgba {
        if (a < 0.0f || a > 1.0f) {
            throw new IllegalArgumentException("alpha");
        } else if (r < 0.0f || r > 1.0f) {
            throw new IllegalArgumentException("red");
        } else if (g < 0.0f || g > 1.0f) {
            throw new IllegalArgumentException("green");
        } else if (b < 0.0f || b > 1.0f) {
            throw new IllegalArgumentException("blue");
        }
    }

    public static Rgba fromR5G6B5(short color) {
        float r = ((color >>> 11) & 0x1f) / 31.0f;
        float g = ((color >>> 5) & 0x3f) / 63.0f;
        float b = (color & 0x1f) / 31.0f;
        return new Rgba(1.0f, r, g, b);
    }

    public static Rgba fromR8G8B8(int color) {
        float r = ((color >>> 16) & 0xff) / 255.0f;
        float g = ((color >>> 8) & 0xff) / 255.0f;
        float b = (color & 0xff) / 255.0f;
        return new Rgba(1.0f, r, g, b);
    }

    public static Rgba fromA8R8G8B8(int color) {
        float a = ((color >>> 24) & 0xff) / 255.0f;
        float r = ((color >>> 16) & 0xff) / 255.0f;
        float g = ((color >>> 8) & 0xff) / 255.0f;
        float b = (color & 0xff) / 255.0f;
        return new Rgba(a, r, g, b);
    }

    public Rgba lerp(Rgba target, float t) {
        Objects.requireNonNull(target, "target");
        if (t < 0.0f || t > 1.0f) {
            throw new IllegalArgumentException("t");
        } else if (t == 0.0f) {
            return this;
        } else if (t == 1.0f) {
            return target;
        }

        float s = 1.0f - t;
        return new Rgba(
                s * this.a() + t * target.a(),
                s * this.r() + t * target.r(),
                s * this.g() + t * target.g(),
                s * this.b() + t * target.b()
        );
    }

    public Rgba withAlpha(float a) {
        if (this.a() == a) {
            return this;
        }

        return new Rgba(a, this.r(), this.g(), this.b());
    }

    public int a8() {
        return (int) (this.a() * 255.0f) & 0xff;
    }

    public int r8() {
        return (int) (this.r() * 255.0f) & 0xff;
    }

    public int g8() {
        return (int) (this.g() * 255.0f) & 0xff;
    }

    public int b8() {
        return (int) (this.b() * 255.0f) & 0xff;
    }

    public int asA8R8G8B8() {
        return (this.a8() << 24) | (this.r8() << 16) | (this.g8() << 8) | this.b8();
    }
}
