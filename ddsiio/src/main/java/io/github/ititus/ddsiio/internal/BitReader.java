package io.github.ititus.ddsiio.internal;

public final class BitReader {

    private final byte[] data;
    private int pos;

    public BitReader(byte[] data) {
        this.data = data;
        this.pos = 0;
    }

    public int pos() {
        return this.pos;
    }

    public void reset() {
        this.pos = 0;
    }

    private int dataPos() {
        return this.pos / Byte.SIZE;
    }

    private int bitPos() {
        return this.pos % Byte.SIZE;
    }

    public int nextBit() {
        byte element = this.data[this.dataPos()];
        int result = (Byte.toUnsignedInt(element) >>> this.bitPos()) & 0b1;
        this.pos++;
        return result;
    }

    public int nextBits(int bitAmount) {
        if (bitAmount < 0 || bitAmount > Integer.SIZE) {
            throw new IllegalArgumentException("bitAmount must be between 0 and " + Integer.SIZE);
        }

        int result = 0;
        for (int i = 0; i < bitAmount; ) {
            int remaining = Math.min(Byte.SIZE - this.bitPos(), bitAmount - i);
            byte element = this.data[this.dataPos()];
            result |= ((Byte.toUnsignedInt(element) >>> this.bitPos()) & ((1 << remaining) - 1)) << i;
            this.pos += remaining;
            i += remaining;
        }
        return result;
    }
}
