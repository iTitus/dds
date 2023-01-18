package io.github.ititus.ddsfx;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DdsDescriptorTest {

    @Test
    void testFormatName() {
        assertThat(DdsDescriptor.getInstance().getFormatName()).isEqualTo("DDS");
    }
}