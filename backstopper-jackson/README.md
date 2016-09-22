# Backstopper - jackson

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This library contains some helper classes for working in an environment that uses Jackson with Backstopper.
 
## Helper classes
 
* **`JsonUtilWithDefaultErrorContractDTOSupport`** - A general-purpose JSON serializer that has built-in support for the default Backstopper error contract DTO. In particular:
    * If an error code is parseable to an integer then the JSON field for that error code will be serialized as a number rather than a string.
    * If an error has an empty metadata section then it will be omitted from the serialized JSON.
    * You can create a Jackson `ObjectMapper` with any combination of the above rules turned on or off by using the `generateErrorContractObjectMapper(...)` static factory method.

## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
