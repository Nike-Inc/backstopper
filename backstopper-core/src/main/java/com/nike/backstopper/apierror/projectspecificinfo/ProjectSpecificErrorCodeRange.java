package com.nike.backstopper.apierror.projectspecificinfo;

import com.nike.backstopper.apierror.ApiError;

/**
 * This interface represents a "reservation" of error codes, and is used to prevent error code overlap and conflict
 * across different teams and projects.
 *
 * <p>In a typical use case, an organization might create a central library with an enum that implements this interface.
 * The enum values would be defined to cover specific error code ranges with each enum value representing a different
 * project. For example:
 *
 * <pre>
 *      public enum MyOrgProjectSpecificErrorCodeRange implements ProjectSpecificErrorCodeRange {
 *          COMMON_API_ERROR_RANGE(1, 999),
 *          FOO_SERVICE_ERROR_RANGE(1000, 1999),
 *          BAR_SERVICE_ERROR_RANGE(Arrays.asList(IntegerRange.of(2000, 2999), IntegerRange.of(9000, 9999)));
 *
 *          protected final List&lt;IntegerRange> ranges;
 *
 *          MyOrgProjectSpecificErrorCodeRange(int lowerRangeInclusive, int upperRangeInclusive) {
 *              this.ranges = Collections.singletonList(IntegerRange.of(lowerRangeInclusive, upperRangeInclusive));
 *          }
 *
 *          MyOrgProjectSpecificErrorCodeRange(List&lt;IntegerRange> ranges) {
 *              this.ranges = ranges;
 *          }
 *
 *          &#64;Override
 *          public boolean isInRange(ApiError error) {
 *              for (IntegerRange range : ranges) {
 *                  if (range.isInRange(error.getErrorCode()))
 *                      return true;
 *              }
 *
 *              return false;
 *          }
 *      }
 * </pre>
 *
 * <p>When creating a new project a team would update the central library enum with the project's error code range so
 * that everyone on all projects is aware and prevented from defining errors with the same codes. After you've defined
 * the error range for your project you just need to have your
 * {@link ProjectApiErrors#getProjectSpecificErrorCodeRange()} return it and make sure all the
 * {@link ApiError}s you define for your project have codes that fall into that range.
 *
 * <p>NOTE: There is a predefined "allow everything" range for projects that don't care about overlapping error codes:
 * {@link #ALLOW_ALL_ERROR_CODES}. See the javadocs for that predefined instance for more information on when it might
 * be appropriate to use it.
 *
 * @author Nic Munroe
 */
public interface ProjectSpecificErrorCodeRange {

    /**
     * This is a special range for projects that don't care if they overlap error codes. Many projects can use this
     * range and define completely different errors using the same error codes.
     *
     * <p><b>IMPORTANT NOTE:</b> Due to this range's definition where multiple projects can redefine error codes to mean
     * different things, <b>you should never use this range if your project is ever exposed to outside users!</b> It is
     * suitable for internal projects only where consumers of your API will understand that the error codes your project
     * returns are *not* unique across projects.
     */
    ProjectSpecificErrorCodeRange ALLOW_ALL_ERROR_CODES = new ProjectSpecificErrorCodeRange() {
        @Override
        public boolean isInRange(ApiError error) {
            return true;
        }

        @Override
        public String getName() {
            return "ALLOW_ALL_ERROR_CODES";
        }
    };

    /**
     * @return true if the given error's {@link ApiError#getErrorCode()} falls within this instance's range.
     */
    boolean isInRange(ApiError error);

    /**
     * @return The name of this {@link ProjectSpecificErrorCodeRange}. Used in logs and exceptions to identify the
     *          culprit when out-of-range type errors occur.
     */
    String getName();

}
