# maventobazel Migration Tool Examples

This directory contains two examples, to show two common situations.

### First Run

This example shows how to get started by migrating your first Maven project to Bazel with the tool.
It analyzes a sample Maven project [my-maven-project](firstrun/my-maven-project) and produces:
- the WORKSPACE file entries for the external dependencies
- the BUILD file list of dependencies for the project (the actual BUILD file is left as an exercise)

To run:
- cd [firstrun](firstrun)
- ./demo.sh

You are meant to read the demo.sh script to see how to iterate with the tool to migrate your project.

### Merge Run

This example shows how to migrate a second (or third, or fourth...) project using the tool.
This includes the situation where the tool must pick between upgrading the external dependency in the WORKSPACE, or retaining the existing version.
This execution also shows how to write rules to pick the right dependency.

This demo assumes you have successfully run the first demo, and there is now a file *external_deps.bzl.firstrun* at the root of the Git repo
 (the *firstrun* demo script will copy it there).

 To run:
 - cd [mergerun](mergerun)
 - ./demo.sh
