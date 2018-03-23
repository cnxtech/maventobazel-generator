/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MavenDependencyVersionTest {

	@Test
	public void testFullSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("1.2.3.4");
        assertEquals(1, ver.majorVersion);
        assertEquals(2, ver.minorVersion);
        assertEquals(3, ver.patchVersion);
        assertEquals(4, ver.hotfixVersion);
        assertTrue(ver.isSemVer);
	}

	@Test
	public void testMajorSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("1");
        assertEquals(1, ver.majorVersion);
        assertEquals(0, ver.minorVersion);
        assertEquals(0, ver.patchVersion);
        assertEquals(0, ver.hotfixVersion);
        assertTrue(ver.isSemVer);
	}

	@Test
	public void testMajorMinorSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("1.2");
        assertEquals(1, ver.majorVersion);
        assertEquals(2, ver.minorVersion);
        assertEquals(0, ver.patchVersion);
        assertEquals(0, ver.hotfixVersion);
        assertTrue(ver.isSemVer);
	}
	
	@Test
	public void testMajorMinorPatchSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("1.2.3");
        assertEquals(1, ver.majorVersion);
        assertEquals(2, ver.minorVersion);
        assertEquals(3, ver.patchVersion);
        assertEquals(0, ver.hotfixVersion);
        assertTrue(ver.isSemVer);
	}
	
	// Non SemVer tests

	@Test
	public void testNonNumericNotSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("myversion");
        assertEquals(0, ver.majorVersion);
        assertEquals(0, ver.minorVersion);
        assertEquals(0, ver.patchVersion);
        assertEquals(0, ver.hotfixVersion);
        assertFalse(ver.isSemVer);
	}

	@Test
	public void testSnapshotNotSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("1.2.3-SNAPSHOT");
        assertEquals(1, ver.majorVersion);
        assertEquals(2, ver.minorVersion);
        assertEquals(0, ver.patchVersion);
        assertEquals(0, ver.hotfixVersion);
        assertFalse(ver.isSemVer);
	}
	
	@Test
	public void testTextTokenNotSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("1.2.heartbleed");
        assertEquals(1, ver.majorVersion);
        assertEquals(2, ver.minorVersion);
        assertEquals(0, ver.patchVersion);
        assertEquals(0, ver.hotfixVersion);
        assertFalse(ver.isSemVer);
	}

	@Test
	public void testNumberAndTextTokenNotSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("1.2.3-heartbleed");
        assertEquals(1, ver.majorVersion);
        assertEquals(2, ver.minorVersion);
        assertEquals(0, ver.patchVersion);
        assertEquals(0, ver.hotfixVersion);
        assertFalse(ver.isSemVer);
	}

	@Test
	public void testExtraNumbersNotSemVer() {
		MavenDependencyVersion ver = new MavenDependencyVersion("1.2.3.4.5");
        assertEquals(1, ver.majorVersion);
        assertEquals(2, ver.minorVersion);
        assertEquals(3, ver.patchVersion);
        assertEquals(4, ver.hotfixVersion);
        assertFalse(ver.isSemVer);
	}
	
}
