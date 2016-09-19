# Backstopper - custom-validators

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This library contains JSR 303 Bean Validation annotations that have proven to be useful and reusable. These are entirely optional. They are also largely dependency free so this library is usable in non-Backstopper projects that utilize JSR 303 validations. 
 
## Custom JSR 303 Validation Constraints
 
* **`StringConvertsToClassType`** - Validates that the annotated element (of type String) can be converted to the desired `classType`. The `classType` can be any of the following:
    * Any boxed primitive class type (e.g. `Integer.class`).
    * Any raw primitive class type (e.g. `int.class`).
    * `String.class` - a String can always be converted to a String, so this validator will always return true in this case.
    * Any enum class type - validation is done by comparing the string value to `Enum.name()`. The value of the `allowCaseInsensitiveEnumMatch()` constraint property determines if the validation is done in a case sensitive or case insensitive manner.
    * `null` is always considered valid - if you need to enforce non-null then you should place an additional `@NotNull` constraint on the field as well.
    * More information and usage instructions can be found in the javadocs for `StringConvertsToClassType`, but here's an example showing how you would mark a model field that you wanted to guarantee was convertible to a `RgbColor` enum after passing JSR 303 validation:
    
``` java
@StringConvertsToClassType(
    message = "NOT_RGB_COLOR_ENUM", classType = RgbColor.class, allowCaseInsensitiveEnumMatch = true
)
public final String rgb_color;
```

## More Info

See the [base project README.md](../README.md) and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
