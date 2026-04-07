@echo off
setlocal enabledelayedexpansion

set REPO=%USERPROFILE%\.m2\repository
set CP=target\classes

rem Spring Boot Core
set CP=%CP%;%REPO%\org\springframework\boot\spring-boot\3.2.4\spring-boot-3.2.4.jar
set CP=%CP%;%REPO%\org\springframework\boot\spring-boot-autoconfigure\3.2.4\spring-boot-autoconfigure-3.2.4.jar

rem Spring Framework
set CP=%CP%;%REPO%\org\springframework\spring-web\6.1.5\spring-web-6.1.5.jar
set CP=%CP%;%REPO%\org\springframework\spring-webmvc\6.1.5\spring-webmvc-6.1.5.jar
set CP=%CP%;%REPO%\org\springframework\spring-context\6.1.5\spring-context-6.1.5.jar
set CP=%CP%;%REPO%\org\springframework\spring-core\6.1.5\spring-core-6.1.5.jar
set CP=%CP%;%REPO%\org\springframework\spring-beans\6.1.5\spring-beans-6.1.5.jar
set CP=%CP%;%REPO%\org\springframework\spring-aop\6.1.5\spring-aop-6.1.5.jar
set CP=%CP%;%REPO%\org\springframework\spring-expression\6.1.5\spring-expression-6.1.5.jar
set CP=%CP%;%REPO%\org\springframework\spring-jcl\6.1.5\spring-jcl-6.1.5.jar

rem Jakarta
set CP=%CP%;%REPO%\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar

rem Logging
set CP=%CP%;%REPO%\ch\qos\logback\logback-classic\1.4.14\logback-classic-1.4.14.jar
set CP=%CP%;%REPO%\ch\qos\logback\logback-core\1.4.14\logback-core-1.4.14.jar
set CP=%CP%;%REPO%\org\slf4j\slf4j-api\2.0.12\slf4j-api-2.0.12.jar
set CP=%CP%;%REPO%\org\slf4j\jul-to-slf4j\2.0.12\jul-to-slf4j-2.0.12.jar

rem Micrometer
set CP=%CP%;%REPO%\io\micrometer\micrometer-observation\1.12.4\micrometer-observation-1.12.4.jar
set CP=%CP%;%REPO%\io\micrometer\micrometer-commons\1.12.4\micrometer-commons-1.12.4.jar

rem Tomcat Embed
for /r "%REPO%\org\apache\tomcat\embed" %%f in (tomcat-embed-core-*.jar) do set CP=!CP!;%%f
for /r "%REPO%\org\apache\tomcat\embed" %%f in (tomcat-embed-el-*.jar) do set CP=!CP!;%%f
for /r "%REPO%\org\apache\tomcat\embed" %%f in (tomcat-embed-websocket-*.jar) do set CP=!CP!;%%f

rem Jackson
for /r "%REPO%\com\fasterxml\jackson\core" %%f in (jackson-core-*.jar) do if not "%%~nf"=="%%~nf-sources" set CP=!CP!;%%f
for /r "%REPO%\com\fasterxml\jackson\core" %%f in (jackson-databind-*.jar) do if not "%%~nf"=="%%~nf-sources" set CP=!CP!;%%f
for /r "%REPO%\com\fasterxml\jackson\core" %%f in (jackson-annotations-*.jar) do if not "%%~nf"=="%%~nf-sources" set CP=!CP!;%%f
for /r "%REPO%\com\fasterxml\jackson\datatype" %%f in (jackson-datatype-jsr310-*.jar) do set CP=!CP!;%%f
for /r "%REPO%\com\fasterxml\jackson\datatype" %%f in (jackson-datatype-jdk8-*.jar) do set CP=!CP!;%%f
for /r "%REPO%\com\fasterxml\jackson\module" %%f in (jackson-module-parameter-names-*.jar) do set CP=!CP!;%%f

rem SnakeYAML
for /r "%REPO%\org\yaml" %%f in (snakeyaml-*.jar) do set CP=!CP!;%%f

echo Starting Library Positioning Backend...
java -cp "%CP%" com.library.LibraryApplication
