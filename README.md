# soft-assertions

A library of soft assertions to help you write safer Java code and minimize crashes. This
repository takes advantage of JitPack's feature whereby JitPack automatically builds and
serves open source libraries whose code is available publicly in GitHub.

To install the library in your code, simply add the following to your build.gradle file: 
 
   ```gradle
   repositories { 
        ...
        maven { url "https://jitpack.io" }
        ...
   }
   dependencies {
         implementation 'com.github.softassertions:soft-assertions:1.0.0'
   }
   ```  
## Usage
After adding the jitpack repository and the soft-assertions dependency to your gradle build process you can then access the various static methods on the SoftAssertions class. For example, add the following static import statement to one of your Java files:
```
import static com.github.softassertions.SoftAssertions.isNotNull;
```
 
And then you can write `if (isNotNull(myVar)) {...}` anywhere that you'd like to safely assert for example that your variable named myVar is not null. When executed, a log message will be printed and `isNotNull(myVar)` returns false if myVar is null. Otherwise `isNotNull(myVar)` evaluates to true and the logic in the if statement's block is executed.

The idea is that often times as developers we believe that one of our variables is not null and safe to access. Nevertheless, being Java code, pretty much any variable can hypotherically be null, especially in negative use-case scenarios. When this happens our code crashes with a NullPointerException unless we wrap our code in some form of `if` statement that first checks for null prior to de-referencing. The SoftAssertions library intends to help ease the burden in these types of scenarios. See the `SoftAssertions` class for all of the available soft assertions and related helper methods, such as help with:

* Null Pointer Checking: `if (isNotNull(myVar)) { ... }` and `withNotNull(getMyVar(), myVar -> { ... } )`
* Array Out of Bounds Checking: `if (hasIndex(myArray)) { ... }` and `withIndex(myArray, index, value -> { ... });`
* Safe array looping, even if the array is null or has null items: `forEach(myArray, (index, value) -> { ... });` or `forEach(myArray, value -> { ... })`
* Safe type checking prior to type casting: `if (isExpectedType(myVar, MyType.class)) { myVarOfType = (MyType)var; ... }` and `withExpectedType(myVar, MyType.class, v -> { ... })`

## Static Initialization (Optional)
Out-of-the-box the SoftAssertions library simply prints to stderr and either returns false or skips the given callback whenever an assertion fails. You can plug in your own logger from somewhere early in your code's startup logic:

```
import com.github.softassertions.SoftAssertions;

import static com.your.chosen.logger.LoggerClass.Log;
...

void yourStartupLogicThatYourApplicationPresumablyCallsAtStartup() {
   SoftAssertions.setLogger( (msg, t) -> {Log.e(LOG_TAG, msg, t);});
}
```

Not Typical but Known Issue: Be careful, for example, on Android if your app uses multiple processes any one call to `SoftAssertions.setLogger(...)` only assigns the logger in the process in which the code is executing. If you have an activity or service, for example, whereby you declare in the manifest that the activity or service executes in its own separate process, then you'll need to perform the call to `SoftAssertions.setLogger(...)` early in that special activity or service's `onCreate(...)` logic.
