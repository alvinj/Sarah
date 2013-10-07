How to Build the Mac App (Latest)
---------------------------------

This is a little convoluted, but:

1 First run 'sbt compile' in the root directory of the project.
2 cd into the 'custom' directory.
3 Run 'ant'. This builds release/Sarah.app under custom.

Notes
-----

I was using sbt-assembly, but I ran into name collisions that 
would probably keep the Java 'mail' jars from working.

The custom/build.xml file requires JAVA_HOME to be set.

The custom/build.xml file need some love.

Requirements (AppBundler)
-------------------------

The Ant build process requires the AppBundler jar to work
on OS X 10.7 and newer along with Java 7 and newer. Here's
some info:

* https://java.net/projects/appbundler/pages/Home
* https://java.net/downloads/appbundler/appbundler.html
* http://intransitione.com/blog/take-java-to-app-store/ (very helpful)
* http://docs.oracle.com/javase/7/docs/technotes/guides/jweb/packagingAppsForMac.html

Assuming this process works, you can safely ignore everything else 
that I've written below.


Old Instructions for Using sbt-assembly
---------------------------------------

The instructions below discuss how to build the application with
sbt-assembly. As long as the directions above work properly, there's
no need to worry about the stuff below.


HOW TO BUILD THE MAC APP
------------------------

1) First run "sbt assembly" in the project's root directory.
   That creates a single JAR file for the entire application.
   (More on this shortly.)

2) Move into the 'custom' directory. Make sure JAVA_HOME is set
   properly, and then run "ant". This should create a 'Sarah.app' subdirectory
   under 'custom'.

IMPORTANT NOTES
---------------

* I configured sbt-assembly in the build.sbt file to handle problems where several
  jars contain the same filenames and paths. This may cause problems. The email jars
  are the primary culprits.

* The 'custom' directory was changed because I need to use 'AppBundler' with Java 7
  instead of the older JarBundler, which I used previously. (JarBundler won't work with
  Java 7.)

* sbt-assembly 'Merge' notes: This URL was helpful:
  http://nicta.github.io/scoobi/guide/com.nicta.scoobi.guide.Deployment.html

HOW TO INSTALL SARAH
--------------------

* At this time the install files are in the Install subdirectory of this directory.
  This is still a work in progress, but I'm aiming to put Sarah's data files in this dir:
  $HOME/Library/Application Support/devdaily.com/Sarah


