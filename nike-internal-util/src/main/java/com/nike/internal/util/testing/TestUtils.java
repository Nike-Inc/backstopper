package com.nike.internal.util.testing;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Contains static helper methods useful during testing.
 */
public class TestUtils {

    // Private constructor - use the static helper methods for everything in this class.
    private TestUtils() {
        // Do nothing
    }

    /**
     * Finds an unused port on the machine hosting the currently running JVM.
     *
     * Does not throw any checked {@link IOException} that occurs while trying to find a free port. If one occurs,
     * it will be wrapped in a {@link RuntimeException}.
     */
    public static int findFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setReuseAddress(true);
            return serverSocket.getLocalPort();
        }
        catch (IOException e) {
            throw new RuntimeException("Error while trying to find a free port.", e);
        }
    }
}
