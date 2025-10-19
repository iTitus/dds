/*
This file was ported from a C++ implementation given by Microsoft in DirectXTK and their documentation.

MIT License

Copyright (c) Microsoft Corporation.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE
*/

package io.github.ititus.dds;

import java.io.IOException;
import java.util.NoSuchElementException;

public enum D3d10ResourceDimension {

    UNKNOWN(0),
    BUFFER(1),
    TEXTURE1D(2),
    TEXTURE2D(3),
    TEXTURE3D(4);

    private static final D3d10ResourceDimension[] VALUES = values();

    private final int value;

    D3d10ResourceDimension(int value) {
        this.value = value;
    }

    public static D3d10ResourceDimension load(DataReader r) throws IOException {
        try {
            return get(r.readUInt());
        } catch (NoSuchElementException e) {
            throw new IOException(e);
        }
    }

    public static D3d10ResourceDimension get(int value) {
        for (D3d10ResourceDimension d : VALUES) {
            if (d.value == value) {
                return d;
            }
        }

        throw new NoSuchElementException("unknown resource dimension");
    }

    public int value() {
        return value;
    }

    public boolean isTexture() {
        return switch (this) {
            case TEXTURE1D, TEXTURE2D, TEXTURE3D -> true;
            default -> false;
        };
    }
}
