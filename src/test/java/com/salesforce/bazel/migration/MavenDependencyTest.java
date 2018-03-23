/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MavenDependencyTest {
	
	@Test
	public void testStandardCompileDep() {
		MavenDependency dep = new MavenDependency("parsed dep line of text", "com.sample", "foo", "compile", "1.2.3");
		assertEquals("parsed dep line of text", dep.originalLine);
		assertEquals("com.sample", dep.groupId);
		assertEquals("foo", dep.artifactId);
		assertEquals(MavenDependency.Scope.COMPILE, dep.scope);
		assertEquals("1.2.3", dep.version.label);
		assertEquals("com.sample:foo", dep.version.groupIdArtifactIdForLog);
		assertEquals(1, dep.version.majorVersion);
		assertEquals(2, dep.version.minorVersion);
		assertEquals(3, dep.version.patchVersion);
		assertNull(dep.classifier);
	}

	@Test
	public void testClassifierProvidedDep() {
		MavenDependency dep = new MavenDependency("parsed dep line of text", "com.sample", "foo", "provided", "1.2", "idl");
		assertEquals("parsed dep line of text", dep.originalLine);
		assertEquals("com.sample", dep.groupId);
		assertEquals("foo", dep.artifactId);
		assertEquals(MavenDependency.Scope.PROVIDED, dep.scope);
		assertEquals("1.2", dep.version.label);
		assertEquals("com.sample:foo", dep.version.groupIdArtifactIdForLog);
		assertEquals(1, dep.version.majorVersion);
		assertEquals(2, dep.version.minorVersion);
		assertEquals("idl", dep.classifier);
	}

	@Test
	public void testClassifierTestDep() {
		MavenDependency dep = new MavenDependency("parsed dep line of text", "com.sample", "foo", "test", "1.2.3.4", "test-jar");
		assertEquals("parsed dep line of text", dep.originalLine);
		assertEquals("com.sample", dep.groupId);
		assertEquals("foo", dep.artifactId);
		assertEquals(MavenDependency.Scope.TEST, dep.scope);
		assertEquals("1.2.3.4", dep.version.label);
		assertEquals("com.sample:foo", dep.version.groupIdArtifactIdForLog);
		assertEquals(1, dep.version.majorVersion);
		assertEquals(2, dep.version.minorVersion);
		assertEquals(3, dep.version.patchVersion);
		assertEquals(4, dep.version.hotfixVersion);
		assertEquals("test-jar", dep.classifier);
	}
}
