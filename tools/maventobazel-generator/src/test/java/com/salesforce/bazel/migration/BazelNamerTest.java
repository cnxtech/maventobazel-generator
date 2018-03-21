/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BazelNamerTest {

	@Test
	public void testNaming() {
        MavenDependency dep = new MavenDependency("test parsed line", "org.sample", "foo-bar", "compile", "4.1.8");
        String bazelName = BazelNamer.computeBazelName(dep);
        String mavenName = BazelNamer.computeBazelMavenName(dep);
        assertEquals("org_sample_foo_bar", bazelName);
        assertEquals("org.sample:foo-bar:4.1.8", mavenName);
	}
	
	@Test
	public void testNamingWithClassifier() {
        MavenDependency dep = new MavenDependency("test parsed line", "org.sample", "foo-bar", "compile", "4.1.8", "my-test_classifier");
        String bazelName = BazelNamer.computeBazelName(dep);
        String mavenName = BazelNamer.computeBazelMavenName(dep);
        assertEquals("org_sample_foo_bar_my_test_classifier", bazelName);
        assertEquals("org.sample:foo-bar:jar:my-test_classifier:4.1.8", mavenName);
	}
	
	@Test
	public void testNamingWithNonSemVersion() {
        MavenDependency dep = new MavenDependency("test parsed line", "org.sample", "foo-bar", "compile", "4.1.8.Final", "my-test_classifier");
        String bazelName = BazelNamer.computeBazelName(dep);
        String mavenName = BazelNamer.computeBazelMavenName(dep);
        assertEquals("org_sample_foo_bar_my_test_classifier", bazelName);
        assertEquals("org.sample:foo-bar:jar:my-test_classifier:4.1.8.Final", mavenName);
	}
}
