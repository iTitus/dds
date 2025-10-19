package io.github.ititus.dds;

import static io.github.ititus.dds.DdsConstants.*;

public final class DdsHelper {

    private DdsHelper() {
    }

    public static PixelFormat derivePixelFormat(DdsFile dds) {
        return derivePixelFormat(dds.header(), dds.header10());
    }

    public static PixelFormat derivePixelFormat(DdsHeader header, DdsHeaderDxt10 header10) {
        if (header10 != null) {
            return header10.dxgiFormat();
        } else {
            return derivePixelFormat(header.ddspf());
        }
    }

    public static PixelFormat derivePixelFormat(DdsPixelformat pf) {
        DxgiFormat dxgiFormat = pf.deriveDxgiFormat();
        if (dxgiFormat != DxgiFormat.UNKNOWN) {
            return dxgiFormat;
        } else {
            return pf.deriveD3dFormat();
        }
    }

    public static D3dFormat deriveD3dFormat(DdsPixelformat pf) {
        /*
        This method was ported from a C++ implementation given by Microsoft in DirectXTK/DirectXTex and their documentation.

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

        int fcc = pf.dwFourCC();
        int f = pf.dwFlags();
        int c = pf.dwRGBBitCount();
        int r = pf.dwRBitMask();
        int g = pf.dwGBitMask();
        int b = pf.dwBBitMask();
        int a = pf.dwABitMask();
        if ((f & DDPF_RGB) == DDPF_RGB) {
            switch (c) {
                case 32 -> {
                    if (r == 0x00ff0000 && g == 0x0000ff00 && b == 0x000000ff && a == 0xff000000) {
                        return D3dFormat.A8R8G8B8;
                    } else if (r == 0x00ff0000 && g == 0x0000ff00 && b == 0x000000ff && a == 0) {
                        return D3dFormat.X8R8G8B8;
                    } else if (r == 0x000000ff && g == 0x0000ff00 && b == 0x00ff0000 && a == 0xff000000) {
                        return D3dFormat.A8B8G8R8;
                    } else if (r == 0x000000ff && g == 0x0000ff00 && b == 0x00ff0000 && a == 0) {
                        return D3dFormat.X8B8G8R8;
                    } else if (r == 0x000003ff && g == 0x000ffc00 && b == 0x3ff00000 && a == 0xc0000000) {
                        // Note that many common DDS reader/writers (including D3DX) swap the
                        // RED/BLUE masks for 10:10:10:2 formats. We assume
                        // below that the 'backwards' header mask is being used since it is most
                        // likely written by D3DX.

                        // For 'correct' writers this should be 0x3ff00000,0x000ffc00,0x000003ff for BGR data
                        return D3dFormat.A2R10G10B10;
                    } else if (r == 0x3ff00000 && g == 0x000ffc00 && b == 0x000003ff && a == 0xc0000000) {
                        // For 'correct' writers this should be 0x000003ff,0x000ffc00,0x3ff00000 for RGB data
                        return D3dFormat.A2B10G10R10;
                    } else if (r == 0x0000ffff && g == 0xffff0000 && b == 0 && a == 0) {
                        return D3dFormat.G16R16;
                    } else if (r == 0xffffffff && g == 0 && b == 0 && a == 0) {
                        return D3dFormat.R32F; // D3DX writes this out as a FourCC of 114
                    }
                }
                case 24 -> {
                    if (r == 0xff0000 && g == 0x00ff00 && b == 0x0000ff && a == 0) {
                        return D3dFormat.R8G8B8;
                    }
                }
                case 16 -> {
                    if (r == 0xf800 && g == 0x07e0 && b == 0x001f && a == 0) {
                        return D3dFormat.R5G6B5;
                    } else if (r == 0x7c00 && g == 0x03e0 && b == 0x001f && a == 0x8000) {
                        return D3dFormat.A1R5G5B5;
                    } else if (r == 0x7c00 && g == 0x03e0 && b == 0x001f && a == 0) {
                        return D3dFormat.X1R5G5B5;
                    } else if (r == 0x0f00 && g == 0x00f0 && b == 0x000f && a == 0xf000) {
                        return D3dFormat.A4R4G4B4;
                    } else if (r == 0x0f00 && g == 0x00f0 && b == 0x000f && a == 0) {
                        return D3dFormat.X4R4G4B4;
                    } else if (r == 0x00e0 && g == 0x001c && b == 0x0003 && a == 0xff00) {
                        return D3dFormat.A8R3G3B2;
                    } else if (r == 0xffff && g == 0 && b == 0 && a == 0) {
                        // NVTT versions 1.x wrote these as RGB instead of LUMINANCE
                        return D3dFormat.L16;
                    } else if (r == 0x00ff && g == 0 && b == 0 && a == 0xff00) {
                        // ditto
                        return D3dFormat.A8L8;
                    }
                }
                case 8 -> {
                    if (r == 0xe0 && g == 0x1c && b == 0x03 && a == 0) {
                        return D3dFormat.R3G3B2;
                    } else if (r == 0xff && g == 0 && b == 0 && a == 0) {// NVTT versions 1.x wrote these as RGB instead of LUMINANCE
                        return D3dFormat.L8;
                    }

                    // Paletted texture formats are typically not supported on modern video cards aka D3DFMT_P8, D3DFMT_A8P8
                }
            }
        } else if ((f & DDPF_LUMINANCE) == DDPF_LUMINANCE) {
            switch (c) {
                case 16 -> {
                    if (r == 0xffff && g == 0 && b == 0 && a == 0) {
                        return D3dFormat.L16;
                    } else if (r == 0x00ff && g == 0 && b == 0 && a == 0xff00) {
                        return D3dFormat.A8L8;
                    }
                }
                case 8 -> {
                    if (r == 0x0f && g == 0 && b == 0 && a == 0xf0) {
                        return D3dFormat.A4L4;
                    } else if (r == 0xff && g == 0 && b == 0 && a == 0) {
                        return D3dFormat.L8;
                    } else if (r == 0x00ff && g == 0 && b == 0 && a == 0xff00) {
                        return D3dFormat.A8L8; // Some DDS writers assume the bitcount should be 8 instead of 16
                    }
                }
            }
        } else if ((f & DDPF_ALPHA) == DDPF_ALPHA) {
            if (c == 8) {
                return D3dFormat.A8;
            }
        } else if ((f & DDPF_BUMPDUDV) == DDPF_BUMPDUDV) {
            switch (c) {
                case 32 -> {
                    if (r == 0x000000ff && g == 0x0000ff00 && b == 0x00ff0000 && a == 0xff000000) {
                        return D3dFormat.Q8W8V8U8;
                    } else if (r == 0x0000ffff && g == 0xffff0000 && b == 0 && a == 0) {
                        return D3dFormat.V16U16;
                    } else if (r == 0x3ff00000 && g == 0x000ffc00 && b == 0x000003ff && a == 0xc0000000) {
                        return D3dFormat.A2W10V10U10;
                    }
                }
                case 16 -> {
                    if (r == 0x00ff && g == 0xff00 && b == 0 && a == 0) {
                        return D3dFormat.V8U8;
                    }
                }
            }
        } else if ((f & DDPF_BUMPLUMINANCE) == DDPF_BUMPLUMINANCE) {
            switch (c) {
                case 32 -> {
                    if (r == 0x000000ff && g == 0x0000ff00 && b == 0x00ff0000 && a == 0) {
                        return D3dFormat.X8L8V8U8;
                    }
                }
                case 16 -> {
                    if (r == 0x001f && g == 0x03e0 && b == 0xfc00 && a == 0) {
                        return D3dFormat.L6V5U5;
                    }
                }
            }
        } else if ((f & DdsConstants.DDPF_FOURCC) == DdsConstants.DDPF_FOURCC) {
            if (fcc == D3DFMT_DXT1) {
                return D3dFormat.DXT1;
            } else if (fcc == D3DFMT_DXT2) {
                return D3dFormat.DXT2;
            } else if (fcc == D3DFMT_DXT3) {
                return D3dFormat.DXT3;
            } else if (fcc == D3DFMT_DXT4) {
                return D3dFormat.DXT4;
            } else if (fcc == D3DFMT_DXT5) {
                return D3dFormat.DXT5;
            } else if (fcc == D3DFMT_R8G8_B8G8) {
                return D3dFormat.R8G8_B8G8;
            } else if (fcc == D3DFMT_G8R8_G8B8) {
                return D3dFormat.G8R8_G8B8;
            } else if (fcc == D3DFMT_UYVY) {
                return D3dFormat.UYVY;
            } else if (fcc == D3DFMT_YUY2) {
                return D3dFormat.YUY2;
            } else if (fcc == D3dFormat.A16B16G16R16.value()) {
                return D3dFormat.A16B16G16R16;
            } else if (fcc == D3dFormat.Q16W16V16U16.value()) {
                return D3dFormat.Q16W16V16U16;
            } else if (fcc == D3dFormat.R16F.value()) {
                return D3dFormat.R16F;
            } else if (fcc == D3dFormat.G16R16F.value()) {
                return D3dFormat.G16R16F;
            } else if (fcc == D3dFormat.A16B16G16R16F.value()) {
                return D3dFormat.A16B16G16R16F;
            } else if (fcc == D3dFormat.R32F.value()) {
                return D3dFormat.R32F;
            } else if (fcc == D3dFormat.G32R32F.value()) {
                return D3dFormat.G32R32F;
            } else if (fcc == D3dFormat.A32B32G32R32F.value()) {
                return D3dFormat.A32B32G32R32F;
            } else if (fcc == D3dFormat.CxV8U8.value()) {
                return D3dFormat.CxV8U8;
            }
        }

        return D3dFormat.UNKNOWN;
    }

    public static DxgiFormat deriveDxgiFormat(DdsPixelformat pf) {
        /*
        This method was ported from a C++ implementation given by Microsoft in DirectXTK/DirectXTex and their documentation.

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

        int fcc = pf.dwFourCC();
        int f = pf.dwFlags();
        int c = pf.dwRGBBitCount();
        int r = pf.dwRBitMask();
        int g = pf.dwGBitMask();
        int b = pf.dwBBitMask();
        int a = pf.dwABitMask();
        if ((f & DDPF_RGB) == DDPF_RGB) {
            switch (c) {
                case 32 -> {
                    if (r == 0x000000ff && g == 0x0000ff00 && b == 0x00ff0000 && a == 0xff000000) {
                        return DxgiFormat.R8G8B8A8_UNORM;
                    } else if (r == 0x00ff0000 && g == 0x0000ff00 && b == 0x000000ff && a == 0xff000000) {
                        return DxgiFormat.B8G8R8A8_UNORM;
                    } else if (r == 0x00ff0000 && g == 0x0000ff00 && b == 0x000000ff && a == 0) {
                        return DxgiFormat.B8G8R8X8_UNORM;
                    } else if (r == 0x3ff00000 && g == 0x000ffc00 && b == 0x000003ff && a == 0xc0000000) {
                        // Note that many common DDS reader/writers (including D3DX) swap the
                        // RED/BLUE masks for 10:10:10:2 formats. We assume
                        // below that the 'backwards' header mask is being used since it is most
                        // likely written by D3DX. The more robust solution is to use the 'DX10'
                        // header extension and specify the DXGI_FORMAT_R10G10B10A2_UNORM format directly

                        // For 'correct' writers, this should be 0x000003ff,0x000ffc00,0x3ff00000 for RGB data
                        return DxgiFormat.R10G10B10A2_UNORM;
                    } else if (r == 0x0000ffff && g == 0xffff0000 && b == 0 && a == 0) {
                        return DxgiFormat.R16G16_UNORM;
                    } else if (r == 0xffffffff && g == 0 && b == 0 && a == 0) {
                        // Only 32-bit color channel format in D3D9 was R32F
                        return DxgiFormat.R32_FLOAT; // D3DX writes this out as a FourCC of 114
                    }

                    // No DXGI format maps to ISBITMASK(0x000000ff,0x0000ff00,0x00ff0000,0) aka D3DFMT_X8B8G8R8
                    // No DXGI format maps to ISBITMASK(0x000003ff,0x000ffc00,0x3ff00000,0xc0000000) aka D3DFMT_A2R10G10B10
                }
                case 24 -> {
                    // No 24bpp DXGI formats aka D3DFMT_R8G8B8
                }
                case 16 -> {
                    if (r == 0x7c00 && g == 0x03e0 && b == 0x001f && a == 0x8000) {
                        return DxgiFormat.B5G5R5A1_UNORM;
                    } else if (r == 0xf800 && g == 0x07e0 && b == 0x001f && a == 0) {
                        return DxgiFormat.B5G6R5_UNORM;
                    } else if (r == 0x0f00 && g == 0x00f0 && b == 0x000f && a == 0xf000) {
                        return DxgiFormat.B4G4R4A4_UNORM;
                    } else if (r == 0x00ff && g == 0 && b == 0 && a == 0xff00) {
                        // NVTT versions 1.x wrote this as RGB instead of LUMINANCE
                        return DxgiFormat.R8G8_UNORM;
                    } else if (r == 0xffff && g == 0 && b == 0 && a == 0) {
                        // ditto
                        return DxgiFormat.R16_UNORM;
                    }

                    // No DXGI format maps to ISBITMASK(0x7c00,0x03e0,0x001f,0) aka D3DFMT_X1R5G5B5
                    // No DXGI format maps to ISBITMASK(0x0f00,0x00f0,0x000f,0) aka D3DFMT_X4R4G4B4
                    // No 3:3:2:8 or paletted DXGI formats aka D3DFMT_A8R3G3B2, D3DFMT_A8P8, etc.
                }
                case 8 -> {
                    if (r == 0xff && g == 0 && b == 0 && a == 0) {
                        // NVTT versions 1.x wrote this as RGB instead of LUMINANCE
                        return DxgiFormat.R8_UNORM;
                    }

                    // No 3:3:2 or paletted DXGI formats aka D3DFMT_R3G3B2, D3DFMT_P8
                }
            }
        } else if ((f & DDPF_LUMINANCE) == DDPF_LUMINANCE) {
            switch (c) {
                case 16 -> {
                    if (r == 0xffff && g == 0 && b == 0 && a == 0) {
                        return DxgiFormat.R16_UNORM; // D3DX10/11 writes this out as DX10 extension
                    } else if (r == 0x00ff && g == 0 && b == 0 && a == 0xff00) {
                        return DxgiFormat.R8G8_UNORM; // D3DX10/11 writes this out as DX10 extension
                    }
                }
                case 8 -> {
                    if (r == 0xff && g == 0 && b == 0 && a == 0) {
                        return DxgiFormat.R8_UNORM; // D3DX10/11 writes this out as DX10 extension
                    } else if (r == 0x00ff && g == 0 && b == 0 && a == 0xff00) {
                        return DxgiFormat.R8G8_UNORM; // Some DDS writers assume the bitcount should be 8 instead of 16
                    }

                    // No DXGI format maps to ISBITMASK(0x0f,0,0,0xf0) aka D3DFMT_A4L4
                }
            }
        } else if ((f & DDPF_ALPHA) == DDPF_ALPHA) {
            if (c == 8) {
                return DxgiFormat.A8_UNORM;
            }
        } else if ((f & DDPF_BUMPDUDV) == DDPF_BUMPDUDV) {
            switch (c) {
                case 32 -> {
                    if (r == 0x000000ff && g == 0x0000ff00 && b == 0x00ff0000 && a == 0xff000000) {
                        return DxgiFormat.R8G8B8A8_SNORM; // D3DX10/11 writes this out as DX10 extension
                    } else if (r == 0x0000ffff && g == 0xffff0000 && b == 0 && a == 0) {
                        return DxgiFormat.R16G16_SNORM; // D3DX10/11 writes this out as DX10 extension
                    }

                    // No DXGI format maps to ISBITMASK(0x3ff00000, 0x000ffc00, 0x000003ff, 0xc0000000) aka D3DFMT_A2W10V10U10
                }
                case 16 -> {
                    if (r == 0x00ff && g == 0xff00 && b == 0 && a == 0) {
                        return DxgiFormat.R8G8_SNORM; // D3DX10/11 writes this out as DX10 extension
                    }
                }
            }
        } else if ((f & DDPF_BUMPLUMINANCE) == DDPF_BUMPLUMINANCE) {
            // No DXGI format maps to DDPF_BUMPLUMINANCE aka D3DFMT_L6V5U5, D3DFMT_X8L8V8U8
        } else if ((f & DDPF_FOURCC) == DDPF_FOURCC) {
            if (fcc == D3DFMT_DXT1) {
                return DxgiFormat.BC1_UNORM;
            } else if (fcc == D3DFMT_DXT2 || fcc == D3DFMT_DXT3) {
                // While pre-multiplied alpha isn't directly supported by the DXGI formats,
                // they are basically the same as these BC formats so they can be mapped
                return DxgiFormat.BC2_UNORM;
            } else if (fcc == D3DFMT_DXT4 || fcc == D3DFMT_DXT5) {
                // ditto
                return DxgiFormat.BC3_UNORM;
            } else if (fcc == DXGI_FORMAT_BC4_UNORM || fcc == DXGI_FORMAT_BC4_UNORM_ALT) {
                return DxgiFormat.BC4_UNORM;
            } else if (fcc == DXGI_FORMAT_BC4_SNORM) {
                return DxgiFormat.BC4_SNORM;
            } else if (fcc == DXGI_FORMAT_BC5_UNORM || fcc == DXGI_FORMAT_BC5_UNORM_ALT) {
                return DxgiFormat.BC5_UNORM;
            } else if (fcc == DXGI_FORMAT_BC5_SNORM) {
                return DxgiFormat.BC5_SNORM;
                // BC6H and BC7 are written using the "DX10" extended header
            } else if (fcc == D3DFMT_R8G8_B8G8) {
                return DxgiFormat.R8G8_B8G8_UNORM;
            } else if (fcc == D3DFMT_G8R8_G8B8) {
                return DxgiFormat.G8R8_G8B8_UNORM;
            } else if (fcc == D3DFMT_YUY2) {
                return DxgiFormat.YUY2;
            } else if (fcc == D3dFormat.A16B16G16R16.value()) {
                return DxgiFormat.R16G16B16A16_UNORM;
            } else if (fcc == D3dFormat.Q16W16V16U16.value()) {
                return DxgiFormat.R16G16B16A16_SNORM;
            } else if (fcc == D3dFormat.R16F.value()) {
                return DxgiFormat.R16_FLOAT;
            } else if (fcc == D3dFormat.G16R16F.value()) {
                return DxgiFormat.R16G16_FLOAT;
            } else if (fcc == D3dFormat.A16B16G16R16F.value()) {
                return DxgiFormat.R16G16B16A16_FLOAT;
            } else if (fcc == D3dFormat.R32F.value()) {
                return DxgiFormat.R32_FLOAT;
            } else if (fcc == D3dFormat.A32B32G32R32F.value()) {
                return DxgiFormat.R32G32B32A32_FLOAT;
            }

            // No DXGI format maps to D3DFMT_CxV8U8
        }

        return DxgiFormat.UNKNOWN;
    }
}
