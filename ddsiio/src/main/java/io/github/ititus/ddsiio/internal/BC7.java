package io.github.ititus.ddsiio.internal;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class BC7 {

    public static final BC.BlockDecoder DECODER = BC7::decode;

    private static void decode(ByteBuffer in, int[] out) {
        byte[] data = new byte[16];
        in.get(data);
        BitReader reader = new BitReader(data);

        int mode = 0;
        while (mode < 8 && reader.nextBit() == 0) {
            mode++;
        }

        switch (mode) {
            case 0 -> _decodeImpl(reader, out, 3, 4, 0, 0, 4, 0, 1, 0, 3, 0);
            case 1 -> _decodeImpl(reader, out, 2, 6, 0, 0, 6, 0, 0, 1, 3, 0);
            case 2 -> _decodeImpl(reader, out, 3, 6, 0, 0, 5, 0, 0, 0, 2, 0);
            case 3 -> _decodeImpl(reader, out, 2, 6, 0, 0, 7, 0, 1, 0, 2, 0);
            case 4 -> _decodeImpl(reader, out, 1, 0, 2, 1, 5, 6, 0, 0, 2, 3);
            case 5 -> _decodeImpl(reader, out, 1, 0, 2, 0, 7, 8, 0, 0, 2, 2);
            case 6 -> _decodeImpl(reader, out, 1, 0, 0, 0, 7, 7, 1, 0, 4, 0);
            case 7 -> _decodeImpl(reader, out, 2, 6, 0, 0, 5, 5, 1, 0, 2, 0);
            case 8 -> Arrays.fill(out, 0);
            default -> throw new AssertionError();
        }
    }

    private static void _decodeImpl(BitReader in, int[] out, int subsets, int partitionBits, int rotationBits, int indexSelectionBits, int colorBits, int alphaBits, int endpointPBits, int sharedPBits, int indexBits, int secondaryIndexBits) {
        assert subsets > 0 && subsets <= 3;
        if (subsets == 1) {
            assert partitionBits == 0;
        } else {
            assert partitionBits >= 0 && partitionBits <= 6;
        }
        assert rotationBits >= 0 && rotationBits <= 2;
        assert indexSelectionBits >= 0 && indexSelectionBits <= 1;
        assert colorBits > 0;
        assert colorBits + sharedPBits + endpointPBits <= Byte.SIZE;
        assert alphaBits >= 0;
        assert alphaBits + sharedPBits + endpointPBits <= Byte.SIZE;
        assert endpointPBits >= 0;
        assert sharedPBits >= 0;
        assert indexBits > 0 && indexBits <= 4;
        assert secondaryIndexBits >= 0 && secondaryIndexBits <= 4;

        int partition = in.nextBits(partitionBits);
        int rotation = in.nextBits(rotationBits);
        int indexSelection = in.nextBits(indexSelectionBits);

        // array of RGBA colors
        byte[] endpoints = new byte[subsets * 2 * 4];

        // color data

        int colorShift = Byte.SIZE - colorBits;
        // red
        for (int i = 0; i < 2 * subsets; i++) {
            endpoints[4 * i] = (byte) (in.nextBits(colorBits) << colorShift);
        }

        // green
        for (int i = 0; i < 2 * subsets; i++) {
            endpoints[4 * i + 1] = (byte) (in.nextBits(colorBits) << colorShift);
        }

        // blue
        for (int i = 0; i < 2 * subsets; i++) {
            endpoints[4 * i + 2] = (byte) (in.nextBits(colorBits) << colorShift);
        }

        // alpha
        int alphaShift;
        if (alphaBits > 0) {
            alphaShift = Byte.SIZE - alphaBits;
            for (int i = 0; i < 2 * subsets; i++) {
                endpoints[4 * i + 3] = (byte) (in.nextBits(alphaBits) << alphaShift);
            }
        } else {
            alphaShift = 0;
            for (int i = 0; i < 2 * subsets; i++) {
                endpoints[4 * i + 3] = (byte) 0xFF;
            }
        }

        // p bits

        if (endpointPBits > 0) {
            colorShift -= endpointPBits;
            if (alphaShift > 0) {
                alphaShift -= endpointPBits;
            }
            for (int i = 0; i < 2 * subsets; i++) {
                int p = in.nextBits(endpointPBits);
                endpoints[4 * i] = (byte) (Byte.toUnsignedInt(endpoints[4 * i]) | (p << colorShift));
                endpoints[4 * i + 1] = (byte) (Byte.toUnsignedInt(endpoints[4 * i + 1]) | (p << colorShift));
                endpoints[4 * i + 2] = (byte) (Byte.toUnsignedInt(endpoints[4 * i + 2]) | (p << colorShift));
                endpoints[4 * i + 3] = (byte) (Byte.toUnsignedInt(endpoints[4 * i + 3]) | (p << alphaShift));
            }
        }
        if (sharedPBits > 0) {
            colorShift -= sharedPBits;
            if (alphaShift > 0) {
                alphaShift -= sharedPBits;
            }
            for (int i = 0; i < subsets; i++) {
                int p = in.nextBits(sharedPBits);
                endpoints[4 * (2 * i)] = (byte) (Byte.toUnsignedInt(endpoints[4 * (2 * i)]) | (p << colorShift));
                endpoints[4 * (2 * i) + 1] = (byte) (Byte.toUnsignedInt(endpoints[4 * (2 * i) + 1]) | (p << colorShift));
                endpoints[4 * (2 * i) + 2] = (byte) (Byte.toUnsignedInt(endpoints[4 * (2 * i) + 2]) | (p << colorShift));
                endpoints[4 * (2 * i) + 3] = (byte) (Byte.toUnsignedInt(endpoints[4 * (2 * i) + 3]) | (p << alphaShift));
                endpoints[4 * (2 * i + 1)] = (byte) (Byte.toUnsignedInt(endpoints[4 * (2 * i + 1)]) | (p << colorShift));
                endpoints[4 * (2 * i + 1) + 1] = (byte) (Byte.toUnsignedInt(endpoints[4 * (2 * i + 1) + 1]) | (p << colorShift));
                endpoints[4 * (2 * i + 1) + 2] = (byte) (Byte.toUnsignedInt(endpoints[4 * (2 * i + 1) + 2]) | (p << colorShift));
                endpoints[4 * (2 * i + 1) + 3] = (byte) (Byte.toUnsignedInt(endpoints[4 * (2 * i + 1) + 3]) | (p << alphaShift));
            }
        }
        if (colorShift > 0 || alphaShift > 0) {
            for (int i = 0; i < 2 * subsets; i++) {
                if (colorShift > 0) {
                    endpoints[4 * i] = (byte) (Byte.toUnsignedInt(endpoints[4 * i]) | (Byte.toUnsignedInt(endpoints[4 * i]) >>> (Byte.SIZE - colorShift)));
                    endpoints[4 * i + 1] = (byte) (Byte.toUnsignedInt(endpoints[4 * i + 1]) | (Byte.toUnsignedInt(endpoints[4 * i + 1]) >>> (Byte.SIZE - colorShift)));
                    endpoints[4 * i + 2] = (byte) (Byte.toUnsignedInt(endpoints[4 * i + 2]) | (Byte.toUnsignedInt(endpoints[4 * i + 2]) >>> (Byte.SIZE - colorShift)));
                }
                if (alphaShift > 0) {
                    endpoints[4 * i + 3] = (byte) (Byte.toUnsignedInt(endpoints[4 * i + 3]) | (Byte.toUnsignedInt(endpoints[4 * i + 3]) >>> (Byte.SIZE - alphaShift)));
                }
            }
        }

        // indices

        byte[] indices = new byte[out.length];
        byte[] secondaryIndices;
        for (int i = 0; i < out.length; i++) {
            indices[i] = (byte) in.nextBits(isAnchorIndex(subsets, partition, i) ? indexBits - 1 : indexBits);
        }
        if (secondaryIndexBits > 0) {
            secondaryIndices = new byte[out.length];
            for (int i = 0; i < out.length; i++) {
                secondaryIndices[i] = (byte) in.nextBits(isAnchorIndex(subsets, partition, i) ? secondaryIndexBits - 1 : secondaryIndexBits);
            }
        } else {
            secondaryIndices = Arrays.copyOf(indices, out.length);
        }

        assert in.pos() == 128;

        // output interpolation
        assert out.length == 16;

        int[] partitionTable = PARTITIONS[subsets - 1][partition];
        int[] colorWeights = indexSelection == 0 || secondaryIndexBits == 0 ? I[indexBits - 1] : I[secondaryIndexBits - 1];
        int[] alphaWeights = indexSelection == 0 && secondaryIndexBits > 0 ? I[secondaryIndexBits - 1] : I[indexBits - 1];
        if (indexSelection != 0) {
            byte[] tmp = secondaryIndices;
            secondaryIndices = indices;
            indices = tmp;
        }
        for (int i = 0; i < out.length; i++) {
            int subset = partitionTable[i];
            int colorWeight = colorWeights[Byte.toUnsignedInt(indices[i])];
            int alphaWeight = alphaWeights[Byte.toUnsignedInt(secondaryIndices[i])];

            int r = (Byte.toUnsignedInt(endpoints[4 * (2 * subset)]) * (64 - colorWeight) + Byte.toUnsignedInt(endpoints[4 * (2 * subset + 1)]) * colorWeight + 32) >>> 6;
            int g = (Byte.toUnsignedInt(endpoints[4 * (2 * subset) + 1]) * (64 - colorWeight) + Byte.toUnsignedInt(endpoints[4 * (2 * subset + 1) + 1]) * colorWeight + 32) >>> 6;
            int b = (Byte.toUnsignedInt(endpoints[4 * (2 * subset) + 2]) * (64 - colorWeight) + Byte.toUnsignedInt(endpoints[4 * (2 * subset + 1) + 2]) * colorWeight + 32) >>> 6;
            int a = (Byte.toUnsignedInt(endpoints[4 * (2 * subset) + 3]) * (64 - alphaWeight) + Byte.toUnsignedInt(endpoints[4 * (2 * subset + 1) + 3]) * alphaWeight + 32) >>> 6;

            switch (rotation) {
                case 1 -> {
                    int tmp = a;
                    a = r;
                    r = tmp;
                }
                case 2 -> {
                    int tmp = a;
                    a = g;
                    g = tmp;
                }
                case 3 -> {
                    int tmp = a;
                    a = b;
                    b = tmp;
                }
            }

            out[i] = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }
    }

    private static final int[][] P1 = {
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
    };
    private static final int[][] P2 = {
            { 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1 },
            { 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1 },
            { 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1 },
            { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1 },
            { 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1 },
            { 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1 },
            { 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1 },
            { 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0 },
            { 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0 },
            { 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0 },
            { 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1 },
            { 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0 },
            { 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0 },
            { 0, 0, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 0, 0 },
            { 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0 },
            { 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0 },
            { 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0 },
            { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1 },
            { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1 },
            { 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0 },
            { 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0 },
            { 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0 },
            { 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0 },
            { 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1 },
            { 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1 },
            { 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 0 },
            { 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0 },
            { 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0 },
            { 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0 },
            { 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0 },
            { 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1 },
            { 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1 },
            { 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
            { 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0 },
            { 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0 },
            { 0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1 },
            { 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0, 1 },
            { 0, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0 },
            { 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0 },
            { 0, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1 },
            { 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1 },
            { 0, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1 },
            { 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1 },
            { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1 },
            { 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0 },
            { 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0 },
            { 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1 }
    };
    private static final int[][] P3 = {
            { 0, 0, 1, 1, 0, 0, 1, 1, 0, 2, 2, 1, 2, 2, 2, 2 },
            { 0, 0, 0, 1, 0, 0, 1, 1, 2, 2, 1, 1, 2, 2, 2, 1 },
            { 0, 0, 0, 0, 2, 0, 0, 1, 2, 2, 1, 1, 2, 2, 1, 1 },
            { 0, 2, 2, 2, 0, 0, 2, 2, 0, 0, 1, 1, 0, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 2, 1, 1, 2, 2 },
            { 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 2, 2, 0, 0, 2, 2 },
            { 0, 0, 2, 2, 0, 0, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 1, 1, 0, 0, 1, 1, 2, 2, 1, 1, 2, 2, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2 },
            { 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2 },
            { 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2 },
            { 0, 0, 1, 2, 0, 0, 1, 2, 0, 0, 1, 2, 0, 0, 1, 2 },
            { 0, 1, 1, 2, 0, 1, 1, 2, 0, 1, 1, 2, 0, 1, 1, 2 },
            { 0, 1, 2, 2, 0, 1, 2, 2, 0, 1, 2, 2, 0, 1, 2, 2 },
            { 0, 0, 1, 1, 0, 1, 1, 2, 1, 1, 2, 2, 1, 2, 2, 2 },
            { 0, 0, 1, 1, 2, 0, 0, 1, 2, 2, 0, 0, 2, 2, 2, 0 },
            { 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 2, 1, 1, 2, 2 },
            { 0, 1, 1, 1, 0, 0, 1, 1, 2, 0, 0, 1, 2, 2, 0, 0 },
            { 0, 0, 0, 0, 1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2 },
            { 0, 0, 2, 2, 0, 0, 2, 2, 0, 0, 2, 2, 1, 1, 1, 1 },
            { 0, 1, 1, 1, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2 },
            { 0, 0, 0, 1, 0, 0, 0, 1, 2, 2, 2, 1, 2, 2, 2, 1 },
            { 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 2, 2, 0, 1, 2, 2 },
            { 0, 0, 0, 0, 1, 1, 0, 0, 2, 2, 1, 0, 2, 2, 1, 0 },
            { 0, 1, 2, 2, 0, 1, 2, 2, 0, 0, 1, 1, 0, 0, 0, 0 },
            { 0, 0, 1, 2, 0, 0, 1, 2, 1, 1, 2, 2, 2, 2, 2, 2 },
            { 0, 1, 1, 0, 1, 2, 2, 1, 1, 2, 2, 1, 0, 1, 1, 0 },
            { 0, 0, 0, 0, 0, 1, 1, 0, 1, 2, 2, 1, 1, 2, 2, 1 },
            { 0, 0, 2, 2, 1, 1, 0, 2, 1, 1, 0, 2, 0, 0, 2, 2 },
            { 0, 1, 1, 0, 0, 1, 1, 0, 2, 0, 0, 2, 2, 2, 2, 2 },
            { 0, 0, 1, 1, 0, 1, 2, 2, 0, 1, 2, 2, 0, 0, 1, 1 },
            { 0, 0, 0, 0, 2, 0, 0, 0, 2, 2, 1, 1, 2, 2, 2, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 2, 2, 1, 2, 2, 2 },
            { 0, 2, 2, 2, 0, 0, 2, 2, 0, 0, 1, 2, 0, 0, 1, 1 },
            { 0, 0, 1, 1, 0, 0, 1, 2, 0, 0, 2, 2, 0, 2, 2, 2 },
            { 0, 1, 2, 0, 0, 1, 2, 0, 0, 1, 2, 0, 0, 1, 2, 0 },
            { 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 0, 0, 0, 0 },
            { 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0 },
            { 0, 1, 2, 0, 2, 0, 1, 2, 1, 2, 0, 1, 0, 1, 2, 0 },
            { 0, 0, 1, 1, 2, 2, 0, 0, 1, 1, 2, 2, 0, 0, 1, 1 },
            { 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 0, 0, 0, 0, 1, 1 },
            { 0, 1, 0, 1, 0, 1, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 2, 1, 2, 1, 2, 1 },
            { 0, 0, 2, 2, 1, 1, 2, 2, 0, 0, 2, 2, 1, 1, 2, 2 },
            { 0, 0, 2, 2, 0, 0, 1, 1, 0, 0, 2, 2, 0, 0, 1, 1 },
            { 0, 2, 2, 0, 1, 2, 2, 1, 0, 2, 2, 0, 1, 2, 2, 1 },
            { 0, 1, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 0, 1, 0, 1 },
            { 0, 0, 0, 0, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1 },
            { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 2, 2, 2, 2 },
            { 0, 2, 2, 2, 0, 1, 1, 1, 0, 2, 2, 2, 0, 1, 1, 1 },
            { 0, 0, 0, 2, 1, 1, 1, 2, 0, 0, 0, 2, 1, 1, 1, 2 },
            { 0, 0, 0, 0, 2, 1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2 },
            { 0, 2, 2, 2, 0, 1, 1, 1, 0, 1, 1, 1, 0, 2, 2, 2 },
            { 0, 0, 0, 2, 1, 1, 1, 2, 1, 1, 1, 2, 0, 0, 0, 2 },
            { 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 2, 2, 2, 2 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 2, 2, 1, 1, 2 },
            { 0, 1, 1, 0, 0, 1, 1, 0, 2, 2, 2, 2, 2, 2, 2, 2 },
            { 0, 0, 2, 2, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 2, 2 },
            { 0, 0, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2, 0, 0, 2, 2 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 1, 2 },
            { 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 1 },
            { 0, 2, 2, 2, 1, 2, 2, 2, 0, 2, 2, 2, 1, 2, 2, 2 },
            { 0, 1, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 },
            { 0, 1, 1, 1, 2, 0, 1, 1, 2, 2, 0, 1, 2, 2, 2, 0 },
    };
    private static final int[][][] PARTITIONS = new int[][][] { P1, P2, P3 };

    private static final int[] A2 = new int[] {
            15, 15, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 15, 15, 15, 15,
            15, 2, 8, 2, 2, 8, 8, 15,
            2, 8, 2, 2, 8, 8, 2, 2,
            15, 15, 6, 8, 2, 8, 15, 15,
            2, 8, 2, 2, 2, 15, 15, 6,
            6, 2, 6, 8, 15, 15, 2, 2,
            15, 15, 15, 15, 15, 2, 2, 15
    };
    private static final int[] A3_A = new int[] {
            3, 3, 15, 15, 8, 3, 15, 15,
            8, 8, 6, 6, 6, 5, 3, 3,
            3, 3, 8, 15, 3, 3, 6, 10,
            5, 8, 8, 6, 8, 5, 15, 15,
            8, 15, 3, 5, 6, 10, 8, 15,
            15, 3, 15, 5, 15, 15, 15, 15,
            3, 15, 5, 5, 5, 8, 5, 10,
            5, 10, 8, 13, 15, 12, 3, 3
    };
    private static final int[] A3_B = new int[] {
            15, 8, 8, 3, 15, 15, 3, 8,
            15, 15, 15, 15, 15, 15, 15, 8,
            15, 8, 15, 3, 15, 8, 15, 8,
            3, 15, 6, 10, 15, 15, 10, 8,
            15, 3, 15, 10, 10, 8, 9, 10,
            6, 15, 8, 15, 3, 6, 6, 8,
            15, 3, 15, 15, 15, 15, 15, 15,
            15, 15, 15, 15, 3, 15, 15, 8
    };

    private static boolean isAnchorIndex(int subsets, int partition, int i) {
        return switch (subsets) {
            case 1 -> i == 0;
            case 2 -> i == 0 || i == A2[partition];
            case 3 -> i == 0 || i == A3_A[partition] || i == A3_B[partition];
            default -> throw new AssertionError();
        };
    }

    private static final int[] I1 = { 0, 64 };
    private static final int[] I2 = { 0, 21, 43, 64 };
    private static final int[] I3 = { 0, 9, 18, 27, 37, 46, 55, 64 };
    private static final int[] I4 = { 0, 4, 9, 13, 17, 21, 26, 30, 34, 38, 43, 47, 51, 55, 60, 64 };
    private static final int[][] I = { I1, I2, I3, I4 };
}
