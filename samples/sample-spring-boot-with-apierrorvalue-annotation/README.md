# Backstopper Sample Application - apierrorvalue-annotation

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This submodule contains a sample application based on Spring Boot 1 that fully integrates Backstopper.
 
* Build the sample by running the `./buildSample.sh` script.
* Launch the sample by running the `./runSample.sh` script. It will bind to port 8080 by default. 
    * You can override the default port by passing in a system property to the run script, 
    e.g. to bind to port 8181: `./runSample.sh -Dserver.port=8181`

## @ApiErrorValue annotation overview

> currently supported Spring 4/Spring Boot 1.x/Spring 2.x

Simple model class showing JSR 303 Bean Validation integration in Backstopper using `@ApiErrorValue`,
that provides the ability to autoconfigure `ProjectApiErrors` with `ApiError`s.         
Can also be used with already existing `ProjectApiErrors` and `ApiError`s.      

```java
public class SampleModel {

     @ApiErrorValue
     @NotBlank
     public String foo;

     @ApiErrorValue(errorCode = "BLANK_BAR", httpStatusCode = 400)
     @NotBlank(message = "bar should not be blank")
     public String bar;
     // -- SNIP -- 
}
```

bad request's response will be as follows:

```json
{
   "error_id": "c6bbec89-c39a-4164-9155-fd8e1c1a5cbc",
   "errors": [
     {
       "code": "INVALID_VALUE",
       "message": "may not be empty",
       "metadata": {
         "field": "foo"
       }
     },
     {
       "code": "BLANK_BAR",
       "message": "bar should not be blank",
       "metadata": {
         "field": "bar"
       }
     }
   ]
}
``` 

## More Info

See the [base project README.md](../../README.md), [User Guide](../../USER_GUIDE.md), and Backstopper repository 
source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
