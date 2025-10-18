package io.github.ititus.ddsiio.internal;

import java.util.Objects;

public record Argb(byte a, byte r, byte g, byte b) {

    public static final Argb TRANSPARENT = fromA8R8G8B8(0x00000000);
    public static final Argb BLACK = fromA8R8G8B8(0xff000000);
    public static final Argb WHITE = fromA8R8G8B8(0xffffffff);
    public static final Argb RED = fromA8R8G8B8(0xffff0000);
    public static final Argb GREEN = fromA8R8G8B8(0xff00ff00);
    public static final Argb BLUE = fromA8R8G8B8(0xff0000ff);

    public static Argb fromR5G6B5(short color) {
        int r = (color >>> 11) & 0x1f;
        int g = (color >>> 5) & 0x3f;
        int b = color & 0x1f;
        // exact multiplications would be 255/31 ≈ 8.2258 and 255/63 ≈ 4.0476 respectively
        return new Argb(
                (byte) 255,
                (byte) ((r << 3) | (r >>> 2)), // equivalent to r * 8.25
                (byte) ((g << 2) | (g >>> 4)), // equivalent to g * 4.0625
                (byte) ((b << 3) | (b >>> 2))  // equivalent to b * 8.25
        );
    }

    public static Argb fromR8G8B8(int color) {
        int r = (color >>> 16) & 0xff;
        int g = (color >>> 8) & 0xff;
        int b = color & 0xff;
        return new Argb((byte) 255, (byte) r, (byte) g, (byte) b);
    }

    public static Argb fromA8R8G8B8(int color) {
        int a = (color >>> 24) & 0xff;
        int r = (color >>> 16) & 0xff;
        int g = (color >>> 8) & 0xff;
        int b = color & 0xff;
        return new Argb((byte) a, (byte) r, (byte) g, (byte) b);
    }

    public Argb lerp(Argb target, int n1, int n2) {
        Objects.requireNonNull(target, "target");
        if (n1 < 0) {
            throw new IllegalArgumentException("n1");
        } else if (n2 < 0) {
            throw new IllegalArgumentException("n2");
        } else if (n1 == 0) {
            return target;
        } else if (n2 == 0) {
            return this;
        }

        int n = n1 + n2;
        return new Argb(
                (byte) ((n1 * this.a8() + n2 * target.a8() + n / 2) / n),
                (byte) ((n1 * this.r8() + n2 * target.r8() + n / 2) / n),
                (byte) ((n1 * this.g8() + n2 * target.g8() + n / 2) / n),
                (byte) ((n1 * this.b8() + n2 * target.b8() + n / 2) / n)
        );
    }

    public Argb withAlpha(byte a) {
        if (this.a() == a) {
            return this;
        }

        return new Argb(a, this.r(), this.g(), this.b());
    }

    public Argb withRed(byte r) {
        if (this.r() == r) {
            return this;
        }

        return new Argb(this.a(), r, this.g(), this.b());
    }

    public Argb withGreen(byte g) {
        if (this.g() == g) {
            return this;
        }

        return new Argb(this.a(), this.r(), g, this.b());
    }

    public Argb withBlue(byte b) {
        if (this.b() == b) {
            return this;
        }

        return new Argb(this.a(), this.r(), this.g(), b);
    }

    public int a8() {
        return Byte.toUnsignedInt(this.a());
    }

    public int r8() {
        return Byte.toUnsignedInt(this.r());
    }

    public int g8() {
        return Byte.toUnsignedInt(this.g());
    }

    public int b8() {
        return Byte.toUnsignedInt(this.b());
    }

    public int asA8R8G8B8() {
        return (this.a8() << 24) | (this.r8() << 16) | (this.g8() << 8) | this.b8();
    }
}
