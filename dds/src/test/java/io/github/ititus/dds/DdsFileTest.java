package io.github.ititus.dds;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdsFileTest {

    @Test
    void testMissingMagic() {
        ByteArrayInputStream bis = new ByteArrayInputStream(new byte[] {'D', 'A', 'S', ' '});
        assertThatThrownBy(() -> DdsFile.load(bis)).isInstanceOf(IOException.class);
    }
}
