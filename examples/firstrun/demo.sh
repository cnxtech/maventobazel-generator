# Hello World example for the Salesforce Bazel migration tool

# first do the dependency:list analysis on the sample Maven project
echo ""
echo "**********************************************************************************"
echo "STEP 1: Generating the dependency list for the sample my-maven-project"
pushd .
cd my-maven-project
mvn -Dsort dependency:list > ../inputs/my-maven-project-deps.txt
popd
cat inputs/my-maven-project-deps.txt

# build the migration tool
pushd .
cd ../..
echo ""
echo "**********************************************************************************"
echo "STEP 2: Building the migration tool"
mvn -DskipTests install
popd

# run the migration tool to generate the WORKSPACE entries for the external deps
echo ""
echo "**********************************************************************************"
echo "STEP 3: generate the file that includes all the external dependencies from my-maven-project for the WORKSPACE"
java -jar ../../target/maventobazel-generator-1.0.0.jar --workspace
cp outputs/external_deps.bzl.out ../external_deps.bzl.firstrun
cat outputs/external_deps.bzl.out


# run the migration tool to generate the BUILD entries for the external deps
echo ""
echo "**********************************************************************************"
echo "STEP 4: generate the file that lists all the external dependencies from my-maven-project for the BUILD file"
java -jar ../../target/maventobazel-generator-1.0.0.jar --build
cat outputs/BUILD.out
