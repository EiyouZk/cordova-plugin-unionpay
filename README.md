# cordoav-plugin-unionpay

cordova plugin add https://github.com/EiyouZk/cordova-plugin-unionpay.git

##错误处理：
Android studio报如下错误：
Error:org.gradle.api.internal.tasks.DefaultTaskInputs$TaskInputUnionFileCollection cannot be cast to org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection
Possible causes for this unexpected error include:<ul><li>Gradle's dependency cache may be corrupt (this sometimes occurs after a network connection timeout.)
<a href="syncProject">Re-download dependencies and sync project (requires network)</a></li><li>The state of a Gradle build process (daemon) may be corrupt. Stopping all Gradle daemons may solve this problem.
<a href="stopGradleDaemons">Stop Gradle build processes (requires restart)</a></li><li>Your project may be using a third-party plugin which is not compatible with the other plugins in the project or the version of Gradle requested by the project.</li></ul>In the case of corrupt Gradle processes, you can also try closing the IDE and then killing all Java processes.

处理方案：打开项目目录／gradle／gradle－wrapper.properties 文件 
         把distributionUrl=https\://services.gradle.org/distributions/gradle-3.3-all.zip 中的gradle-3.3-all.zip替换成其它可运行项目的压缩包（gradle-2.14.1-all.zip）

原因：android studio中gradle的版本导致
