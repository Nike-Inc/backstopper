/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.backstopper.handler.spring.webflux;

import org.springframework.core.NestedExceptionUtils;

import java.util.Set;

/**
 * Copied from spring-web-6.1.12. We need some of the same logic in Backstopper.
 * Modified slightly from the original to make the methods static, get rid of the logging and methods we don't need, etc.
 */
class DisconnectedClientHelper {

	private static final Set<String> EXCEPTION_PHRASES =
			Set.of("broken pipe", "connection reset");

	private static final Set<String> EXCEPTION_TYPE_NAMES =
			Set.of("AbortedException", "ClientAbortException",
					"EOFException", "EofException", "AsyncRequestNotUsableException");

	/**
	 * Whether the given exception indicates the client has gone away.
	 * <p>Known cases covered:
	 * <ul>
	 * <li>ClientAbortException or EOFException for Tomcat
	 * <li>EofException for Jetty
	 * <li>IOException "Broken pipe" or "connection reset by peer"
	 * <li>SocketException "Connection reset"
	 * </ul>
	 */
	public static boolean isClientDisconnectedException(Throwable ex) {
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		if (message != null) {
			String text = message.toLowerCase();
			for (String phrase : EXCEPTION_PHRASES) {
				if (text.contains(phrase)) {
					return true;
				}
			}
		}
		return EXCEPTION_TYPE_NAMES.contains(ex.getClass().getSimpleName());
	}

}
