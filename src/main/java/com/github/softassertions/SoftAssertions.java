/*
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org/>
*/
package com.github.softassertions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.List;

/**
 * SoftAssertions is a class consisting of static utility methods used to
 * perform runtime soft-assertion checks.
 */
public class SoftAssertions {

    public interface Logger {
        void log(String message, Throwable exception);

    }

    public static synchronized void setLogger(Logger logger) {
        SoftAssertions.logger = logger;
    }

    static Logger logger = null;


    /**
     * An instance of this class is passed to the {@link #log(String, Throwable)} method when the
     * {@link #log(String, Throwable)} method is called in the various static utility methods.
     *
     * PURPOSE: To provide a stack trace with each {@link #log(String, Throwable)} that occurs from
     * within the various static utility methods.
     *
     * Notice that this class simply extends Throwable, and does nothing more. Granted,
     * a new Throwable() could have been passed into each such call to the
     * {@link #log(String, Throwable)} method. However, by extending Throwable, the logs at least
     * now make it clear that the stack trace is for a StackRevealer object as opposed to just
     * an anonymous Throwable.
     */
    private static class StackRevealer extends Throwable {

    }

    /**
     * This interface is used internally by the following methods:
     * <ul>
     *     <li>isMinimumLength(Object[], int)</li>
     *     <li>isMinimumLength(Collection&lt;?&gt;, int)</li>
     *     <li>isEmpty(Object[])</li>
     *     <li>isEmpty(Collection&lt;?&gt;)</li>
     *     <li>hasIndex(Object[], int)</li>
     *     <li>hasIndex(Collection&lt;?&gt;, int)</li>
     * </ul>
     *
     * So that each of those methods can funnel into
     * isMinimumLength(Collection&lt;?&gt;, int) methods so that those two methods
     * can call a generic/templatized method that does the actual work, but displays
     * a different warning message that is appropriate to the situation.
     *
     * Basically, the check for minimum length, empty, or hasIndex guards can and should be
     * implemented by the same chunk of code. This interface solves the problem in that the
     * resulting warning message from the chunk of code needs to be tailored to the situation.
     */
    private interface MinimumLengthMessageGenerator {
        /**
         * The helper_isMinimumLength(...) method calls this method when the minimum
         * length is not satisfied. The size of the array or collection is passed in.
         *
         * For more background context, see the methods referenced in the JavaDoc for
         * this interface.
         *
         * @param size the size of the array or collection
         *
         * @return a warning message that is appropriate to the situation, as explained
         * in the description for this method and as explained in the description for this
         * interface.
         */
        String toString(int size);
    }

    static void log(String message, Throwable t) {
        Logger logger = SoftAssertions.logger;
        if (logger == null) {
            System.err.println("Soft Assertion Failed: " + message);
            if (t != null) {
                t.printStackTrace(System.err);
            }
        } else {
            logger.log(message, t);
        }
    }

    /**
     * This method is called throughout the Android codebase to ensure that a given object
     * reference is not null. If the given object reference is null, a Android Log.e(...) is
     * performed indicating "Please have my program fixed!" and providing a stack trace.
     *
     * @param o reference to an object that this method will ensure is not null.
     *
     * @return true if the given object is not null or false if the given object is null
     */
    public static boolean isNotNull(Object o) {
        boolean retVal = false;
        if (o == null) {
            log("Unexpected null reference. Please have my program fixed!",
                    new StackRevealer());
        } else {
            retVal = true;
        }
        return retVal;
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withNotNull(Object, WithRunnable)} method.
     *
     * @param <T> this is the implicitly determined type of value that is passed
     *           as the one and only value to the block of code that is passed as
     *           the Java 8 LAMBDA-style statement that is passed as the last argument
     *           to the method such as the {@link #withNotNull(Object, WithRunnable)}
     *           method.
     *
     * @see #withNotNull(Object, WithRunnable)
     * @see #withIndex(Object[], int, WithRunnable)
     * @see #withExpectedType(Object, Class, WithRunnable)
     * @see #withIndex(List, int, WithRunnable)
     */
    public interface WithRunnable<T> {
        void run(@NonNull T t);
    }

    public interface WithRunnableIndex<T> {
        void run(int index, @NonNull T t);
    }

    /**
     * Invokes the given logic passing in {@code o} if the given value {@code} is not null.
     *
     * Example Invocation 1:
     * <pre>
     *     withNotNull(getFoo(), foo -&gt; {
     *        // Do something with foo
     *        ...
     *     });
     * </pre>
     *
     * Example Invocation 2:
     *
     * <pre>
     *     void doSomething(Foo foo) {
     *         ...
     *     }
     *     withNotNull(getFoo(), this::doSomething);
     * </pre>
     *
     * @param o a value that is presumably non-null
     * @param runnable a chunk of Java 8 LAMBDA-style logic that takes {@code o} as a parameter.
     *                 This logic will be invoked and passed the value {@code o} if {@code o} is
     *                 indeed non-null.
     * @param <T> the type for {@code o}, which the Java compiler implicitly determines when the
     *           given {@code runnable} is provided as a chunk of Java 8 LAMBDA-style logic.
     */
    public static <T> void withNotNull(T o, WithRunnable<T> runnable) {
        if (isNotNull(o) && isNotNull(runnable)) {
            runnable.run(o);
        }
    }

    /**
     * Extracts the value from the given array at the given index and passes the value as the
     * one-and-only argument to the given chunk of Java-8 LAMBDA-style logic if the given array
     * is non-null, the given chunk of Java-8 LAMBDA-style logic is non-null, the given array is
     * of sufficient size to include the indicated index, and if the value in the array at the
     * indicated index is non-null.
     *
     * Example Invocation:
     * <pre>
     *     Object myArray[] = new Object[...];
     *     ...
     *     withIndex(myArray, 0, firstItem -&gt; {
     *         // Processes the first item in the array
     *         doSomething(firstItem);
     *     });
     * </pre>
     *
     * @param array an array of objects that is presumably non-null, otherwise an error message is
     *              appended to the error log instead of crashing.
     * @param index the location in the array to access. Presumably the array is of sufficient
     *              length to access this index, otherwise an error message is appended to the
     *              error log instead of crashing.
     * @param runnable a chunk of Java 8 LAMBDA-style logic that takes the corresponding element
     *                 from the given {@code array} as the logic's one-and-only parameter. This
     *                 logic will be invoked and passed the value the value from the given
     *                 {@code array} at the indicated {@code index} if the given {@code array} is
     *                 non-null and if the given {@code array} is of sufficiently allocated length
     *                 to indeed have the indicated {@code index}.
     * @param <T> the element type for the given {@code array}, which the Java compiler implicitly
     *           determines when the given {@code runnable} is provided as a chunk of Java 8
     *           LAMBDA-style logic.
     */
    public static <T> void withIndex(T[] array, int index, WithRunnable<T> runnable) {
        if (hasIndex(array, index)) {
            T obj = array[index];
            if (isNotNull(obj)) {
                runnable.run(obj);
            }
        }
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(int[], int, WithRunnableInt)} method.
     *
     * @see #withIndex(int[], int, WithRunnableInt)
     */
    public interface WithRunnableInt {
        void run(int i);
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(int[], int, WithRunnableInt)} method.
     *
     * @see #withIndex(int[], int, WithRunnableInt)
     */
    public interface WithRunnableIndexInt {
        void run(int index, int value);
    }

    /**
     * Same as {@link #withIndex(Object[], int, WithRunnable)} but for primitive int arrays.
     *
     * @param intArray a primitive int[] to safely access a value therein
     * @param index the index within intArray to access
     * @param runnable a chunk of Java-8 LAMBDA-Style code to be called and passed the index
     *                 as the first parameter and the array value at the specified index.
     */
    public static void withIndex(int[] intArray, int index, WithRunnableInt runnable) {
        if (hasIndex(intArray, index)) {
            runnable.run(intArray[index]);
        }
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(short[], int, WithRunnableShort)}  method.
     *
     * @see #withIndex(short[], int, WithRunnableShort)
     */
    public interface WithRunnableShort {
        void run(short s);
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(short[], int, WithRunnableShort)}  method.
     *
     * @see #withIndex(short[], int, WithRunnableShort)
     */
    public interface WithRunnableIndexShort {
        void run(int index, short value);
    }

    /**
     * Same as {@link #withIndex(Object[], int, WithRunnable)} but for primitive short arrays.
     *
     * @param shortArray a primitive short[] to safely access a value therein
     * @param index the index within shortArray to access
     * @param runnable a chunk of Java-8 LAMBDA-Style code to be called and passed the index
     *                 as the first parameter and the array value at the specified index.
     */
    public static void withIndex(short[] shortArray, int index, WithRunnableShort runnable) {
        if (hasIndex(shortArray, index)) {
            runnable.run(shortArray[index]);
        }
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(byte[], int, WithRunnableByte)}  method.
     *
     * @see #withIndex(byte[], int, WithRunnableByte)
     */
    public interface WithRunnableByte {
        void run(byte b);
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(byte[], int, WithRunnableByte)}  method.
     *
     * @see #withIndex(byte[], int, WithRunnableByte)
     */
    public interface WithRunnableIndexByte {
        void run(int index, byte value);
    }

    /**
     * Same as {@link #withIndex(Object[], int, WithRunnable)} but for primitive byte arrays.
     *
     * @param byteArray a primitive byte[] to safely access a value therein
     * @param index the index within byteArray to access
     * @param runnable a chunk of Java-8 LAMBDA-Style code to be called and passed the index
     *                 as the first parameter and the array value at the specified index.
     */
    public static void withIndex(byte[] byteArray, int index, WithRunnableByte runnable) {
        if (hasIndex(byteArray, index)) {
            runnable.run(byteArray[index]);
        }
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(char[], int, WithRunnableChar)}  method.
     *
     * @see #withIndex(char[], int, WithRunnableChar)
     * @see #withCharAt(String, int, WithRunnableChar)
     */
    public interface WithRunnableChar {
        void run(char b);
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(char[], int, WithRunnableChar)}  method.
     *
     * @see #withIndex(char[], int, WithRunnableChar)
     * @see #withCharAt(String, int, WithRunnableChar)
     */
    public interface WithRunnableIndexChar {
        void run(int index, char value);
    }

    /**
     * Same as {@link #withIndex(Object[], int, WithRunnable)} but for primitive char arrays.
     *
     * @param charArray a primitive char[] to safely access a value therein
     * @param index the index within charArray to access
     * @param runnable a chunk of Java-8 LAMBDA-Style code to be called and passed the index
     *                 as the first parameter and the array value at the specified index.
     */
    public static void withIndex(char[] charArray, int index, WithRunnableChar runnable) {
        if (hasIndex(charArray, index)) {
            runnable.run(charArray[index]);
        }
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #withIndex(boolean[], int, WithRunnableBoolean)}  method.
     *
     * @see #withIndex(boolean[], int, WithRunnableBoolean)
     */
    public interface WithRunnableBoolean {
        void run(boolean b);
    }

    /**
     * This interface helps facilitate Java 8 / LAMBDA-style programming when calling
     * methods such as the {@link #forEach(boolean[], WithRunnableIndexBoolean)}  method.
     *
     * @see #forEach(boolean[], WithRunnableIndexBoolean)
     */
    public interface WithRunnableIndexBoolean {
        void run(int index, boolean value);
    }

    /**
     * Same as {@link #withIndex(Object[], int, WithRunnable)} but for primitive boolean arrays.
     *
     * @param booleanArray a primitive boolean[] to safely access a value therein
     * @param index the index within booleanArray to access
     * @param runnable a chunk of Java-8 LAMBDA-Style code to be called and passed the index
     *                 as the first parameter and the array value at the specified index.
     */
    public static void withIndex(boolean[] booleanArray, int index, WithRunnableBoolean runnable) {
        if (hasIndex(booleanArray, index)) {
            runnable.run(booleanArray[index]);
        }
    }

    /**
     * Extracts the value from the given {@link List} at the given index and passes the
     * value as the one-and-only argument to the given chunk of Java-8 LAMBDA-style logic if the
     * given {@link List} is non-null, the given chunk of Java-8 LAMBDA-style logic is non-null,
     * the given {@link List} is of sufficient size to include the indicated index, and if the
     * value in the {@link List} at the indicated index is non-null.
     *
     * Example Invocation:
     * <pre>
     *     List&lt;SomeClass&gt; myList = new ArrayList&lt;&gt;();
     *     ...
     *     withIndex(myList, 0, firstItem -&gt; {
     *         // Processes the first item in the list
     *         doSomething(firstItem);
     *     });
     * </pre>
     *
     * @param list a list of objects that is presumably non-null, otherwise an error message is
     *              appended to the error log instead of crashing.
     * @param index the location in the list to access. Presumably the list is of sufficient
     *              length to access this index, otherwise an error message is appended to the
     *              error log instead of crashing.
     * @param runnable a chunk of Java 8 LAMBDA-style logic that takes the corresponding element
     *                 from the given {@code array} as the logic's one-and-only parameter. This
     *                 logic will be invoked and passed the value the value from the given
     *                 {@code array} at the indicated {@code index} if the given {@code array} is
     *                 non-null and if the given {@code array} is of sufficiently allocated length
     *                 to indeed have the indicated {@code index}.
     * @param <T> the element type for the given {@code list}, which the Java compiler implicitly
     *           determines when the given {@code runnable} is provided as a chunk of Java 8
     *           LAMBDA-style logic.
     */
    public static <T> void withIndex(List<T> list, int index, WithRunnable<T> runnable) {
        if (hasIndex(list, index) && isNotNull(runnable)) {
            T obj = list.get(index);
            if (isNotNull(obj)) {
                runnable.run(obj);
            }
        }
    }

    /**
     * Same as {@link #forEach(Object[], WithRunnableIndex)} but for primitive int arrays.
     *
     * @param intArray a primitive int[] to loop over
     * @param runnable a chunk of Java-8 LAMBDA-style logic to invoke. The first parameter is
     *                 the index into the given {@code intArray}. The second parameter is the
     *                 value at the index within the given {@code intArray}.
     */
    public static void forEach(int[] intArray, WithRunnableIndexInt runnable) {
        if (isNotNull(intArray) && isNotNull(runnable)) {
            for (int i = 0; i < intArray.length; i++) {
                runnable.run(i, intArray[i]);
            }
        }
    }

    /**
     * Same as {@link #forEach(Object[], WithRunnableIndex)} but for primitive short arrays.
     *
     * @param shortArray a primitive short[] to loop over
     * @param runnable a chunk of Java-8 LAMBDA-style logic to invoke. The first parameter is
     *                 the index into the given {@code shortArray}. The second parameter is the
     *                 value at the index within the given {@code shortArray}.
     */
    public static void forEach(short[] shortArray, WithRunnableIndexShort runnable) {
        if (isNotNull(shortArray) && isNotNull(runnable)) {
            for (int i = 0; i < shortArray.length; i++) {
                runnable.run(i, shortArray[i]);
            }
        }
    }

    /**
     * Same as {@link #forEach(Object[], WithRunnableIndex)} but for primitive byte arrays.
     *
     * @param byteArray a primitive byte[] to loop over
     * @param runnable a chunk of Java-8 LAMBDA-style logic to invoke. The first parameter is
     *                 the index into the given {@code byteArray}. The second parameter is the
     *                 value at the index within the given {@code byteArray}.
     */
    public static void forEach(byte[] byteArray, WithRunnableIndexByte runnable) {
        if (isNotNull(byteArray) && isNotNull(runnable)) {
            for (int i = 0; i < byteArray.length; i++) {
                runnable.run(i, byteArray[i]);
            }
        }
    }

    /**
     * Same as {@link #forEach(Object[], WithRunnableIndex)} but for primitive char arrays.
     *
     * @param charArray a primitive char[] to loop over
     * @param runnable a chunk of Java-8 LAMBDA-style logic to invoke. The first parameter is
     *                 the index into the given {@code charArray}. The second parameter is the
     *                 value at the index within the given {@code charArray}.
     */
    public static void forEach(char[] charArray, WithRunnableIndexChar runnable) {
        if (isNotNull(charArray) && isNotNull(runnable)) {
            for (int i = 0; i < charArray.length; i++) {
                runnable.run(i, charArray[i]);
            }
        }
    }

    /**
     * Same as {@link #forEach(Object[], WithRunnableIndex)} but for primitive boolean arrays.
     *
     * @param booleanArray the primitive boolean array to safely iterate over
     * @param runnable callback logic to be called for each boolean in the given collection
     */
    public static void forEach(boolean[] booleanArray, WithRunnableIndexBoolean runnable) {
        if (isNotNull(booleanArray) && isNotNull(runnable)) {
            for (int i = 0; i < booleanArray.length; i++) {
                runnable.run(i, booleanArray[i]);
            }
        }
    }

    /**
     * Loops through the given {@code array} and passes each non-null value within the array
     * as the one-and-only argument to the given chunk of Java-8 LAMBDA-style logic if the
     * given {@code array} is non-null and the given chunk of Java-8 LAMBDA-style logic is non-null.
     *
     * Example Invocation:
     * <pre>
     *     List&lt;SomeClass&gt; myList = new ArrayList&lt;&gt;();
     *     ...
     *     forEach(myList, item -&gt; {
     *         doSomething(item);
     *     });
     * </pre>
     *
     * @param array an array of objects that is presumably non-null, otherwise an error message is
     *              appended to the error log instead of crashing.
     * @param runnable a chunk of Java 8 LAMBDA-style logic that will be called repeatedly for each
     *                 non-null object in the given {@code array}. Each non-null object in the
     *                 given array is passed as the logic's one-and-only parameter.
     * @param <T> the element type for the given {@code array}, which the Java compiler implicitly
     *           determines when the given {@code runnable} is provided as a chunk of Java 8
     *           LAMBDA-style logic.
     */
    public static <T> void forEach(T[] array, WithRunnable<T> runnable) {
        if (isNotNull(array) && isNotNull(runnable)) {
            for (T t: array) {
                if (isNotNull(t)) {
                    runnable.run(t);
                }
            }
        }
    }

    /**
     * Loops through the given {@code array} and passes each non-null value within the array
     * as the one-and-only argument to the given chunk of Java-8 LAMBDA-style logic if the
     * given {@code array} is non-null and the given chunk of Java-8 LAMBDA-style logic is
     * non-null. The given chunk of Java-8 LAMBDA-style logic receives the array index as
     * the first parameter and the value at the array index as the second parameter.
     *
     * Example Invocation:
     * <pre>
     *     List&lt;SomeClass&gt; myList = new ArrayList&lt;&gt;();
     *     ...
     *     forEach(myList, index, item -&gt; {
     *         System.out.println("Processing myList[" + index + "] = " + item);
     *         doSomething(item);
     *     });
     * </pre>
     *
     * @param array an array of objects that is presumably non-null, otherwise an error message is
     *              appended to the error log instead of crashing.
     * @param runnable a chunk of Java 8 LAMBDA-style logic that will be called repeatedly for each
     *                 non-null object in the given {@code array}. Each non-null object in the
     *                 given array is passed as the logic's one-and-only parameter.
     * @param <T> the element type for the given {@code array}, which the Java compiler implicitly
     *           determines when the given {@code runnable} is provided as a chunk of Java 8
     *           LAMBDA-style logic.
     */
    public static <T> void forEach(T[] array, WithRunnableIndex<T> runnable) {
        if (isNotNull(array) && isNotNull(runnable)) {
            for (int i = 0; i < array.length; i++) {
                T objectInArray = array[i];
                if (isNotNull(objectInArray)) {
                    runnable.run(i, objectInArray);
                }
            }
        }
    }

    /**
     * Same as {@link #forEach(Object[], WithRunnable)} but for a Java {@link Collection}, which
     * includes all Java {@link List}'s.
     *
     * @param collection the {@link Collection} to safely iterate over
     * @param runnable callback logic to be called for each member in the given collection
     * @param <T> the element type for the given {@code collection}, which the Java compiler
     *           implicitly determines when the given {@code runnable} is provided as a chunk
     *           of Java 8 LAMBDA-style logic.
     */
    public static <T> void forEach(Collection<T> collection, WithRunnable<T> runnable) {
        if (isNotNull(collection) && isNotNull(runnable)) {
            for (T t: collection) {
                if (isNotNull(t)) {
                    runnable.run(t);
                }
            }
        }
    }

    /**
     * Checks that the given {@code obj} is of the given {@code expectedType}, ensuring that the
     * given {@code obj} is non-null, casts the {@code obj} to the given {@code expectedType} and
     * passes the casted result to the given logic {@code r} which can be a chunk of JAVA-8
     * LAMBDA-Style logic.
     *
     * Example Invocation:
     * <pre>
     *     View v  = getView();
     *     ...
     *     withExpectedType(v, RealViewType.class, realView -&gt; {
     *         // The logic here-in has a reference to the value 'v' but as a 'RealViewType'
     *         // reference.
     *         ...
     *     });
     * </pre>
     *
     * @param obj a given object that is presumably non-null and that is presumably safe to cast
     *            to the given {@code expectedType}
     * @param expectedType the polymorphic type to which it is presumably safe to cast the given
     *                     {@code obj}.
     * @param r logic that will be invoked and passed the given {@code obj} but cast as the
     *          given {@code expectedType}. This can be a chunk of JAVA-8 LAMBDA-Style logic.
     *
     * @param <T> the element type for the given {@code obj}, which the Java compiler implicitly
     *           determines when the given {@code runnable} is provided as a chunk of Java 8
     *           LAMBDA-style logic.
     */
    public static<T> void withExpectedType(Object obj, Class<T> expectedType, WithRunnable<T> r) {
        if (isExpectedType(obj, expectedType) && isNotNull(r)) {
            // Let's tell Android Studio not to warn about the following cast since
            // isExpectedType(obj, expectedType) has indeed confirmed that it is safe
            // to cast.
            //
            //noinspection unchecked
            r.run((T) obj);
        }
    }

    /**
     * Similar to {@link #withIndex(char[], int, WithRunnableChar)} but for safely accessing a char
     * within a {@link String} without crashing even if the {@link String} is null or of
     * insufficient length to access the requested char within the {@link String}.
     *
     * Example Invocation:
     * <pre>
     *     String someString = ...;
     *     ...
     *     withCharAt(someString, 0, firstChar -&gt; {
     *         System.out.println("First char in someString = " + firstChar);
     *     });
     * </pre>
     *
     * @param s a {@link String} that is presumably non-null and that presumably is of sufficient
     *          {@link String#length()} to extract the requested {@code index} via the
     *          {@link String#charAt(int)} method.
     * @param index the index of the character to access within the given {@code s}.
     * @param runnable a chunk of Java 8 LAMBDA-style logic that takes the corresponding char
     *                 from the given {@link String} {@code s} as the logic's one-and-only
     *                 parameter. This logic will be invoked and passed the value the char
     *                 from the given {@link String} {@code s} at the indicated {@code index} if
     *                 the given {@link String}{@code s} is non-null and if the given
     *                 {@link String} {@code s} is of sufficiently allocated length to indeed have
     *                 a character at the indicated {@code index}.
     */
    public static void withCharAt(String s, int index, WithRunnableChar runnable) {
        if (hasIndex(s, index) && isNotNull(runnable)) {
            runnable.run(s.charAt(index));
        }
    }

    /**
     * This method is called throughout the Android codebase to ensure that a given object
     * reference is castable to a given type. This method also ensures that the given object
     * reference is not null. If the given object reference is null, the given
     * expectedTypeClass is null, or the given object reference cannot be cast to the
     * expectedTypeClass then an Android Log.e(...) is performed indicating "Please
     * have my program fixed!" and providing a stack trace.
     *
     * @param o reference to an object that this method will ensure is not null.
     * @param expectedTypeClass the class to which to ensure that o is castable
     *
     * @return true if the given object reference is not null, the given expectedTypeClass is
     * not null, and the given object reference is castable to the given expectedTypeClass; or
     * false if the given object is null, the given expectedTypeClass is null, or the given object
     * reference cannot be cast to the given expectedTypeClass.
     */
    public static boolean isExpectedType(Object o,
                                         Class expectedTypeClass) {
        boolean retVal = false;
        if (o == null) {
            log("Unexpected null reference. Please have my program fixed!",
                    new StackRevealer());
        } else if (expectedTypeClass == null){
            log("Null passed in as expectedTypeClass. Please have my program fixed!",
                    new StackRevealer());
        } else if (expectedTypeClass.isInstance(o)){
            retVal = true;
        } else {
            log("Object is the wrong type: Wrong Type: "+
                    o.getClass().getName()+". Expected Type: "+expectedTypeClass.getName()+
                    ". Please have my program fixed!",
                        new StackRevealer());
        }

        return retVal;
    }

    /**
     * Checks that the given collection is large enough to contain the given index
     *
     * @param collection the given collection
     * @param index the given index
     *
     * @return true if the given collection is non-null, the given index is non-negative, and
     * the given collection is large enough to hold the given index; or false if the given
     * collection is null, the given minLength is negative, OR the given collection is not large
     * enough to hold the given index.
     */
    public static boolean hasIndex(final Collection<?> collection, final int index) {
        return helper_hasIndex(collection, index);
    }

    public static boolean hasIndex(final String string, final int index) {
        return helper_hasIndex(string, index);
    }

    /**
     * Checks that the given array is large enough to contain the given index
     *
     * @param array the given array
     * @param index the given index
     *
     * @return true if the given array is non-null, the given index is non-negative, and
     * the given array is large enough to hold the given index; or false if the given array is
     * null, the given minLength is negative, OR the given array is not large enough to hold the
     * given index.
     */
    public static boolean hasIndex(final Object[] array, final int index) {
        return helper_hasIndex(array, index);
    }

    /**
     * Checks that the given array is large enough to contain the given index
     *
     * @param array the given array
     * @param index the given index
     *
     * @return true if the given array is non-null, the given index is non-negative, and
     * the given array is large enough to hold the given index; or false if the given array
     * is null, the given minLength is negative, OR the given array is not large enough to hold
     * the given index.
     */
    public static boolean hasIndex(final Object array, final int index) {
        return helper_hasIndex(array, index);
    }

    /*
     * Helper method that checks whether the a given array or collection has an entry at a
     * given index.
     *
     * This is a Java generic method. Although it may seem complicated to use a Java generic
     * method, this simplifies the following methods by allowing each of the
     * following methods to simply call this one method, rather than having to duplicate logic:
     *
     * <ul>
     *     <li>hasIndex(Object[], int)</li>
     *     <li>hasIndex(Collection&lt;?&gt;, int)</li>
     * </ul>
     *
     * By using a Java generic method, this one method can be called and passed either a collection
     * or an array.
     *
     * @param <T> Type of the given collectionOrArray. This should be a type representing a
     *           Collection or array.
     *
     * @Param T the given collectionOrArray.
     * @param index the index to ensure is a valid index within the given collectionOrArray.
     *
     * @return true if the given collectionOrArray is not null, the given index is not
     * is at least zero, and the size/length of the given collectionOrArray is large enough
     * to contain the given index; or  false if the given collectionOrArray is null, the given
     * index is negative, OR the size/length of the given collectionOrArray too small for the the
     * given collectionOrArray to possibly be able to contain the given index.
     */
    private static <T> boolean helper_hasIndex(final T collectionOrArray, final int index) {
        boolean retVal = false;
        if (index < 0) {
            log("Unable to check for negative index "+index+". Please have my program fixed!",
                    new StackRevealer());
        } else {
            retVal = helper_isMinimumLength(
                    collectionOrArray,
                    index + 1,
                    size -> collectionOrArray.getClass().getSimpleName() +
                            " does not have index " + index + ".");
        }
        return retVal;
    }

    /**
     * Checks that the given array is not empty.
     *
     * @param array the given array
     *
     * @return true if the given array is non-null and the given array's length is at least 1 or
     * false if the given array null, OR the given array's length is less than 1.
     */
    public static boolean isNotEmpty(final Object[] array) {
        return helper_isMinimumLength(
                array,
                1,
                size -> array.getClass().getSimpleName() + " is empty.");
    }

    /**
     * Checks that the given collection is not empty.
     *
     * @param collection the given array
     *
     * @return true if the given collection is non-null and the given collection is not empty or
     * false if the given collection is null, OR the given collection is empty.
     */
    public static boolean isNotEmpty(final Collection<?> collection) {
        boolean retVal = false;
        if (isNotNull(collection)) {
            if (collection.isEmpty()) {
                log(collection.getClass().getSimpleName() + " is empty.",
                        new StackRevealer());
            } else {
                retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Checks that the given array is of the given minimum length.
     *
     * @param array the given array
     * @param minLength the given minimum length
     *
     * @return true if the given array is non-null, the given minLength is non-negative, and
     * the given array's size is at least the given minLength; or false if the given array is
     * null, the given minLength is negative, OR the given array's size is less than the given
     * minLength.
     */
    public static boolean isMinimumLength(final Object[] array, final int minLength) {
        return helper_isMinimumLength(
                array,
                minLength,
                size -> array.getClass().getSimpleName()+ " of length " + size +
                        " is too short (<"+minLength+").");
    }

    /**
     * Checks that the given byte primitiveArray is of the given minimum length.
     *
     * @param primitiveArray the given primitive (byte, short, int, long, float, double, char) array
     * @param minLength the given minimum length
     *
     * @return true if the given primitiveArray is non-null, the given minLength is non-negative,
     * and the given primitiveArray's size is at least the given minLength; or false if the given
     * {@code primitiveArray} is null, the given minLength is negative, OR the given
     * {@code primitiveArray}'s size is less than the given minLength.
     */
    public static boolean isMinimumLength(final Object primitiveArray, final int minLength) {
        return helper_isMinimumLength(
                primitiveArray,
                minLength,
                size -> primitiveArray.getClass().getSimpleName()+ " of length " + size +
                        " is too short (<"+minLength+").");
    }

    /**
     * Checks that the given collection is of the given minimum length.
     *
     * @param collection the given collection
     * @param minLength the given minimum length
     *
     * @return true if the given collection is non-null, the given minLength is non-negative, and
     * the given collection's size is at least the given minLength; or false if the given
     * collection is null, the given minLength is negative, OR the given collection's size is less
     * than the given minLength.
     */
    public static boolean isMinimumLength(final Collection<?> collection, final int minLength) {
        return helper_isMinimumLength(
                collection,
                minLength,
                size -> collection.getClass().getSimpleName() + " of length " + size +
                        " is too short (<"+minLength+").");
    }

    /*
     * Helper method that checks whether the size/length of a given array or collection is at
     * least of a given length. This is a Java generic method. Although it may seem complicated
     * to use a Java generic method, this simplifies the following methods by allowing each of the
     * following methods to simply call this one method, rather than having to duplicate logic:
     * <ul>
     *     <li>isMinimumLength(Object[], int)</li>
     *     <li>isMinimumLength(Collection&lt;?&gt;, int)</li>
     *     <li>isEmpty(Object[])</li>
     *     <li>isEmpty(Collection&lt;?&gt;)</li>
     *     <li>hasIndex(Object[], int)</li>
     *     <li>hasIndex(Collection&lt;?&gt;, int)</li>     *
     * </ul>
     *
     * By using a Java generic method, this one method can be called and passed either a collection
     * or an array.
     *
     * @param <T> Type of the given collectionOrArray. This should be a type representing a
     *           Collection or array.
     *
     * @Param T the given collectionOrArray.
     * @param minLength the minimum length for which to ensure the given collectionOrArray's
     *                  size/length is at least this much.
     * @param messageFormatter an object that generates the warning message that will appear
     *                         in the Android log if the length of the given collectionOrArray
     *                         is less than the given minLength. After this method determines
     *                         that the given collectionOrArray's size/length is too short, this
     *                         object's toString(int) method will be called and passed in the
     *                         known length/size of the given collectionOrArray. The string
     *                         that the messageFormater.toString(int) returns is then placed
     *                         into the Android log as a warning.
     *
     * @return true if the given collectionOrArray is not null, the given messageFormatter is not
     * null, the given minLength is at least zero, the size/length of the given collectionOrArray
     * is non-negative, AND the size/length of the given collectionOrArray is at least the given
     * minLength; or false if the given collectionOrArray is null, the given messageFormatter is null,
     * the given minLength is negative, the size/length of the given collectionOrArray is negative,
     * OR the size/length of the
     * given collectionOrArray is less than the given minLength
     */
    private static <T> boolean helper_isMinimumLength(T collectionOrArray, int minLength,
                                                      MinimumLengthMessageGenerator messageFormatter) {
        boolean retVal = false;

        if (isNotNull(collectionOrArray) && isNotNull(messageFormatter)) {
            if (minLength < 0) {
                log("The given minLength value " + minLength + " is negative. Unable to ensure " +
                                "minimum length. Please have my program fixed!",
                        new StackRevealer());

            } else {
                int size = helper_getSize(collectionOrArray);
                if (size < 0) {
                    log("Unexpected negative size for " +
                                    collectionOrArray.getClass().getSimpleName() +
                                    ". Please have my program fixed!", new StackRevealer());

                } else if (size >= minLength) {
                    retVal = true;
                } else {
                    log(messageFormatter.toString(size),
                            new StackRevealer());
                }
            }
        }
        return retVal;
    }


    /*
     * Helper method called from the generic method helper_isMinimumLength(...) to get
     * the size of the generic/templatized collectionOrArray parameter.
     *
     * @param o an array or a collection
     *
     * @return -1 if parameter o is neither an object array nor a collection; or the size() of
     * the collection if parameter o is a collection; or  the length of the array if parameter
     * o is an array
     */
    private static int helper_getSize(Object o) {
        int retVal = -1;
        if (o instanceof Object[]) {
            retVal = ((Object[]) o).length;
        } else if (o instanceof Collection<?>) {
            retVal = ((Collection<?>) o).size();
        } else if (o instanceof byte[]) {
            retVal = ((byte[]) o).length;
        } else if (o instanceof int[]) {
            retVal = ((int[]) o).length;
        } else if (o instanceof long[]) {
            retVal = ((long[]) o).length;
        } else if (o instanceof boolean[]) {
            retVal = ((boolean[]) o).length;
        } else if (o instanceof float[]) {
            retVal = ((float[]) o).length;
        } else if (o instanceof double[]) {
            retVal = ((double[]) o).length;
        } else if (o instanceof char[]) {
            retVal = ((char[]) o).length;
        } else if (o instanceof short[]) {
            retVal = ((short[]) o).length;
        } else if (o instanceof String) {
            retVal = ((String)o).length();
        } else {
            log("Unable to get size of given object reference since given object " +
                           "reference is neither an array nor a collection. "+
                           "Please have my program fixed!",
                    new StackRevealer());
        }
        return retVal;
    }


    /**
     * Checks that the given int value is greater than or equal to the given boundary. This
     * method is intended to be called from code that fundamentally already believes the value
     * toCheck is greater than or equal to the boundary. This IS NOT intended to be a general
     * replacement for the mathematical &gt;= operation. Instead, this is intended to be used
     * as a safe run-time assertion. "Safe" means that this method does not crash or halt
     * the program if the check fails. Instead, this method simply returns false but also
     * logs the error.
     *
     * @param toCheck: int value to check/ensure is greater than or equal to the given boundary.
     * @param boundary: int value specifying the value that toCheck must be greater than or
     *                equal to.
     *
     * @return true if the given toCheck int value is greater than or equal to the given boundary,
     * false otherwise.
     */
    public static boolean isEqualOrGreater(int toCheck, int boundary) {
        boolean retVal = false;
        if (toCheck < boundary) {
            log("Fundamental value " + toCheck + " should have been >= " + boundary +
                    ". Please have my program fixed!", new StackRevealer());
        } else {
            retVal = true;
        }
        return retVal;
    }

    /**
     * Checks that the given long value is greater than or equal to the given boundary. This
     * method is intended to be called from code that fundamentally already believes the value
     * toCheck is greater than or equal to the boundary. This IS NOT intended to be a general
     * replacement for the mathematical &gt;= operation. Instead, this is intended to be used
     * as a safe run-time assertion. "Safe" means that this method does not crash or halt
     * the program if the check fails. Instead, this method simply returns false but also
     * logs the error.
     *
     * @param toCheck: long value to check/ensure is greater than or equal to the given boundary.
     * @param boundary: long value specifying the value that toCheck must be greater than or
     *                equal to.
     *
     * @return true if the given toCheck long value is greater than or equal to the given boundary,
     * false otherwise.
     */
    public static boolean isEqualOrGreater(long toCheck, long boundary) {
        boolean retVal = false;
        if (toCheck < boundary) {
            log("Fundamental value " + toCheck + " should have been >= " + boundary +
                    ". Please have my program fixed!", new StackRevealer());
        } else {
            retVal = true;
        }
        return retVal;
    }

    /**
     * Checks that the given int value is greater than the given boundary. This
     * method is intended to be called from code that fundamentally already believes the value
     * toCheck is greater the boundary. This IS NOT intended to be a general
     * replacement for the mathematical &gt; operation. Instead, this is intended to be used
     * as a safe run-time assertion. "Safe" means that this method does not crash or halt
     * the program if the check fails. Instead, this method simply returns false but also
     * logs the error.
     *
     * @param toCheck: int value to check/ensure is greater than the given boundary.
     * @param boundary: int value specifying the value that toCheck must be greater than.
     *
     * @return true if the given toCheck int value is greater than the given boundary,
     * false otherwise.
     */
    public static boolean isGreater(int toCheck, int boundary) {
        boolean retVal = false;
        if (toCheck > boundary) {
            retVal = true;
        } else {
            log("Fundamental value " + toCheck + " should have been >= " + boundary +
                    ". Please have my program fixed!", new StackRevealer());
        }
        return retVal;
    }

    /**
     * Checks that the given long value is greater than the given boundary. This
     * method is intended to be called from code that fundamentally already believes the value
     * toCheck is greater than the boundary. This IS NOT intended to be a general
     * replacement for the mathematical &gt; operation. Instead, this is intended to be used
     * as a safe run-time assertion. "Safe" means that this method does not crash or halt
     * the program if the check fails. Instead, this method simply returns false but also
     * logs the error.
     *
     * @param toCheck: long value to check/ensure is greater than the given boundary.
     * @param boundary: long value specifying the value that toCheck must be greater than.
     *
     * @return true if the given toCheck long value is greater than the given boundary,
     * false otherwise.
     */
    public static boolean isGreater(long toCheck, long boundary) {
        boolean retVal = false;
        if (toCheck > boundary) {
            retVal = true;
        } else {
            log("Fundamental value " + toCheck + " should have been > " + boundary +
                    ". Please have my program fixed!", new StackRevealer());
        }
        return retVal;
    }

    /**
     * Checks that the given int value is less than the given boundary. This
     * method is intended to be called from code that fundamentally already believes the value
     * toCheck is less the boundary. This IS NOT intended to be a general
     * replacement for the mathematical &lt; operation. Instead, this is intended to be used
     * as a safe run-time assertion. "Safe" means that this method does not crash or halt
     * the program if the check fails. Instead, this method simply returns false but also
     * logs the error.
     *
     * @param toCheck: int value to check/ensure is less than the given boundary.
     * @param boundary: int value specifying the value that toCheck less than.
     *
     * @return true if the given toCheck int value is less than the given boundary,
     * false otherwise.
     */
    public static boolean isLess(int toCheck, int boundary) {
        boolean retVal = false;
        if (toCheck < boundary) {
            retVal = true;
        } else {
            log("Fundamental value " + toCheck + " should have been < " + boundary +
                    ". Please have my program fixed!", new StackRevealer());
        }
        return retVal;
    }

    /**
     * Checks that the given long value is less than the given boundary. This
     * method is intended to be called from code that fundamentally already believes the value
     * toCheck is less than the boundary. This IS NOT intended to be a general
     * replacement for the mathematical &lt; operation. Instead, this is intended to be used
     * as a safe run-time assertion. "Safe" means that this method does not crash or halt
     * the program if the check fails. Instead, this method simply returns false but also
     * logs the error.
     *
     * @param toCheck: long value to check/ensure is less than the given boundary.
     * @param boundary: long value specifying the value toCheck less than.
     *
     * @return true if the given toCheck long value is less than the given boundary,
     * false otherwise.
     */
    public static boolean isLess(long toCheck, long boundary) {
        boolean retVal = false;
        if (toCheck < boundary) {
            retVal = true;
        } else {
            log("Fundamental value " + toCheck + " should have been < " + boundary +
                    ". Please have my program fixed!", new StackRevealer());
        }
        return retVal;
    }

    /**
     * Checks that the given int value is less than or equal to the given boundary. This
     * method is intended to be called from code that fundamentally already believes the value
     * toCheck is less than or equal to the boundary. This IS NOT intended to be a general
     * replacement for the mathematical &gt;= operation. Instead, this is intended to be used
     * as a safe run-time assertion. "Safe" means that this method does not crash or halt
     * the program if the check fails. Instead, this method simply returns false but also
     * logs the error.
     *
     * @param toCheck: int value to check/ensure is less than or equal to the given boundary.
     * @param boundary: int value specifying the value that toCheck must be less than or
     *                equal to.
     *
     * @return true if the given toCheck int value is less than or equal to the given boundary,
     * false otherwise.
     */
    public static boolean isEqualOrLess(int toCheck, int boundary) {
        boolean retVal = false;
        if (toCheck > boundary) {
            log("Fundamental value " + toCheck + " should have been <= " + boundary +
                    ". Please have my program fixed!", new StackRevealer());
        } else {
            retVal = true;
        }
        return retVal;
    }

    /**
     * Checks that the given long value is less than or equal to the given boundary. This
     * method is intended to be called from code that fundamentally already believes the value
     * toCheck is less than or equal to the boundary. This IS NOT intended to be a general
     * replacement for the mathematical &gt;= operation. Instead, this is intended to be used
     * as a safe run-time assertion. "Safe" means that this method does not crash or halt
     * the program if the check fails. Instead, this method simply returns false but also
     * logs the error.
     *
     * @param toCheck: long value to check/ensure is less than or equal to the given boundary.
     * @param boundary: long value specifying the value that toCheck must be less than or
     *                equal to.
     *
     * @return true if the given toCheck int value is less than or equal to the given boundary,
     * false otherwise.
     */
    public static boolean isEqualOrLess(long toCheck, long boundary) {
        boolean retVal = false;
        if (toCheck > boundary) {
            log("Fundamental value " + toCheck + " should have been <= " + boundary +
                    ". Please have my program fixed!", new StackRevealer());
        } else {
            retVal = true;
        }
        return retVal;
    }
}
