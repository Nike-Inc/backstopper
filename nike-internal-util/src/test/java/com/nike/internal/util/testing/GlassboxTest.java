package com.nike.internal.util.testing;

import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests the functionality of {@link Glassbox}.
 */
public class GlassboxTest {

    private static class SomeObject {
        public final String pubFinalFoo;
        private final String privFinalFoo;
        public final String pubFoo;
        private final String privFoo;

        private SomeObject(String pubFinalFoo, String privFinalFoo, String pubFoo, String privFoo) {
            this.pubFinalFoo = pubFinalFoo;
            this.privFinalFoo = privFinalFoo;
            this.pubFoo = pubFoo;
            this.privFoo = privFoo;
        }

        public String getPrivFinalFoo() {
            return privFinalFoo;
        }

        public String getPrivFoo() {
            return privFoo;
        }
    }

    private static class SomeObjectExtension extends SomeObject {
        public final String pubFinalBar;

        private SomeObjectExtension(
            String pubFinalBar,
            String pubFinalFoo,
            String privFinalFoo,
            String pubFoo,
            String privFoo
        ) {
            super(pubFinalFoo, privFinalFoo, pubFoo, privFoo);
            this.pubFinalBar = pubFinalBar;
        }
    }

    @Test
    public void getInternalState_works_as_expected() {
        // given
        String expectedPubFinalVal = UUID.randomUUID().toString();
        String expectedPrivFinalVal = UUID.randomUUID().toString();
        String expectedPubVal = UUID.randomUUID().toString();
        String expectedPrivVal = UUID.randomUUID().toString();
        SomeObject someObj = new SomeObject(expectedPubFinalVal, expectedPrivFinalVal, expectedPubVal, expectedPrivVal);

        // when
        String actualPubFinal = (String)Glassbox.getInternalState(someObj, "pubFinalFoo");
        String actualPrivFinal = (String)Glassbox.getInternalState(someObj, "privFinalFoo");
        String actualPub = (String)Glassbox.getInternalState(someObj, "pubFoo");
        String actualPriv = (String)Glassbox.getInternalState(someObj, "privFoo");

        // then
        assertThat(actualPubFinal).isEqualTo(expectedPubFinalVal);
        assertThat(actualPrivFinal).isEqualTo(expectedPrivFinalVal);
        assertThat(actualPub).isEqualTo(expectedPubVal);
        assertThat(actualPriv).isEqualTo(expectedPrivVal);
    }

    @Test
    public void getInternalState_works_as_expected_for_subclass() {
        // given
        String expectedBarPubFinal = UUID.randomUUID().toString();
        String expectedFooPubFinal = UUID.randomUUID().toString();
        String expectedFooPrivFinal = UUID.randomUUID().toString();
        String expectedFooPubVal = UUID.randomUUID().toString();
        String expectedFooPrivVal = UUID.randomUUID().toString();
        SomeObjectExtension someObj = new SomeObjectExtension(
            expectedBarPubFinal, expectedFooPubFinal, expectedFooPrivFinal, expectedFooPubVal, expectedFooPrivVal
        );

        // when
        String actualBarPubFinal = (String)Glassbox.getInternalState(someObj, "pubFinalBar");
        String actualFooPubFinal = (String)Glassbox.getInternalState(someObj, "pubFinalFoo");
        String actualFooPrivFinal = (String)Glassbox.getInternalState(someObj, "privFinalFoo");
        String actualFooPub = (String)Glassbox.getInternalState(someObj, "pubFoo");
        String actualFooPriv = (String)Glassbox.getInternalState(someObj, "privFoo");

        // then
        assertThat(actualBarPubFinal).isEqualTo(expectedBarPubFinal);
        assertThat(actualFooPubFinal).isEqualTo(expectedFooPubFinal);
        assertThat(actualFooPrivFinal).isEqualTo(expectedFooPrivFinal);
        assertThat(actualFooPub).isEqualTo(expectedFooPubVal);
        assertThat(actualFooPriv).isEqualTo(expectedFooPrivVal);
    }

    @Test
    public void getInternalState_throws_expected_error_when_it_cannot_find_field() {
        // given
        SomeObject someObj = new SomeObject(
            "doesnotmatter", "doesnotmatter", "doesnotmatter", "doesnotmatter"
        );

        // when
        Throwable ex = catchThrowable(() -> Glassbox.getInternalState(someObj, "fieldDoesNotExist"));

        // then
        assertThat(ex)
            .isNotNull()
            .hasMessage("Unable to get internal state for field=fieldDoesNotExist on "
                        + "target_class=com.nike.internal.util.testing.GlassboxTest$SomeObject");
    }

    @Test
    public void setInternalState_works_as_expected() {
        // given
        String expectedPubFinalVal = UUID.randomUUID().toString();
        String expectedPrivFinalVal = UUID.randomUUID().toString();
        String expectedPubVal = UUID.randomUUID().toString();
        String expectedPrivVal = UUID.randomUUID().toString();
        SomeObject someObj = new SomeObject("UNSET", "UNSET", "UNSET", "UNSET");

        // when
        Glassbox.setInternalState(someObj, "pubFinalFoo", expectedPubFinalVal);
        Glassbox.setInternalState(someObj, "privFinalFoo", expectedPrivFinalVal);
        Glassbox.setInternalState(someObj, "pubFoo", expectedPubVal);
        Glassbox.setInternalState(someObj, "privFoo", expectedPrivVal);

        // then
        assertThat(someObj.pubFinalFoo).isEqualTo(expectedPubFinalVal);
        assertThat(someObj.privFinalFoo).isEqualTo(expectedPrivFinalVal);
        assertThat(someObj.pubFoo).isEqualTo(expectedPubVal);
        assertThat(someObj.privFoo).isEqualTo(expectedPrivVal);
    }

    @Test
    public void setInternalState_works_as_expected_for_subclass() {
        // given
        String expectedBarPubFinal = UUID.randomUUID().toString();
        String expectedFooPubFinal = UUID.randomUUID().toString();
        String expectedFooPrivFinal = UUID.randomUUID().toString();
        String expectedFooPubVal = UUID.randomUUID().toString();
        String expectedFooPrivVal = UUID.randomUUID().toString();
        SomeObjectExtension someObj = new SomeObjectExtension("UNSET", "UNSET", "UNSET", "UNSET", "UNSET");

        // when
        Glassbox.setInternalState(someObj, "pubFinalBar", expectedBarPubFinal);
        Glassbox.setInternalState(someObj, "pubFinalFoo", expectedFooPubFinal);
        Glassbox.setInternalState(someObj, "privFinalFoo", expectedFooPrivFinal);
        Glassbox.setInternalState(someObj, "pubFoo", expectedFooPubVal);
        Glassbox.setInternalState(someObj, "privFoo", expectedFooPrivVal);

        // then
        assertThat(someObj.pubFinalBar).isEqualTo(expectedBarPubFinal);
        assertThat(someObj.pubFinalFoo).isEqualTo(expectedFooPubFinal);
        assertThat(someObj.getPrivFinalFoo()).isEqualTo(expectedFooPrivFinal);
        assertThat(someObj.pubFoo).isEqualTo(expectedFooPubVal);
        assertThat(someObj.getPrivFoo()).isEqualTo(expectedFooPrivVal);
    }

    @Test
    public void setInternalState_throws_expected_error_when_it_cannot_find_field() {
        // given
        SomeObject someObj = new SomeObject(
            "doesnotmatter", "doesnotmatter", "doesnotmatter", "doesnotmatter"
        );

        // when
        Throwable ex = catchThrowable(() -> Glassbox.setInternalState(someObj, "fieldDoesNotExist", "doesnotmatter"));

        // then
        assertThat(ex)
            .isNotNull()
            .hasMessage("Unable to set internal state for field=fieldDoesNotExist on "
                        + "target_class=com.nike.internal.util.testing.GlassboxTest$SomeObject");
    }
}