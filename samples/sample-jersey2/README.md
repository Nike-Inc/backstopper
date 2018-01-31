# Backstopper Sample Application - jersey2

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This submodule contains a sample application based on Jersey 2 that fully integrates Backstopper.
 
* Build the sample by running the `./buildSample.sh` script.
* Launch the sample by running the `./runSample.sh` script. It will bind to port 8080 by default. 
    * You can override the default port by passing in a system property to the run script, e.g. to bind to port 8181: `./runSample.sh -Djersey2Sample.server.port=8181`
 
## Things to try
 
All examples here assume the sample app is running on port 8080, so you would hit each path by going to `http://localhost:8080/[endpoint-path]`. It's recommended that you use a REST client like [Postman](https://www.getpostman.com/) for making the requests so you can easily specify HTTP method, payloads, headers, etc, and fully inspect the response.

Also note that all the following things to try are verified in a component test: `VerifyExpectedErrorsAreReturnedComponentTest`. If you prefer to experiment via code you can run, debug, and otherwise explore that test. 

As you are doing the following you should check the logs that are output by the sample application and notice what is included in the log messages. In particular notice how you can search for an `error_id` that came from an error response and go directly to the relevant log message in the logs. Also notice how the `ApiError.getName()` value shows up in the logs for each error represented in a returned error contract (there can be more than one per request).
 
* `GET /sample` - Returns the JSON serialization for the `SampleModel` model object. You can copy this into a `POST` call to experiment with triggering errors.
* `POST /sample` with `ContentType: application/json` header - Using the JSON model retrieved by the `GET` call, you can trigger numerous different types of errors, all of which get caught by the Backstopper system and converted into the appropriate error contract.
    * Omit the `foo` field.
    * Set the value of the `range_0_to_42` field to something outside of the allowed 0-42 range.
    * Set the value of the `rgb_color` field to something besides `RED`, `GREEN`, or `BLUE`, or omit it entirely. Note that the validation and deserialization of this enum field is done in a case insensitive manner - i.e. you can pass `red`, `Green`, or `bLuE` if you want and it will not throw an error.
    * Set two or more invalid values for `foo`, `range_0_to_42`, and `rgb_color` to invalid values all at once - notice you get back all relevant errors at once in the same error contract.
    * Set `throw_manual_error` to true to trigger a manual exception to be thrown inside the normal `POST /sample` endpoint.
        * Note the extra response headers that are included when you do this, and how they relate to the `.withExtraResponseHeaders(...)` method call on the builder of the exception that is thrown.
    * Pass in an empty JSON payload - you should receive a `"Missing expected content"` error back.
    * Pass in a junk payload that is not valid JSON - you should receive a `"Malformed request"` error back.
* `GET /sample/coreErrorWrapper` - Triggers an error to be thrown that appears to the caller like a normal generic service exception, but the `SOME_MEANINGFUL_ERROR_NAME` name from the `ApiError` it represents shows up in the logs to help you disambiguate what the true cause was.
* `GET /sample/triggerUnhandledError` - Triggers an error that is caught by the unhandled exception handler portion of Backstopper and converted to a generic service exception.
* `GET /sample/throwExceptionFromAsyncEndpoint` - Hits an async endpoint (using Jersey's `@Suspended AsyncResponse` 
feature) that throws an exception (shows that Backstopper handles async endpoints that throw exceptions).
* `GET /sample/resumeAsyncResponseWithException` - Hits an async endpoint (using Jersey's `@Suspended AsyncResponse` 
feature) that resumes the async response via `AsyncResponse.resume(Throwable)` (shows that Backstopper handles async 
endpoints that explicitly resume the response with an exception from another thread).
* `GET /does-not-exist` - Triggers a framework 404 which Backstopper handles.
* `DELETE /sample` - Triggers a framework 405 which Backstopper handles.   
* `GET /sample` with `Accept: application/octet-stream` header - Triggers a framework 406 which Backstopper handles.
* `POST /sample` with `ContentType: text/plain` - Triggers a framework 415 which Backstopper handles.

## More Info

See the [base project README.md](../../README.md), [User Guide](../../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
