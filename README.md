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
         implementation 'com.github.softassertions:soft-assertions:1.0'
   }
   ```  

