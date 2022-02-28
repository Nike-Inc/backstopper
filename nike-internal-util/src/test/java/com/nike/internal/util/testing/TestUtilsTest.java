package com.nike.internal.util.testing;

import org.junit.Test;

import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests the functionality of {@link TestUtils}.
 */
public class TestUtilsTest {

    @Test
    public void findFreePort_finds_an_open_port() {
        // given
        int freePort = TestUtils.findFreePort();

        // when
        Throwable ex = catchThrowable(() -> {
            ServerSocket serverSocket = new ServerSocket(freePort);
            serverSocket.close();
        });

        // then
        assertThat(ex).isNull();
    }
}