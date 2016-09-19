package com.nike.backstopper.exception.network;

import com.nike.internal.util.MapBuilder;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the functionality of {@link ServerUnknownHttpStatusCodeException}.
 *
 * @author Nic Munroe
 */
public class ServerUnknownHttpStatusCodeExceptionTest {

    @Test
    public void constructor_sets_values_as_expected() {
        // given
        Throwable cause = new RuntimeException("cause");
        String connectionType = "foo_conn";
        Throwable details = new RuntimeException("details");
        int responseStatusCode = 500;
        Map<String, List<String>> responseHeaders = MapBuilder
            .builder("foo", singletonList(UUID.randomUUID().toString()))
            .put("bar", Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
            .build();
        String rawResponseBody = UUID.randomUUID().toString();

        // when
        ServerUnknownHttpStatusCodeException ex = new ServerUnknownHttpStatusCodeException(
            cause, connectionType, details, responseStatusCode, responseHeaders, rawResponseBody
        );

        // then
        assertThat(ex).hasCause(cause);
        assertThat(ex.getConnectionType()).isEqualTo(connectionType);
        assertThat(ex.getDetails()).isEqualTo(details);
        assertThat(ex.getResponseStatusCode()).isEqualTo(responseStatusCode);
        assertThat(ex.getResponseHeaders()).isEqualTo(responseHeaders);
        assertThat(ex.getRawResponseBody()).isEqualTo(rawResponseBody);
    }

}