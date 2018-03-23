/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

/**
 * Takes in a set of deduplicated {@link MavenDependency} objects, and writes out the <b>deps</b> list for use in a 
 * Bazel BUILD file.
 * This allows migrating projects to easily generate the full list of transitive dependencies, and insert into a BUILD file
 * using the Bazel naming convention.
 * <p>
 * <b>Input</b>
 * <p>
 * A list of {@link MavenDependency} POJOs, typically obtained from parsing the output of a <i>mvn dependency:list</i> 
 * command using the {@link DependenciesParser} class. 
 * <p>
 * <b>Output</b>
 * <p>
 * A file with the the dependencies written in Bazel BUILD file form, one line per dependency. For example:
 * <pre>
    "@com_fasterxml_jackson_core_jackson_annotations//jar",
    "@com_fasterxml_jackson_core_jackson_core//jar",
    "@com_fasterxml_jackson_core_jackson_databind//jar",
    "@com_fasterxml_jackson_jaxrs_jackson_jaxrs_base//jar",
    "@com_fasterxml_jackson_jaxrs_jackson_jaxrs_json_provider//jar",
   </pre>
 * <p>
 * <b>Usage Notes:</b>
 * <p>
 * 1. Note that the output of this is <b>not</b> a usable BUILD file. It is just the <i>deps</i> list that you can plug into
 * a BUILD file (that you manually migrated).
 * <p>
 * 2. If any of the dependencies (e.g. an upstream library) is also moving into the Bazel WORKSPACE, you will need to manually
 * edit the dependency line. For example, assume the migrating Maven project consumes a library <i>com.sample:foo</i> from Nexus/Artifactory.
 * This tool will write out the dependency like this: <i>@com_sample_foo//jar</i> which is an external dependency. But 
 * if the library has also been migrated into the workspace at <i>//libs/foo</i>, you will need to manually update the output of
 * this tool to replace <i>@com_sample_foo//jar</i> with <i>//libs/foo</i>.
 * 
 * @author plaird
 */
public class BazelBuildDependenciesGenerator {
	/**
	 * Writes the list of dependencies in BUILD file form, as described in the class-level Javadoc.
	 * 
	 * @param dependencies the list of dependencies 
	 * @param outputFile the file that will contain the list of dependencies written in BUILD file form
	 * @throws Exception
	 */
    public void writeDependenciesAsBuildFile(Map<String, MavenDependency> dependencies, File outputFile)  throws Exception {
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter(outputFile);
            bw = new BufferedWriter(fw);
            bw.write("# this is a list of dependencies to copy into your deps attribute in your Java target's BUILD file\n\n");

            // write a @
            for (MavenDependency dep : dependencies.values()) {
                bw.write("  \"@");
                bw.write(BazelNamer.computeBazelName(dep));
                bw.write("//jar\",\n");
            }

            System.out.println("Wrote Bazel partial BUILD file "+outputFile.getAbsolutePath());
        } finally {
            if (bw != null) bw.close();
            if (fw != null) fw.close();
        }
    }
}
