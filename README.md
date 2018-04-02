## Maven to Bazel Generator Tool

This command line tool automates some of the process of migrating projects that use [Maven](https://maven.apache.org/) as a build tool to the [Bazel](https://bazel.build/) build system.
Specifically it processes the Maven dependencies and outputs the following artifacts:
- the Bazel [WORKSPACE](https://docs.bazel.build/versions/master/build-ref.html#workspace) entries that identify the external Maven dependencies in Nexus/Artifactory
- the Bazel [BUILD](https://docs.bazel.build/versions/master/skylark/build-style.html) entries that refer to the external Maven dependencies in the project BUILD file

There are other Maven to Bazel migration tools available, see [this section below](https://github.com/salesforce/maventobazel-generator#why-not-use-the-bazel-supplied-migrationgenerator-tool) for
  why we wrote our own.

### Quick Start

You don't need to copy this tool into your Bazel Workspace - it runs independent of your Workspace.
You just need to build the tool somewhere on your machine, and copy in files that are required for a migration tool run.
The tool is built with Maven.

This is how you run it:
```
# TOOL_DIR is the directory that contains this README

# do this in your Maven project that you are migrating to Bazel
mvn dependency:list > $TOOL_DIR/inputs/my-project-deps.txt

# now run the migration tool
cd $TOOL_DIR
mvn install
java -jar target/maventobazel-generator-1.0.0.jar --workspace
```

The *TOOL_DIR/outputs* directory will now contain migrated artifacts.
Documentation is below for exactly what is produced, and what options you have.

### Examples

You will read below about how to run the tool to work the two use cases.
Before you begin reading that, be aware that there are example available that let you run the tool and show the features.
After reading the use cases below, be sure to try them out.

-  [Examples](examples)

### Use Case 1: Generating the dependencies for the WORKSPACE for migrating Maven projects

This generator tool will take the latest version of each dependency it finds in one or more text files
   and outputs it into a fresh file as an external (i.e. maven_jar) dependency.
The output Skylark file is intended to be loaded by the [WORKSPACE](WORKSPACE) file in the Bazel monorepo.
The output file is loaded by adding this to your WORKSPACE file:

```
# load our migrated dependencies
load("//:external_deps.bzl", "external_maven_jars")
external_maven_jars()
```

This tool is useful as you incrementally bring in more projects into the monorepo.
Each project may be on different versions of various upstream Maven dependencies.
But with Bazel, all projects in the monorepo must be on the same version of each upstream dependency.
Therefore this tool is needed to 'merge' any new/updated dependencies required by a newly migrated project
  into the existing WORKSPACE (which contains the list of Nexus/Artifactory dependencies and versions).

To use this tool:

- Run this command in the Maven version of the Maven project being migrated:  ```mvn -Dsort dependency:list > mydeps.txt```
- Consider preserving the mydeps.txt file in source control as a record of your migration (we don't anymore, but might be helpful)
- Create subdirectories named *inputs* and *outputs* in the migration tool directory if they don't already exist
- Copy your WORKSPACE file (if there are *maven_jar* entries in it), your *external_deps.bzl* file (if you have one already), and the output from step 1 into the *inputs* subdir.
- Build the maventobazel-generator tool (execute from this directory):  ```mvn install```
- Run the maventobazel-generator tool:  ```java -jar  target/maventobazel-generator-1.0.0.jar --workspace```
- Copy the *outputs/external_deps.bzl* file to the root of your workspace.  ```cp external_deps.bzl $YOUR_BAZEL_REPO```
- Add the *load* stanza listed above to the end of your WORKSPACE, if you have not already

If you want a different version of a particular dependency than 'latest', see below how to write Rules to change this.


### Use Case 2: Generating the List of Transitive Dependencies of a Project for a BUILD file

This generator tool will construct the list of the transitive closure of upstream Nexus/Artifactory dependencies for a given Maven project.
The output can then be used in the *deps* attribute of your *java_library* rule in your BUILD file.
It does NOT generate a BUILD file for you - that is a manual step.

To use this tool:

- Run this command in the Maven version of the project being migrated:  ```mvn -Dsort dependency:list > mydeps.txt```
- Create a subdirectories named *inputs* and *outputs* in the migration tool dir if they don't already exist
- Copy the file from step 1 into the *inputs* directory
- Build the maventobazel-generator tool (execute from this directory):  ```mvn install```
- Run this generator tool:  ```java -jar  target/maventobazel-generator-1.0.0.jar --build```
- Copy the contents of *outputs/BUILD.out* and use as the *deps* attribute in your *java_library* rule in your BUILD file

### Dependency Arbiter Rules

When you are merging in a new Maven project into your existing WORKSPACE, there may be a version conflict with one or more external dependencies.
This is solved by choosing the later (higher) version of the dependency by default if the dependency follows the SemVer standard.
For example, 3.1.0 of a dependency will be preferred over 2.5.7.
You can override the default SemVer choice by adding Rules as explained below.

The default logic assumes your dependency follows [SemVer](https://semver.org/) version rules.
If the tool cannot parse a SemVer version from a dependency version, the tool will complain and exit.
You will need to write a Rule to help the tool for those cases.
You can also write Rules when you want to override the default choice.

Rules are written to a text file and put in the *inputs* directory.
Rules files have this format:

```
# for any dep from org.green with artifactId ending in -transport, use version 1.5.0
# RULE groupId=org.green artifactId=.*-transport pinnedVersion=1.5.0

# for any dep from org.red, always pick a version with -patched if available
# RULE groupId=org.red winningVersion=.*-patched  

# for any dep from org.yellow, always pick a version with -acme as a suffix (e.g. my company has its own versions)
# RULE groupId=org.yellow winningVersion=.*-acme  

```

Details:
- groupId must always be specified
- groupId and artifactId use regex for matching
- the 'pinnedVersion' format is used to override any matching logic
- the 'winningVersion' format is a regex, and if a dependency version matches it will be chosen over the other version
- rules are executed in 'first matched rule wins' order, so put all of them in the same text file for consistent behavior
- you can actually embed rules into any file in the *inputs* directory (e.g. your external_deps.bzl file) if that is more convenient

### Why Not Use the Bazel Supplied Migration/Generator Tool?

Bazel provides a [migration tool](https://github.com/bazelbuild/migration-tooling).
Maybe it works for you.

There were several drawbacks that we hit with it:

- There [is a bug](https://github.com/bazelbuild/migration-tooling/issues/47) that gets triggered by migrating one of our projects. There is a proposed fix for that bug in a fork, but even the proposed fix doesn't resolve the problem for us.
- The tool does not allow you to merge new dependencies into an existing [WORKSPACE](../../WORKSPACE) - it creates a single WORKSPACE from a single Maven project. We need a merge feature, as we will migrate Maven projects incrementally, one at a time, from Maven to Bazel.

We wrote this tool as an alternative to the Bazel supplied tool.
It takes a different approach.
Notably, the input into this tool is not your Maven pom.xml, it is the output of running ```mvn dependency:list``` in your Maven project.
This eliminates the need for Aether.
In addition, it can parse WORKSPACE files, which allows it to do smart merges when enforcing the Single Version Policy of a monorepo.

There is another migration tool [bazel-deps](https://github.com/johnynek/bazel-deps) which has a similar feature set of this tool.
The main differences with this tool as compared to *bazel-deps* are:
- It is written in Java not Scala
- It does not use Aether. By using a file to list the migrating dependencies, we have a history of the inputs into the migration.
- It does not build transitive constructs in the WORKSPACE or BUILD files, all the dependencies are flat. We went with a no-transitive approach to dependencies in the beginning for a reason that is no longer relevant, but the approach remains. This may or may not be a good thing.


### License

Copyright (c) 2018, salesforce.com, inc.
All rights reserved.
Licensed under the BSD 3-Clause license.
For full license text, see [LICENSE.txt](LICENSE.txt) file or [https://opensource.org/licenses/BSD-3-Clause](https://opensource.org/licenses/BSD-3-Clause)
