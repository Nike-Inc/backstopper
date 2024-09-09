package com.nike.backstopper.apierror.contract.jsr303convention;

import com.nike.backstopper.validation.constraints.StringConvertsToClassType;
import com.nike.internal.util.Pair;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.validation.Constraint;

/**
 * Base class for tests that need to troll through the JSR 303 annotations in the project in order to do some checking
 * on them. We have this base class because the reflection magic necessary to populate the various data is both
 * complicated and time consuming, so we only want to do it once per project and/or test suite. Also provides several
 * reusable helper methods that will come in handy for any test class that needs to process those JSR 303 annotations.
 * <p/>
 * <b>NOTE:</b> For unit tests based on this class you're probably going to be most interested in the
 * {@link #projectRelevantConstraintAnnotationsExcludingUnitTestsList} field and the various helper methods.
 * <p/>
 * <b>QUICK START:</b>
 * <ol>
 *     <li>Create an extension of this class and fill in the required abstract methods.</li>
 *     <li>
 *         If you're following the "JSR 303 messages must correspond to a {@link
 *         com.nike.backstopper.apierror.ApiError}" convention then create an extension of {@link
 *         VerifyJsr303ValidationMessagesPointToApiErrorsTest}, fill in the required abstract method, and then make sure
 *         it is actually getting run by inspecting your unit test report (if it's not then you'll need to manually call
 *         the verification method as part of a test that gets picked up by your system).
 *     </li>
 *     <li>
 *         If you're using the {@link StringConvertsToClassType} JSR 303 annotation then create an extension of {@link
 *         VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest}, fill in the
 *         required abstract method, and then make sure it is actually getting run by inspecting your unit test report
 *         (if it's not then you'll need to manually call the verification method as part of a test that gets picked up
 *         by your system).
 *     </li>
 *     <li>
 *         Add any other tests you need that require JSR 303 annotation trolling (if any). Use the logic in {@link
 *         VerifyJsr303ValidationMessagesPointToApiErrorsTest} and {@link
 *         VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest} as examples
 *         of how to use this base class to perform useful tests.
 *     </li>
 * </ol>
 * See the <b>USAGE</b> section below for more information on how to implement these items (including example classes).
 * <p/>
 * <b>USAGE:</b>
 * <ul>
 *     <li>
 *         This class will usually be extended once per project to represent the relevant JSR 303 annotations that need
 *         to be checked. The two abstract methods that concrete instances of this class must implement are {@link
 *         #ignoreAllAnnotationsAssociatedWithTheseProjectClasses()} and {@link
 *         #specificAnnotationDeclarationExclusionsForProject()}. Both of these methods tell this class which JSR 303
 *         annotations to ignore, so all others found when doing the reflection trolling will be picked up and used to
 *         populate {@link #projectRelevantConstraintAnnotationsExcludingUnitTestsList}.
 *     </li>
 *     <li>
 *         Depending on the system running your tests the reflection trolling can take a long time (several seconds), so
 *         wherever possible it's recommended that you create an extension of this class that is accessed and reused as
 *         a singleton so that the time consuming stuff done in the {@link
 *         ReflectionBasedJsr303AnnotationTrollerBase#ReflectionBasedJsr303AnnotationTrollerBase()} constructor only has
 *         to be done once per project. Here's example:
 *         <pre>
 *               public class SomeJsr303AnnotationTrollerBaseExtension extends ReflectionBasedJsr303AnnotationTrollerBase {
 *
 *                   public static final SomeJsr303AnnotationTrollerBaseExtension INSTANCE = new SomeJsr303AnnotationTrollerBaseExtension();
 *                   public static final SomeJsr303AnnotationTrollerBaseExtension getInstance() { return INSTANCE; }
 *
 *                   // Intentionally private - use {@code getInstance()} to retrieve the singleton instance of this class.
 *                   private SomeJsr303AnnotationTrollerBaseExtension(){ super(); }
 *
 *                   &#64;Override
 *                   protected List&lt;Class&lt;?>> ignoreAllAnnotationsAssociatedWithTheseProjectClasses() {
 *                       List&lt;Class&lt;?>> ignoreList = new ArrayList&lt;>();
 *                       ignoreList.add(SomeAnnotatedTestClass.class);
 *                       ignoreList.addAll(Arrays.asList(SomeTestClassWithAnnotatedInnerClasses.class.getDeclaredClasses()));
 *                       return ignoreList;
 *                   }
 *
 *                   &#64;Override
 *                   protected List&lt;Predicate&lt;Pair&lt;Annotation, AnnotatedElement>>> specificAnnotationDeclarationExclusionsForProject() throws Exception {
 *                       return Arrays.asList(
 *                          ReflectionBasedJsr303AnnotationTrollerBase.generateExclusionForAnnotatedElementAndAnnotationClass(
 *                              SomeTestClassWithSpecificAnnotationToIgnore.class.getDeclaredField("nonCompliantField"),
 *                              NotNull.class
 *                          )
 *                       );
 *                   }
 *               }
 *         </pre>
 *         <p/>
 *         See the implementation of {@code
 *         com.nike.backstopper.apierror.contract.jsr303convention.ReflectionMagicWorksTest.StrictMemberCheck#getStrictMemberCheckExclusionsForNonCompliantDeclarations()}
 *         (a class in the test package of this module) to see all the different ways to generate specific annotation
 *         declaration exclusions (class-level, method-level, field-level, etc).
 *     </li>
 *     <li>
 *         There are two prebuilt unit test classes that are usually all that projects require:
 *         <ol>
 *             <li>
 *                 {@link VerifyJsr303ValidationMessagesPointToApiErrorsTest} - Verifies that *ALL* non-excluded JSR 303
 *                 validation annotations in your project have a message defined that maps to a {@link
 *                 com.nike.backstopper.apierror.ApiError} name from your project's {@link
 *                 com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors}.
 *             </li>
 *             <li>
 *                 {@link
 *                 VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest} -
 *                 Makes sure that any Enums referenced by {@link StringConvertsToClassType} JSR 303 annotations are
 *                 case insensitive
 *             </li>
 *         </ol>
 *         To take advantage of these prebuilt classes all you have to do is extend them and fill in the abstract {@code
 *         getAnnotationTroller()} method to have it return your project's extension of this class (again reusing a
 *         singleton is highly recommended as described above). As long as your unit test runner picks them up and runs
 *         the test methods in the subclass you should be good to go. <b>NOTE:</b> This is especially important if your
 *         project is using TestNG instead of JUnit, for example, as these base prebuilt unit test classes are annotated
 *         with JUnit {@code @Test} annotations. You may need to create a new test method that is guaranteed to get
 *         fired during your unit tests and manually call the parent method to perform the checks. For example:
 *         <pre>
 *              public class MyProjectVerifyJsr303ValidationMessagesPointToApiErrorsTest extends VerifyJsr303ValidationMessagesPointToApiErrorsTest {
 *
 *                  &#64;Override
 *                  protected ReflectionBasedJsr303AnnotationTrollerBase getAnnotationTroller() {
 *                      return MyProjectJsr303AnnotationTrollerBase.getInstance();
 *                  }
 *
 *                  &#64;org.testng.annotations.Test
 *                  &#64;Override
 *                  public void verifyThatAllValidationAnnotationsReferToApiErrors() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
 *                      super.verifyThatAllValidationAnnotationsReferToApiErrors();
 *                  }
 *              }
 *         </pre>
 *     </li>
 * </ul>
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public abstract class ReflectionBasedJsr303AnnotationTrollerBase {

    /**
     * The default set of packages to include when {@link #getDefaultPackagesToSearchForConstraintAnnotations()} is
     * called.
     */
    private final Set<String> DEFAULT_CONSTRAINT_SEARCH_PACKAGES = new LinkedHashSet<>(Arrays.asList(
        "com.nike",
        "org.hibernate.validator.constraints",
        "jakarta.validation.constraints"
    ));

    /**
     * Utility for trolling through the project looking for declarations that match specific requirements - this will be
     * configured to pick up all JSR 303 annotation classes and declarations in the project that we care about (i.e.
     * classes that we use and the declarations in *our* code).
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final Reflections reflections;
    /**
     * The list of annotation classes used in our project. NOTE: This includes annotations on test classes!
     */
    public final List<Class<? extends Annotation>> constraintAnnotationClasses;
    /**
     * The FULL list of "annotation type to annotation declaration" pairs in our project, INCLUDING annotations placed
     * on unit test classes. Each annotation declaration in our project will have one of these pairs. NOTE: This
     * INCLUDES annotations on test classes! If you just want the annotations relevant to the project (excluding unit
     * tests) you should use {@link #projectRelevantConstraintAnnotationsExcludingUnitTestsList} instead.
     */
    public final List<Pair<Annotation, AnnotatedElement>> allConstraintAnnotationsMasterList;
    /**
     * The list of "annotation type to annotation declaration" pairs in our project, EXCLUDING irrelevant unit test
     * annotation declarations. Each annotation declaration in our project will have one of these pairs. NOTE: This
     * EXCLUDES annotations on test classes! If you want ALL the annotations, including annotations placed on unit test
     * classes, you should use {@link #allConstraintAnnotationsMasterList} instead.
     */
    public final List<Pair<Annotation, AnnotatedElement>> projectRelevantConstraintAnnotationsExcludingUnitTestsList;

    /**
     * Classes added to this list via {@link #ignoreAllAnnotationsAssociatedWithTheseProjectClasses()} will be excluded
     * from the {@link #projectRelevantConstraintAnnotationsExcludingUnitTestsList} list. *All* annotations on this
     * class or its fields/methods/constructors/etc will be ignored.
     * <p/>
     * If you have a single, *specific* annotation declaration that you want ignored you should use {@link
     * #specificAnnotationDeclarationsExcludedFromStrictMessageRequirement} instead.
     */
    private final List<Class<?>> ignoreAllAnnotationsAssociatedWithTheseClasses;
    /**
     * Each item in this list represents a single, *specific* annotation declaration that should be excluded from the
     * {@link #projectRelevantConstraintAnnotationsExcludingUnitTestsList} list. If a predicate in this list returns
     * true then that specific annotation declaration (represented by the pair of the annotation declaration and the
     * element it is annotated on) will be excluded.
     * <p/>
     * There is a helper method at {@link #generateExclusionForAnnotatedElementAndAnnotationClass(java.lang.reflect.AnnotatedElement,
     * Class)} that should build Predicates to cover most cases you might care about, however if you need more specific
     * logic you can build your own Predicates.
     * <p/>
     * If you need a blanket "ignore all annotations on this class" type of exclusion, use {@link
     * #ignoreAllAnnotationsAssociatedWithTheseClasses} instead.
     */
    private final List<Predicate<Pair<Annotation, AnnotatedElement>>>
        specificAnnotationDeclarationsExcludedFromStrictMessageRequirement;

    /**
     * The list returned by this method is used to help populate {@link #ignoreAllAnnotationsAssociatedWithTheseClasses}
     * (see that field's javadocs for more information). Concrete extensions of this class should implement this to
     * include any classes where they want *all* JSR 303 annotations in the class excluded from the "JSR 303 annotation
     * messages must point to ApiErrors" requirement. Note that if you have a class with several inner classes defined
     * and you want everything ignored (common for unit test classes) then you can get all those inner classes at once
     * with the convenient {@link Class#getDeclaredClasses()} method (e.g. {@code SomeWidgetTest.class.getDeclaredClasses()}).
     * You can safely return null for this method and it will be treated the same as an empty list.
     */
    protected abstract List<Class<?>> ignoreAllAnnotationsAssociatedWithTheseProjectClasses();

    /**
     * The list returned by this method is used to help populate {@link #specificAnnotationDeclarationsExcludedFromStrictMessageRequirement}.
     * Concrete extensions of this class should implement this to include any specific annotation declarations they want
     * excluded from the "JSR 303 annotation messages must point to ApiErrors" requirement. See {@link
     * #specificAnnotationDeclarationsExcludedFromStrictMessageRequirement} for more details, and make your life easier
     * by using {@link #generateExclusionForAnnotatedElementAndAnnotationClass(java.lang.reflect.AnnotatedElement,
     * Class)}. For example, to exclude a {@code @NotNull} annotation on {@code SomeWidget.someField} you would call
     * {@code generateExclusionForAnnotatedElementAndAnnotationClass(SomeWidget.class.getDeclaredField("someField"),
     * NotNull.class)} and add that to the returned list. You can safely return null for this method and it will be
     * treated the same as an empty list.
     *
     * @see #specificAnnotationDeclarationsExcludedFromStrictMessageRequirement
     * @see #generateExclusionForAnnotatedElementAndAnnotationClass(java.lang.reflect.AnnotatedElement, Class)
     */
    protected abstract List<Predicate<Pair<Annotation, AnnotatedElement>>> specificAnnotationDeclarationExclusionsForProject()
        throws Exception;

    /**
     * Helper constructor that calls {@link #ReflectionBasedJsr303AnnotationTrollerBase(Set)} passing in null in order
     * to use only the default packages when searching for constraint annotations.
     */
    public ReflectionBasedJsr303AnnotationTrollerBase() {
        this((Set<String>) null);
    }

    /**
     * Helper constructor that calls {@link #ReflectionBasedJsr303AnnotationTrollerBase(Set)} passing in the given
     * varargs as a set for the extra packages to use when searching for constraint annotations.
     */
    @SuppressWarnings("unused")
    public ReflectionBasedJsr303AnnotationTrollerBase(String... extraPackagesForConstraintAnnotationSearch) {
        this(new LinkedHashSet<>(Arrays.asList(extraPackagesForConstraintAnnotationSearch)));
    }

    /**
     * Initializes the instance based on what is returned by {@link #ignoreAllAnnotationsAssociatedWithTheseProjectClasses()}
     * and {@link #specificAnnotationDeclarationExclusionsForProject()}. This is time consuming and should only be done
     * once per project if possible - see the usage info in the {@link ReflectionBasedJsr303AnnotationTrollerBase}
     * class-level javadocs.
     *
     * <p>The given set of extra packages for constraint annotation searching will be passed into {@link
     * #getFinalPackagesToSearchForConstraintAnnotations(Set)} to generate the final set of packages that are searched.
     * If you don't want the {@link #DEFAULT_CONSTRAINT_SEARCH_PACKAGES} default packages to be searched you can
     * override {@link #getDefaultPackagesToSearchForConstraintAnnotations()}.
     */
    public ReflectionBasedJsr303AnnotationTrollerBase(Set<String> extraPackagesForConstraintAnnotationSearch) {

        /*
         * Set up the {@link #ignoreAllAnnotationsAssociatedWithTheseClasses} and
         * {@link #specificAnnotationDeclarationsExcludedFromStrictMessageRequirement} fields so we know which
         * annotations are project-relevant vs. unit-test-only.
         */
        ignoreAllAnnotationsAssociatedWithTheseClasses =
            new ArrayList<>(setupIgnoreAllAnnotationsAssociatedWithTheseClasses());
        specificAnnotationDeclarationsExcludedFromStrictMessageRequirement =
            new ArrayList<>(setupSpecificAnnotationDeclarationExclusions());

        /*
         * Set up the {@link #reflections}, {@link #constraintAnnotationClasses}, and
         * {@link #allConstraintAnnotationsMasterList} fields. This is where the crazy reflection magic happens to troll
         * the project for the JSR 303 annotation declarations.
         */
        // Create the ConfigurationBuilder to search the relevant set of packages.
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        for (String packageToAdd : getFinalPackagesToSearchForConstraintAnnotations(
            extraPackagesForConstraintAnnotationSearch)) {
            configurationBuilder.addUrls(ClasspathHelper.forPackage(packageToAdd));
        }

        // Create the Reflections object so it scans for all validation annotations we care about and all project
        //      classes that might have annotations on them.
        reflections = new Reflections(configurationBuilder.setScanners(
            new SubTypesScanner(), new MethodParameterScanner(), new TypeAnnotationsScanner(),
            new MethodAnnotationsScanner(), new FieldAnnotationsScanner()
        ));

        // Gather the list of all JSR 303 validation annotations in the project. Per the JSR 303 spec this is any
        //      annotation class type that is marked with @Constraint.
        constraintAnnotationClasses = new ArrayList<>();
        for (Class<?> constraintAnnotatedType : reflections.getTypesAnnotatedWith(Constraint.class, true)) {
            if (constraintAnnotatedType.isAnnotation()) {
                //noinspection unchecked
                constraintAnnotationClasses.add((Class<? extends Annotation>) constraintAnnotatedType);
            }
        }

        // We're not done gathering validation annotations though, unfortunately. JSR 303 also says that *any*
        //      annotation (whether it is a Constraint or not) that has a value field that returns an array of actual
        //      Constraints is treated as a "multi-value constraint", and the validation processor will run each
        //      of the Constraints in the array as if they were declared separately on the annotated element. Therefore,
        //      we have to dig through all the annotations in the project, find any that fall into this "multi-value
        //      constraint" category, and include them in our calculations.
        for (Class<? extends Annotation> annotationClass : reflections.getSubTypesOf(Annotation.class)) {
            if (isMultiValueConstraintClass(annotationClass))
                constraintAnnotationClasses.add(annotationClass);
        }

        // Setup the master constraint list
        allConstraintAnnotationsMasterList =
            new ArrayList<>(setupAllConstraintAnnotationsMasterList(reflections, constraintAnnotationClasses));

        /*
         * Finally use the info we've gathered/constructed previously to populate the
         * {@link #projectRelevantConstraintAnnotationsExcludingUnitTestsList} field, which is the main chunk of data
         * that extensions of this class will care about.
         */
        projectRelevantConstraintAnnotationsExcludingUnitTestsList = Collections.unmodifiableList(
            getSubAnnotationListUsingExclusionFilters(allConstraintAnnotationsMasterList,
                                                      ignoreAllAnnotationsAssociatedWithTheseClasses,
                                                      specificAnnotationDeclarationsExcludedFromStrictMessageRequirement));
    }

    /**
     * @param extraPackagesForConstraintAnnotationSearch Extra project-specific packages that should be included in the
     *                                                   constraint annotation searching.
     * @return The final set of packages that should be used when doing constraint annotation searching. The given
     * {@code extraPackagesForConstraintAnnotationSearch} will be added to {@link #getDefaultPackagesToSearchForConstraintAnnotations()}
     * and returned. If you want a different set of default packages then you should override that method.
     */
    protected Set<String> getFinalPackagesToSearchForConstraintAnnotations(
        Set<String> extraPackagesForConstraintAnnotationSearch) {
        Set<String> finalPackages = new LinkedHashSet<>(getDefaultPackagesToSearchForConstraintAnnotations());
        if (extraPackagesForConstraintAnnotationSearch != null)
            finalPackages.addAll(extraPackagesForConstraintAnnotationSearch);
        return finalPackages;
    }

    /**
     * @return {@link #DEFAULT_CONSTRAINT_SEARCH_PACKAGES}. If you need different behavior then override this method.
     */
    protected Set<String> getDefaultPackagesToSearchForConstraintAnnotations() {
        return DEFAULT_CONSTRAINT_SEARCH_PACKAGES;
    }

    /**
     * @return {@link #ignoreAllAnnotationsAssociatedWithTheseProjectClasses()}, or an empty list if it is null.
     */
    private List<Class<?>> setupIgnoreAllAnnotationsAssociatedWithTheseClasses() {
        List<Class<?>> ignoreList = ignoreAllAnnotationsAssociatedWithTheseProjectClasses();
        if (ignoreList == null)
            ignoreList = new ArrayList<>();

        return ignoreList;
    }

    /**
     * @return {@link #specificAnnotationDeclarationExclusionsForProject()}, or an empty list if it is null.
     */
    private List<Predicate<Pair<Annotation, AnnotatedElement>>> setupSpecificAnnotationDeclarationExclusions() {
        List<Predicate<Pair<Annotation, AnnotatedElement>>> specificDeclarationExclusionsList;

        try {
            specificDeclarationExclusionsList = specificAnnotationDeclarationExclusionsForProject();
            if (specificDeclarationExclusionsList == null)
                specificDeclarationExclusionsList = new ArrayList<>();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return specificDeclarationExclusionsList;
    }

    /**
     * @return The master list of constraint annotations appropriate for populating {@link
     * #allConstraintAnnotationsMasterList} - see that field's javadocs for more info.
     */
    private List<Pair<Annotation, AnnotatedElement>> setupAllConstraintAnnotationsMasterList(
        Reflections reflectionsArg, List<Class<? extends Annotation>> constraintAnnotationClassesArg
    ) {
        List<Pair<Annotation, AnnotatedElement>> masterList = new ArrayList<>();
        for (Class<? extends Annotation> constraintAnnotationClass : constraintAnnotationClassesArg) {
            // We will need to treat multi-value and single Constraints differently
            boolean isMultiValueConstraint = isMultiValueConstraintClass(constraintAnnotationClass);

            // Grab the easy-to-handle elements annotated with this class.
            List<AnnotatedElement> elementsAnnotatedWithThisClass = new ArrayList<>();
            elementsAnnotatedWithThisClass
                .addAll(reflectionsArg.getConstructorsAnnotatedWith(constraintAnnotationClass));
            elementsAnnotatedWithThisClass.addAll(reflectionsArg.getMethodsAnnotatedWith(constraintAnnotationClass));
            elementsAnnotatedWithThisClass.addAll(reflectionsArg.getFieldsAnnotatedWith(constraintAnnotationClass));

            // Register the easy-to-handle element annotations into our master list.
            for (AnnotatedElement annotatedElement : elementsAnnotatedWithThisClass) {
                List<Annotation> annotationsToRegister = explodeAnnotationToManyConstraintsIfMultiValue(
                    annotatedElement.getAnnotation(constraintAnnotationClass), isMultiValueConstraint);
                for (Annotation annotationToRegister : annotationsToRegister) {
                    masterList.add(Pair.of(annotationToRegister, annotatedElement));
                }
            }

            // Grab the class types annotated with this class.
            List<Class<?>> typesAnnotatedWithThisClass =
                new ArrayList<>(reflectionsArg.getTypesAnnotatedWith(constraintAnnotationClass));

            // Register the class type annotations into our master list.
            for (Class<?> annotatedClassType : typesAnnotatedWithThisClass) {
                // We don't want to include annotations on this class type if it is itself an annotation class since
                //      that is how validation annotations do "inheritance", "composition", or "is-a" marking.
                //      e.g. @NotBlank is itself annotated with @NotNull to indicate that it is an extension of
                //      NotNull's logic, and it shouldn't be part of the strict message checking.
                if (!annotatedClassType.isAnnotation()) {
                    List<Annotation> annotationsToRegister = explodeAnnotationToManyConstraintsIfMultiValue(
                        annotatedClassType.getAnnotation(constraintAnnotationClass), isMultiValueConstraint);
                    for (Annotation annotationToRegister : annotationsToRegister) {
                        masterList.add(Pair.of(annotationToRegister, annotatedClassType));
                    }
                }
            }

            // Grab the method params annotated with this class.
            List<Method> methodParamsAnnotatedWithThisClass =
                new ArrayList<>(reflectionsArg.getMethodsWithAnyParamAnnotated(constraintAnnotationClass));

            // Register the method param annotations into our master list.
            for (Method methodWithAnnotatedParam : methodParamsAnnotatedWithThisClass) {
                Annotation[][] paramAnnotations = methodWithAnnotatedParam.getParameterAnnotations();
                masterList.addAll(
                    extractAnnotationsFrom2dArray(paramAnnotations, constraintAnnotationClass, isMultiValueConstraint,
                                                  methodWithAnnotatedParam));
            }

            // Grab the constructor params annotated with this class.
            @SuppressWarnings("rawtypes")
            List<Constructor> constructorParamsAnnotatedWithThisClass =
                new ArrayList<>(reflectionsArg.getConstructorsWithAnyParamAnnotated(constraintAnnotationClass));

            // Register the constructor param annotations into our master list.
            for (Constructor<?> constructorWithAnnotatedParam : constructorParamsAnnotatedWithThisClass) {
                Annotation[][] paramAnnotations = constructorWithAnnotatedParam.getParameterAnnotations();
                masterList.addAll(
                    extractAnnotationsFrom2dArray(paramAnnotations, constraintAnnotationClass, isMultiValueConstraint,
                                                  constructorWithAnnotatedParam));
            }
        }

        return masterList;
    }

    /**
     * @return true if this is a multi-value constraint class as per JSR 303 requirements (contains a value() method
     * which returns an array of Constraints), false otherwise.
     */
    private static boolean isMultiValueConstraintClass(Class<? extends Annotation> annotationClass) {
        // It must have a value() method.
        Method valueMethod;
        try {
            valueMethod = annotationClass.getDeclaredMethod("value");
        }
        catch (NoSuchMethodException e) {
            return false;
        }

        // That value field must return a type of "array of Constraint"
        //noinspection RedundantIfStatement
        if (valueMethod.getReturnType().isArray()
            && valueMethod.getReturnType().getComponentType().getAnnotation(Constraint.class) != null) {
            return true;
        }

        return false;
    }


    /**
     * @return The list of Constraint annotations retrieved from annotation.value() if it is a multi-value constraint
     * annotation, or a singleton list containing only the given annotation if it is not a multi-value constraint.
     */
    private static List<Annotation> explodeAnnotationToManyConstraintsIfMultiValue(Annotation annotation,
                                                                                   boolean isMultiValueConstraint) {
        if (!isMultiValueConstraint)
            return Collections.singletonList(annotation);

        try {
            Method valueMethod = annotation.getClass().getMethod("value");
            Object[] subAnnotations = (Object[]) valueMethod.invoke(annotation);
            // We know that each object in the array is a Constraint, so we can safely cast it to an annotation.
            List<Annotation> returnList = new ArrayList<>();
            for (Object subAnnotation : subAnnotations) {
                returnList.add((Annotation) subAnnotation);
            }
            return returnList;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("Expected multi-value constraint annotation to have a 'value' method.", e);
        }
        catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The list of annotation->owningElement pairs from the given 2-dimensional array that match the given
     * desiredAnnotationClass - note that if desiredAnnotationClassIsMultiValueConstraint is true then each matching
     * annotation will be exploded via {@link #explodeAnnotationToManyConstraintsIfMultiValue(java.lang.annotation.Annotation,
     * boolean)} before being added to the return list.
     */
    private static List<Pair<Annotation, AnnotatedElement>> extractAnnotationsFrom2dArray(
        Annotation[][] annotations2dArray, Class<? extends Annotation> desiredAnnotationClass,
        boolean desiredAnnotationClassIsMultiValueConstraint, AnnotatedElement owningElement) {
        List<Pair<Annotation, AnnotatedElement>> returnList = new ArrayList<>();
        for (Annotation[] innerArray : annotations2dArray) {
            for (Annotation annotation : innerArray) {
                if (annotation.annotationType().equals(desiredAnnotationClass)) {
                    List<Annotation> annotationsToRegister = explodeAnnotationToManyConstraintsIfMultiValue(
                        annotation, desiredAnnotationClassIsMultiValueConstraint
                    );
                    for (Annotation annotationToRegister : annotationsToRegister) {
                        returnList.add(Pair.of(annotationToRegister, owningElement));
                    }
                }
            }
        }

        return returnList;
    }

    // ==================== REUSABLE HELPER METHODS FOR SUBCLASSES =========================

    /**
     * @return A Predicate that will exclude the given annotation declaration (represented by annotated element and
     * annotation class), ready for insertion into {@link #specificAnnotationDeclarationsExcludedFromStrictMessageRequirement}
     */
    public static Predicate<Pair<Annotation, AnnotatedElement>> generateExclusionForAnnotatedElementAndAnnotationClass(
        final AnnotatedElement annotatedElement, final Class<? extends Annotation> annotationClass) {
        return input -> {
            //noinspection RedundantIfStatement
            if (annotatedElement.equals(input.getRight()) && annotationClass
                .equals(input.getLeft().annotationType()))
                return true;

            return false;
        };
    }

    /**
     * @return Helper method to extract the annotation.message() string.
     */
    public static String extractMessageFromAnnotation(Annotation annotation) {
        try {
            Method messageMethod = annotation.annotationType().getDeclaredMethod("message");
            return (String) messageMethod.invoke(annotation);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Helper method that extracts the "owner class" from the given AnnotatedElement. In the context of this
     * unit test, this method expects annotatedElement to be either a {@link java.lang.reflect.Member} (in which case
     * the owning class is {@link java.lang.reflect.Member#getDeclaringClass()}), or annotatedElement should be a Class
     * (in which case there is no owning class since the annotation is *on* the class, and this method just returns the
     * annotatedElement cast to a Class).
     */
    public static Class<?> getOwnerClass(AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Member)
            return ((Member) annotatedElement).getDeclaringClass();
        else if (annotatedElement instanceof Class)
            return (Class<?>) annotatedElement;

        throw new IllegalArgumentException(
            "Expected annotatedElement to be of type Member or Class, but instead received: " + annotatedElement
                .getClass().getName());
    }

    /**
     * @return Helper method that returns the given listToFilter after it has been filtered down based on the given
     * keepTheseItemsFilter predicate.
     */
    public static List<Pair<Annotation, AnnotatedElement>> getSubAnnotationList(
        List<Pair<Annotation, AnnotatedElement>> listToFilter,
        Predicate<Pair<Annotation, AnnotatedElement>> keepTheseItemsFilter
    ) {
        List<Pair<Annotation, AnnotatedElement>> returnList = new ArrayList<>();
        for (Pair<Annotation, AnnotatedElement> pair : listToFilter) {
            if (keepTheseItemsFilter.test(pair))
                returnList.add(pair);
        }

        return returnList;
    }

    /**
     * @return Helper method that filters the listToFilter down to only the items where {@link
     * #getOwnerClass(java.lang.reflect.AnnotatedElement)} matches the given ownerClass.
     */
    public static List<Pair<Annotation, AnnotatedElement>> getSubAnnotationListForElementsOfOwnerClass(
        List<Pair<Annotation, AnnotatedElement>> listToFilter,
        final Class<?> ownerClass
    ) {
        return getSubAnnotationList(listToFilter, input -> getOwnerClass(input.getRight()).equals(ownerClass));
    }

    /**
     * @return Helper method that filters the listToFilter down to only the items where the annotation.annotationType()
     * matches the given desiredAnnotationClass.
     */
    public static List<Pair<Annotation, AnnotatedElement>> getSubAnnotationListForAnnotationsOfClassType(
        List<Pair<Annotation, AnnotatedElement>> listToFilter,
        final Class<?> desiredAnnotationClass
    ) {
        return getSubAnnotationList(
            listToFilter,
            input -> input.getLeft().annotationType().equals(desiredAnnotationClass)
        );
    }

    /**
     * @return Helper method that returns the given listToFilter after any pairs have been removed where the pair's
     * AnnotatedElement's owning class is in annotatedElementOwnerClassesToExclude OR where the pair matches any matcher
     * in specificAnnotationDeclarationExclusionMatchers.
     */
    public static List<Pair<Annotation, AnnotatedElement>> getSubAnnotationListUsingExclusionFilters(
        List<Pair<Annotation, AnnotatedElement>> listToFilter,
        final List<Class<?>> annotatedElementOwnerClassesToExclude,
        final List<Predicate<Pair<Annotation, AnnotatedElement>>> specificAnnotationDeclarationExclusionMatchers) {
        return getSubAnnotationList(listToFilter, input -> {
            AnnotatedElement annotatedElement = input.getRight();

            if (annotatedElementOwnerClassesToExclude != null && annotatedElementOwnerClassesToExclude
                .contains(getOwnerClass(annotatedElement)))
                return false;

            if (specificAnnotationDeclarationExclusionMatchers != null) {
                for (Predicate<Pair<Annotation, AnnotatedElement>> exclusionMatcher : specificAnnotationDeclarationExclusionMatchers) {
                    if (exclusionMatcher.test(input))
                        return false;
                }
            }

            return true;
        });
    }

    /**
     * @return A String identifying where the given AnnotatedElement lives in the project - for example an annotation
     * placed on a field would return a string like {@code "com.nike.somepackage.SomeClass.someField[FIELD]"}. This is
     * handy for helping a dev track down a specific annotation declaration causing a unit test to fail.
     */
    public static String getAnnotatedElementLocationAsString(AnnotatedElement annotatedElement) {
        StringBuilder sb = new StringBuilder();

        sb.append(getOwnerClass(annotatedElement).getName());
        if (annotatedElement instanceof Constructor)
            sb.append("[CONSTRUCTOR]");
        else if (annotatedElement instanceof Class)
            sb.append("[CLASS]");
        else if (annotatedElement instanceof Method)
            sb.append(".").append(((Method) annotatedElement).getName()).append("[METHOD]");
        else if (annotatedElement instanceof Field)
            sb.append(".").append(((Field) annotatedElement).getName()).append("[FIELD]");
        else
            sb.append(".").append(annotatedElement.toString()).append("[???]");

        return sb.toString();
    }
}
