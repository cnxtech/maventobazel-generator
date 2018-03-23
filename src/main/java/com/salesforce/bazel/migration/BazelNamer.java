/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

/**
 * Container for Bazel naming utilities.
 *  
 * @author plaird
 */
public class BazelNamer {
    
	/**
	 * Constructs a consistent logical name for a Maven dependency that will be used to name the 
	 * dependency in the WORKSPACE file, and refer to that dependency in a BUILD file.
	 * <p>
	 * <b>Examples</b>
	 * <ul>
	 * <li>com.sample:foo => com_sample_foo
	 * <li>com.sample:foo:test-jar => com_sample_foo_test_jar
	 * <li>com.sample:foo:idl => com_sample_foo_idl
	 * </ul>
	 * 
	 * @param dep the Maven dependency
	 * @return the String Bazelized name
	 */
	public static String computeBazelName(MavenDependency dep) {
        String name = dep.getLogicalName();
        name = name.replace("-", "_");
        name = name.replace(".", "_");
        name = name.replace(":", "_");
        return name;
    }
	
	/**
	 * Constructs the Maven artifact name for use in a Bazel maven_jar rule that is used to download the
	 * artifact from Nexus/Artifactory.
	 * <p>
	 * <b>Examples</b>
	 * <ul>
	 * <li>Standard:  io.netty:netty-transport:4.1.16.Final
	 * <li>With Classifier:  io.netty:netty-transport-native-epoll:jar:linux-x86_64:4.1.8.Final
	 * </ul>
	 * 
	 * @param dep the Maven dependency
	 * @return the String Bazelized artifact Maven name
	 */
	public static String computeBazelMavenName(MavenDependency dep) {
		StringBuilder sb = new StringBuilder();
		sb.append(dep.groupId);
		sb.append(":");
		sb.append(dep.artifactId);
		sb.append(":");
		if (dep.classifier != null) {
			sb.append("jar:");
			sb.append(dep.classifier);
			sb.append(":");
		}
		sb.append(dep.version.label);
        return sb.toString();
	}
}
