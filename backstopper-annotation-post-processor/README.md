# Backstopper - annotation-post-processor

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

The annotation Processor that writes metadata file for `@ApiErrorValue` annotation including enclosed JSR 303 constraint annotations or a valid constraint annotation such as Hibernate/custom.        
 
## @ApiErrorValue annotation overview

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

the response to an empty request will be as follows:

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

> See `@ApiErrorValue` javaDoc for further information.

## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
