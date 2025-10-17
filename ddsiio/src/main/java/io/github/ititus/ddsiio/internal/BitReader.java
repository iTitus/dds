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

    private void next() {
        this.pos++;
    }

    private int bit() {
        byte element = this.data[this.dataPos()];
        return (Byte.toUnsignedInt(element) >>> this.bitPos()) & 0b1;
    }

    public int nextBit() {
        return this.nextBits(1);
    }

    public int nextBits(int bitAmount) {
        if (bitAmount < 0 || bitAmount > Integer.SIZE) {
            throw new IllegalArgumentException("bitAmount must be between 0 and " + Integer.SIZE);
        }

        int result = 0;
        for (int i = 0; i < bitAmount; i++) {
            result |= this.bit() << i;
            this.next();
        }
        return result;
    }
}
