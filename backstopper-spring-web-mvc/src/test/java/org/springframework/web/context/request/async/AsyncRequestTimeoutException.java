package org.springframework.web.context.request.async;

/**
 * A copy of Spring's {@code AsyncRequestTimeoutException} from the same package. Used during testing to trigger
 * code branches that require an exception with this fully qualified classname.
 *
 * @author Nic Munroe
 */
public class AsyncRequestTimeoutException extends RuntimeException {

}
