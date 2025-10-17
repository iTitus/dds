package io.github.ititus.ddsiio.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BitReaderTest {

    @Test
    void testNextBit1() {
        var r = new BitReader(new byte[] { (byte) 0b1010_1010 });
        assertEquals(0, r.nextBit());
        assertEquals(1, r.nextBit());
        assertEquals(0, r.nextBit());
        assertEquals(1, r.nextBit());
        assertEquals(0, r.nextBit());
        assertEquals(1, r.nextBit());
        assertEquals(0, r.nextBit());
        assertEquals(1, r.nextBit());
        assertThrows(ArrayIndexOutOfBoundsException.class, r::nextBit);
    }

    @Test
    void testNextBit2() {
        var r = new BitReader(new byte[] { (byte) 0b0101_0101 });
        assertEquals(1, r.nextBit());
        assertEquals(0, r.nextBit());
        assertEquals(1, r.nextBit());
        assertEquals(0, r.nextBit());
        assertEquals(1, r.nextBit());
        assertEquals(0, r.nextBit());
        assertEquals(1, r.nextBit());
        assertEquals(0, r.nextBit());
        assertThrows(ArrayIndexOutOfBoundsException.class, r::nextBit);
    }

    @Test
    void testNextBitsFullByte1() {
        byte b = (byte) 0b1010_1010;
        var r = new BitReader(new byte[] { b });
        assertEquals(Byte.toUnsignedInt(b), r.nextBits(Byte.SIZE));
        assertThrows(ArrayIndexOutOfBoundsException.class, r::nextBit);
    }

    @Test
    void testNextBitsFullByte2() {
        byte b = (byte) 0b0101_0101;
        var r = new BitReader(new byte[] { b });
        assertEquals(Byte.toUnsignedInt(b), r.nextBits(Byte.SIZE));
        assertThrows(ArrayIndexOutOfBoundsException.class, r::nextBit);
    }

    @Test
    void testNextBits() {
        byte b = (byte) 0b1010_1010;
        var r = new BitReader(new byte[] { b });
        r.nextBit();
        assertEquals(Byte.toUnsignedInt((byte) 0b10_101), r.nextBits(5));
    }
}
