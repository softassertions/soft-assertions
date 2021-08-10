# soft-assertions

A library of soft assertions to help you write safer Java code and minimize crashes. This
repository takes advantage of JitPack's feature whereby JitPack automatically builds and
serves open source libraries whose code is available publicly in GitHub.

To install the library in your code, simply add the following to your build.gradle file: 
 
   ```gradle
   repositories { 
        jcenter()
        maven { url "https://jitpack.io" }
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

The idea is that often times as developers we believe that one of our variables is not null and safe to access. Nevertheless, being Java code, pretty much any variable can hypotherically be null, especially in negative use-case scenarios. When this happens our code crashes with a NullPointerException unless we wrap our code in some form of `if` statement that first checks for null prior to de-referencing. The SoftAssertions library intends to help ease the burden in these types of scenarios. See the `SoftAssertions` class for all of the available soft assertions and related helper methods.
