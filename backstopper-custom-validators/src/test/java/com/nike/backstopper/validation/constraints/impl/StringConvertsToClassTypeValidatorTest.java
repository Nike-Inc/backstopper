package com.nike.backstopper.validation.constraints.impl;

import com.nike.backstopper.validation.constraints.StringConvertsToClassType;
import com.nike.internal.util.testing.Glassbox;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies the functionality of {@link StringConvertsToClassTypeValidator}
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class StringConvertsToClassTypeValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private StringConvertsToClassTypeValidator validatorImpl;

    private enum RgbColors {
        RED, Green, blue
    }

    private static class CorrectAnnotationPlacement {
        @StringConvertsToClassType(classType = RgbColors.class, allowCaseInsensitiveEnumMatch = true)
        public String fooColorEnumCaseInsensitive;
        @StringConvertsToClassType(classType = RgbColors.class)
        public String fooColorEnumCaseSensitive;
        @StringConvertsToClassType(classType = Byte.class)
        public String fooByteBoxed;
        @StringConvertsToClassType(classType = byte.class)
        public String fooByte;
        @StringConvertsToClassType(classType = Short.class)
        public String fooShortBoxed;
        @StringConvertsToClassType(classType = short.class)
        public String fooShort;
        @StringConvertsToClassType(classType = Integer.class)
        public String fooIntegerBoxed;
        @StringConvertsToClassType(classType = int.class)
        public String fooInt;
        @StringConvertsToClassType(classType = Long.class)
        public String fooLongBoxed;
        @StringConvertsToClassType(classType = long.class)
        public String fooLong;
        @StringConvertsToClassType(classType = Float.class)
        public String fooFloatBoxed;
        @StringConvertsToClassType(classType = float.class)
        public String fooFloat;
        @StringConvertsToClassType(classType = Double.class)
        public String fooDoubleBoxed;
        @StringConvertsToClassType(classType = double.class)
        public String fooDouble;
        @StringConvertsToClassType(classType = Boolean.class)
        public String fooBooleanBoxed;
        @StringConvertsToClassType(classType = boolean.class)
        public String fooBoolean;
        @StringConvertsToClassType(classType = Character.class)
        public String fooCharacterBoxed;
        @StringConvertsToClassType(classType = char.class)
        public String fooCharacter;
        @StringConvertsToClassType(classType = String.class)
        public String fooString;
        @StringConvertsToClassType(classType = Object.class)
        public String fooObject;

        private CorrectAnnotationPlacement withFooColorEnumCaseInsensitive(String fooColorEnum) {
            this.fooColorEnumCaseInsensitive = fooColorEnum;
            return this;
        }
        private CorrectAnnotationPlacement withFooColorEnumCaseSensitive(String fooColorEnum) {
            this.fooColorEnumCaseSensitive = fooColorEnum;
            return this;
        }
        private CorrectAnnotationPlacement withFooByteBoxed(String fooByteBoxed) {
            this.fooByteBoxed = fooByteBoxed;
            return this;
        }
        private CorrectAnnotationPlacement withFooByte(String fooByte) {
            this.fooByte = fooByte;
            return this;
        }
        private CorrectAnnotationPlacement withFooShortBoxed(String fooShortBoxed) {
            this.fooShortBoxed = fooShortBoxed;
            return this;
        }
        private CorrectAnnotationPlacement withFooShort(String fooShort) {
            this.fooShort = fooShort;
            return this;
        }
        private CorrectAnnotationPlacement withFooIntegerBoxed(String fooIntegerBoxed) {
            this.fooIntegerBoxed = fooIntegerBoxed;
            return this;
        }
        private CorrectAnnotationPlacement withFooInt(String fooInt) {
            this.fooInt = fooInt;
            return this;
        }
        private CorrectAnnotationPlacement withFooLongBoxed(String fooLongBoxed) {
            this.fooLongBoxed = fooLongBoxed;
            return this;
        }
        private CorrectAnnotationPlacement withFooLong(String fooLong) {
            this.fooLong = fooLong;
            return this;
        }
        private CorrectAnnotationPlacement withFooFloatBoxed(String fooFloatBoxed) {
            this.fooFloatBoxed = fooFloatBoxed;
            return this;
        }
        private CorrectAnnotationPlacement withFooFloat(String fooFloat) {
            this.fooFloat = fooFloat;
            return this;
        }
        private CorrectAnnotationPlacement withFooDoubleBoxed(String fooDoubleBoxed) {
            this.fooDoubleBoxed = fooDoubleBoxed;
            return this;
        }
        private CorrectAnnotationPlacement withFooDouble(String fooDouble) {
            this.fooDouble = fooDouble;
            return this;
        }
        private CorrectAnnotationPlacement withFooBooleanBoxed(String fooBooleanBoxed) {
            this.fooBooleanBoxed = fooBooleanBoxed;
            return this;
        }
        private CorrectAnnotationPlacement withFooBoolean(String fooBoolean) {
            this.fooBoolean = fooBoolean;
            return this;
        }
        private CorrectAnnotationPlacement withFooCharacterBoxed(String fooCharacterBoxed) {
            this.fooCharacterBoxed = fooCharacterBoxed;
            return this;
        }
        private CorrectAnnotationPlacement withFooCharacter(String fooCharacter) {
            this.fooCharacter = fooCharacter;
            return this;
        }
        private CorrectAnnotationPlacement withFooString(String fooString) {
            this.fooString = fooString;
            return this;
        }
        private CorrectAnnotationPlacement withFooObject(String fooObject) {
            this.fooObject = fooObject;
            return this;
        }
    }

    protected CorrectAnnotationPlacement newObj() {
        return new CorrectAnnotationPlacement();
    }
    
    @Before
    public void setupMethod() {
        validatorImpl = new StringConvertsToClassTypeValidator();
    }

    protected void doValidationTest(CorrectAnnotationPlacement testMe, String value, Class<?> desiredClass, boolean expectedResult) {
        doValidationTest(testMe, value, desiredClass, false, expectedResult);
    }

    protected void doValidationTest(CorrectAnnotationPlacement testMe, String value, Class<?> desiredClass, boolean allowCaseInsensitiveEnumMatch, boolean expectedResult) {
        Glassbox.setInternalState(validatorImpl, "desiredClass", desiredClass);
        Glassbox.setInternalState(validatorImpl, "allowCaseInsensitiveEnumMatch", allowCaseInsensitiveEnumMatch);
        boolean directValidationResult = validatorImpl.isValid(value, null);
        Set<ConstraintViolation<CorrectAnnotationPlacement>> validatorResult = validator.validate(testMe);

        assertThat(directValidationResult, is(expectedResult));
        assertThat(validatorResult.isEmpty(), is(expectedResult));
    }

    @DataProvider(value = {
        "RED        |   true    |   true",
        "Red        |   true    |   false",
        "red        |   true    |   false",
        "RED_BAD    |   false   |   false",
        "GREEN      |   true    |   false",
        "Green      |   true    |   true",
        "green      |   true    |   false",
        "GreenFOO   |   false   |   false",
        "BLUE       |   true    |   false",
        "Blue       |   true    |   false",
        "blue       |   true    |   true",
        "NOTblue    |   false   |   false"
    }, splitBy = "\\|")
    @Test
    public void shouldValidateValidColorEnumCorrectly(String colorAsString, boolean expectValidCaseInsensitive, boolean expectValidCaseSensitive) {
        doValidationTest(newObj().withFooColorEnumCaseInsensitive(colorAsString), colorAsString, RgbColors.class, true, expectValidCaseInsensitive);
        doValidationTest(newObj().withFooColorEnumCaseSensitive(colorAsString), colorAsString, RgbColors.class, false, expectValidCaseSensitive);
    }

    // Byte (boxed) and byte (primitive) ===============================================================
    @Test
    public void shouldValidateValidByteBoxed() {
        doValidationTest(newObj().withFooByteBoxed("3"), "3", Byte.class, true);
    }

    @Test
    public void shouldNotValidateInvalidByteBoxed() {
        doValidationTest(newObj().withFooByteBoxed("asdf"), "asdf", Byte.class, false);
    }

    @Test
    public void shouldNotValidateByteBoxedForValueTooBigForByte() {
        doValidationTest(newObj().withFooByteBoxed(String.valueOf(Integer.MAX_VALUE)), String.valueOf(Integer.MAX_VALUE), Byte.class, false);
    }

    @Test
    public void shouldValidateValidByte() {
        doValidationTest(newObj().withFooByte("4"), "4", byte.class, true);
    }

    @Test
    public void shouldNotValidateInvalidByte() {
        doValidationTest(newObj().withFooByte("asdf"), "asdf", byte.class, false);
    }

    @Test
    public void shouldNotValidateByteForValueTooBigForByte() {
        doValidationTest(newObj().withFooByte(String.valueOf(Integer.MAX_VALUE)), String.valueOf(Integer.MAX_VALUE), byte.class, false);
    }

    // Short (boxed) and short (primitive) ===============================================================
    @Test
    public void shouldValidateValidShortBoxed() {
        doValidationTest(newObj().withFooShortBoxed("9"), "9", Short.class, true);
    }

    @Test
    public void shouldNotValidateInvalidShortBoxed() {
        doValidationTest(newObj().withFooShortBoxed("asdf"), "asdf", Short.class, false);
    }

    @Test
    public void shouldNotValidateShortBoxedForValueTooBigForShort() {
        doValidationTest(newObj().withFooShortBoxed(String.valueOf(Integer.MAX_VALUE)), String.valueOf(Integer.MAX_VALUE), Short.class, false);
    }

    @Test
    public void shouldValidateValidShort() {
        doValidationTest(newObj().withFooShort("9"), "9", short.class, true);
    }

    @Test
    public void shouldNotValidateInvalidShort() {
        doValidationTest(newObj().withFooShort("asdf"), "asdf", short.class, false);
    }

    @Test
    public void shouldNotValidateShortForValueTooBigForShort() {
        doValidationTest(newObj().withFooShort(String.valueOf(Integer.MAX_VALUE)), String.valueOf(Integer.MAX_VALUE), short.class, false);
    }

    // Integer (boxed) and int (primitive) ===============================================================
    @Test
    public void shouldValidateValidIntegerBoxed() {
        doValidationTest(newObj().withFooIntegerBoxed("22"), "22", Integer.class, true);
    }

    @Test
    public void shouldNotValidateInvalidIntegerBoxed() {
        doValidationTest(newObj().withFooIntegerBoxed("asdf"), "asdf", Integer.class, false);
    }

    @Test
    public void shouldNotValidateIntegerBoxedForValueTooBigForInteger() {
        doValidationTest(newObj().withFooIntegerBoxed(String.valueOf(Long.MAX_VALUE)), String.valueOf(Long.MAX_VALUE), Integer.class, false);
    }

    @Test
    public void shouldValidateValidInteger() {
        doValidationTest(newObj().withFooInt("22"), "22", int.class, true);
    }

    @Test
    public void shouldNotValidateInvalidInteger() {
        doValidationTest(newObj().withFooInt("asdf"), "asdf", int.class, false);
    }

    @Test
    public void shouldNotValidateIntegerForValueTooBigForInteger() {
        doValidationTest(newObj().withFooInt(String.valueOf(Long.MAX_VALUE)), String.valueOf(Long.MAX_VALUE), int.class, false);
    }

    // Long (boxed) and long (primitive) ===============================================================
    @Test
    public void shouldValidateValidLongBoxed() {
        doValidationTest(newObj().withFooLongBoxed("444"), "444", Long.class, true);
    }

    @Test
    public void shouldNotValidateInvalidLongBoxed() {
        doValidationTest(newObj().withFooLongBoxed("asdf"), "asdf", Long.class, false);
    }

    @Test
    public void shouldNotValidateLongBoxedForValueTooBigForLong() {
        doValidationTest(newObj().withFooLongBoxed("9" + Long.MAX_VALUE), "9" + Long.MAX_VALUE, Long.class, false);
    }

    @Test
    public void shouldValidateValidLong() {
        doValidationTest(newObj().withFooLong("444"), "444", long.class, true);
    }

    @Test
    public void shouldNotValidateInvalidLong() {
        doValidationTest(newObj().withFooLong("asdf"), "asdf", long.class, false);
    }

    @Test
    public void shouldNotValidateLongForValueTooBigForLong() {
        doValidationTest(newObj().withFooLong("9" + Long.MAX_VALUE), "9" + Long.MAX_VALUE, long.class, false);
    }

    // Float (boxed) and float (primitive) ===============================================================
    @Test
    public void shouldValidateValidFloatBoxed() {
        doValidationTest(newObj().withFooFloatBoxed("1.1"), "1.1", Float.class, true);
    }

    @Test
    public void shouldNotValidateInvalidFloatBoxed() {
        doValidationTest(newObj().withFooFloatBoxed("asdf"), "asdf", Float.class, false);
    }

    @Test
    public void shouldNotValidateInvalidNaNFloatBoxed() {
        doValidationTest(newObj().withFooFloatBoxed("NaN"), "NaN", Float.class, false);
    }

    @Test
    public void shouldNotValidateFloatBoxedForValueTooBigForFloat() {
        doValidationTest(newObj().withFooFloatBoxed(String.valueOf(Double.MAX_VALUE)), String.valueOf(Double.MAX_VALUE), Float.class, false);
    }

    @Test
    public void shouldValidateValidFloat() {
        doValidationTest(newObj().withFooFloat("1.1"), "1.1", float.class, true);
    }

    @Test
    public void shouldNotValidateInvalidFloat() {
        doValidationTest(newObj().withFooFloat("asdf"), "asdf", float.class, false);
    }

    @Test
    public void shouldNotValidateInvalidNaNFloat() {
        doValidationTest(newObj().withFooFloat("NaN"), "NaN", float.class, false);
    }

    @Test
    public void shouldNotValidateFloatForValueTooBigForFloat() {
        doValidationTest(newObj().withFooFloat(String.valueOf(Double.MAX_VALUE)), String.valueOf(Double.MAX_VALUE), float.class, false);
    }

    // Double (boxed) and double (primitive) ===============================================================
    @Test
    public void shouldValidateValidDoubleBoxed() {
        doValidationTest(newObj().withFooDoubleBoxed("2.2"), "2.2", Double.class, true);
    }

    @Test
    public void shouldNotValidateInvalidDoubleBoxed() {
        doValidationTest(newObj().withFooDoubleBoxed("asdf"), "asdf", Double.class, false);
    }

    @Test
    public void shouldNotValidateInvalidNanDoubleBoxed() {
        doValidationTest(newObj().withFooDoubleBoxed("NaN"), "NaN", Double.class, false);
    }

    @Test
    public void shouldNotValidateDoubleBoxedForValueTooBigForDouble() {
        doValidationTest(newObj().withFooDoubleBoxed("1" + Double.MAX_VALUE), "1" + Double.MAX_VALUE, Double.class, false);
    }

    @Test
    public void shouldValidateValidDouble() {
        doValidationTest(newObj().withFooDouble("2.2"), "2.2", double.class, true);
    }

    @Test
    public void shouldNotValidateInvalidDouble() {
        doValidationTest(newObj().withFooDouble("asdf"), "asdf", double.class, false);
    }

    @Test
    public void shouldNotValidateInvalidNaNDouble() {
        doValidationTest(newObj().withFooDouble("NaN"), "NaN", double.class, false);
    }

    @Test
    public void shouldNotValidateDoubleForValueTooBigForDouble() {
        doValidationTest(newObj().withFooDouble("1" + Double.MAX_VALUE), "1" + Double.MAX_VALUE, double.class, false);
    }

    // Boolean (boxed) and boolean (primitive) ===============================================================
    @Test
    public void shouldValidateValidBooleanBoxed() {
        doValidationTest(newObj().withFooBooleanBoxed("true"), "true", Boolean.class, true);
        doValidationTest(newObj().withFooBooleanBoxed("tRuE"), "tRuE", Boolean.class, true);
        doValidationTest(newObj().withFooBooleanBoxed("false"), "false", Boolean.class, true);
        doValidationTest(newObj().withFooBooleanBoxed("FALSE"), "FALSE", Boolean.class, true);
    }

    @Test
    public void shouldNotValidateInvalidBooleanBoxed() {
        doValidationTest(newObj().withFooBooleanBoxed("asdf"), "asdf", Boolean.class, false);
        doValidationTest(newObj().withFooBooleanBoxed(""), "", Boolean.class, false);
    }

    @Test
    public void shouldValidateValidBoolean() {
        doValidationTest(newObj().withFooBoolean("true"), "true", boolean.class, true);
        doValidationTest(newObj().withFooBoolean("tRuE"), "tRuE", boolean.class, true);
        doValidationTest(newObj().withFooBoolean("false"), "false", boolean.class, true);
        doValidationTest(newObj().withFooBoolean("FALSE"), "FALSE", boolean.class, true);
    }

    @Test
    public void shouldNotValidateInvalidBoolean() {
        doValidationTest(newObj().withFooBoolean("asdf"), "asdf", boolean.class, false);
        doValidationTest(newObj().withFooBoolean(""), "", boolean.class, false);
    }

    // Character (boxed) and char (primitive) ===============================================================
    @Test
    public void shouldValidateValidCharacterBoxed() {
        doValidationTest(newObj().withFooCharacterBoxed("a"), "a", Character.class, true);
        doValidationTest(newObj().withFooCharacterBoxed("Z"), "Z", Character.class, true);
        doValidationTest(newObj().withFooCharacterBoxed("5"), "5", Character.class, true);
        doValidationTest(newObj().withFooCharacterBoxed("\\"), "\\", Character.class, true);
        doValidationTest(newObj().withFooCharacterBoxed("\t"), "\t", Character.class, true);
    }

    @Test
    public void shouldNotValidateInvalidCharacterBoxed() {
        doValidationTest(newObj().withFooCharacterBoxed("aa"), "aa", Character.class, false);
        doValidationTest(newObj().withFooCharacterBoxed(""), "", Character.class, false);
    }

    @Test
    public void shouldValidateValidCharacter() {
        doValidationTest(newObj().withFooCharacter("a"), "a", char.class, true);
        doValidationTest(newObj().withFooCharacter("Z"), "Z", char.class, true);
        doValidationTest(newObj().withFooCharacter("5"), "5", char.class, true);
        doValidationTest(newObj().withFooCharacter("\\"), "\\", char.class, true);
        doValidationTest(newObj().withFooCharacter("\t"), "\t", char.class, true);
    }

    @Test
    public void shouldNotValidateInvalidCharacter() {
        doValidationTest(newObj().withFooCharacter("aa"), "aa", char.class, false);
        doValidationTest(newObj().withFooCharacter(""), "", char.class, false);
    }

    @Test
    public void shouldValidateNonEmptyString() {
        doValidationTest(newObj().withFooString("garbagioQUWIYERQOPIUR"), "garbagioQUWIYERQOPIUR", String.class, true);
    }

    @Test
    public void shouldValidateEmptyString() {
        doValidationTest(newObj().withFooString(""), "", String.class, true);
    }

    @Test
    public void shouldNotValidateNonCoveredClassType() {
        doValidationTest(newObj().withFooObject("5"), "5", Object.class, false);
    }

    // Exceptional case tests =======================================================================
    @Test
    public void validateAsBooleanShouldReturnFalseIfExceptionIsThrown() {
        assertThat(validatorImpl.validateAsBoolean(null), is(false));
    }

    @Test
    public void validateAsCharShouldReturnFalseIfExceptionIsThrown() {
        assertThat(validatorImpl.validateAsChar(null), is(false));
    }
}
