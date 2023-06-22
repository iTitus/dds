package io.github.ititus.dds;

public record Rgba(float a, float r, float g, float b) {

    public static final Rgba TRANSPARENT = new Rgba(0.0f, 0.0f, 0.0f, 0.0f);
    public static final Rgba BLACK = new Rgba(1.0f, 0.0f, 0.0f, 0.0f);
    public static final Rgba WHITE = new Rgba(1.0f, 1.0f, 1.0f, 1.0f);

    public static Rgba from565(short color) {
        float r = ((Short.toUnsignedInt(color) & 0xf800) >> 11) / 31.0f;
        float g = ((Short.toUnsignedInt(color) & 0x7e0) >> 5) / 63.0f;
        float b = (Short.toUnsignedInt(color) & 0x1f) / 31.0f;
        return new Rgba(1.0f, r, g, b);
    }

    public Rgba lerp(Rgba target, float t) {
        float s = 1.0f - t;
        return new Rgba(
                s * this.a() + t * target.a(),
                s * this.r() + t * target.r(),
                s * this.g() + t * target.g(),
                s * this.b() + t * target.b()
        );
    }

    public Rgba withAlpha(float a) {
        return new Rgba(a, this.r(), this.g(), this.b());
    }

    public int a8() {
        return (int) (this.a() * 255.0f);
    }

    public int r8() {
        return (int) (this.r() * 255.0f);
    }

    public int g8() {
        return (int) (this.g() * 255.0f);
    }

    public int b8() {
        return (int) (this.b() * 255.0f);
    }

    public int asARGB() {
        return ((this.a8() & 0xff) << 24) | ((this.r8() & 0xff) << 16) | ((this.g8() & 0xff) << 8) | (this.b8() & 0xff);
    }
}
