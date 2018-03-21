/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class DependencyAnalyzerTest {
	MavenDependency dep1 = new MavenDependency("parsed dep line of text", "com.sample", "foo", "compile", "1.2.3");
	MavenDependency dep1_major = new MavenDependency("parsed dep line of text", "com.sample", "foo", "compile", "2.0.0");
	MavenDependency dep1_minor = new MavenDependency("parsed dep line of text", "com.sample", "foo", "compile", "1.3.0");
	MavenDependency dep1_patch = new MavenDependency("parsed dep line of text", "com.sample", "foo", "compile", "1.2.5");
	MavenDependency dep1_hotfix = new MavenDependency("parsed dep line of text", "com.sample", "foo", "compile", "1.2.3.1");
	MavenDependency dep2 = new MavenDependency("parsed dep line of text", "com.sample", "bar", "compile", "4.5.6");
	MavenDependency greenDep = new MavenDependency("parsed dep line of text", "com.green", "baz", "compile", "7.8.9");
	MavenDependency greenDep_patched = new MavenDependency("parsed dep line of text", "com.green", "baz", "compile", "7.8.9-patched");
	MavenDependency greenDep_oldpatched = new MavenDependency("parsed dep line of text", "com.green", "baz", "compile", "6.7.9-patched");

	private MavenDependencyArbiter arbiter = new MavenDependencyArbiter();
	
	@Test
	public void testNoConflicts() {
		List<MavenDependency> inputDependencies = new ArrayList<>();
		inputDependencies.add(dep1);
		inputDependencies.add(dep2);
		
		
		DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
		Map<String, MavenDependency> processedDeps = analyzer.processDependencies(inputDependencies, false);
		
		assertEquals(2, processedDeps.size());
		assertVersion(processedDeps, "1.2.3", dep1);
		assertVersion(processedDeps, "4.5.6", dep2);
	}

	@Test
	public void testConflictMajor() {
		List<MavenDependency> inputDependencies = new ArrayList<>();
		inputDependencies.add(dep1);
		inputDependencies.add(dep1_major);
		
		
		DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
		Map<String, MavenDependency> processedDeps = analyzer.processDependencies(inputDependencies, false);
		
		assertEquals(1, processedDeps.size());
		assertVersion(processedDeps, "2.0.0", dep1);
	}

	@Test
	public void testConflictMinor() {
		List<MavenDependency> inputDependencies = new ArrayList<>();
		inputDependencies.add(dep1);
		inputDependencies.add(dep1_minor);
		
		
		DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
		Map<String, MavenDependency> processedDeps = analyzer.processDependencies(inputDependencies, false);
		
		assertEquals(1, processedDeps.size());
		assertVersion(processedDeps, "1.3.0", dep1);
	}

	@Test
	public void testConflictPatch() {
		List<MavenDependency> inputDependencies = new ArrayList<>();
		inputDependencies.add(dep1);
		inputDependencies.add(dep1_patch);
		
		
		DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
		Map<String, MavenDependency> processedDeps = analyzer.processDependencies(inputDependencies, false);
		
		assertEquals(1, processedDeps.size());
		assertVersion(processedDeps, "1.2.5", dep1);
	}

	@Test
	public void testConflictHotfix() {
		List<MavenDependency> inputDependencies = new ArrayList<>();
		inputDependencies.add(dep1);
		inputDependencies.add(dep1_hotfix);
		
		
		DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
		Map<String, MavenDependency> processedDeps = analyzer.processDependencies(inputDependencies, false);
		
		assertEquals(1, processedDeps.size());
		assertVersion(processedDeps, "1.2.3.1", dep1);
	}

	@Test
	public void testFailOnConflict_NotEnoughInfoForAutoCompare() {
		List<MavenDependency> inputDependencies = new ArrayList<>();
		// one version is SemVer, the other two are non-SemVer, so auto compare should fail
		inputDependencies.add(greenDep);
		inputDependencies.add(greenDep_patched);
		inputDependencies.add(greenDep_oldpatched);
		
		
		DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
		try {
			analyzer.processDependencies(inputDependencies, false);
			fail("The arbiter should not try to compare non-SemVer format versions, without help from a Rule");
		} catch (IllegalStateException ie) {
			// expected
		}
	}
	
	@Test
	public void testRulePinnedVersion() {
		List<MavenDependency> inputDependencies = new ArrayList<>();
		inputDependencies.add(dep1);
		inputDependencies.add(greenDep);
		
		
		DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
		analyzer.dependencyArbiter.addArbiterRule("groupId=com.green pinnedVersion=100.50.25");
		Map<String, MavenDependency> processedDeps = analyzer.processDependencies(inputDependencies, false);
		
		assertEquals(2, processedDeps.size());
		assertVersion(processedDeps, "1.2.3", dep1);
		assertVersion(processedDeps, "100.50.25", greenDep);
	}
	
	@Test
	public void testRuleWinningVersion() {
		List<MavenDependency> inputDependencies = new ArrayList<>();
		inputDependencies.add(dep1);
		inputDependencies.add(greenDep);
		inputDependencies.add(greenDep_patched);
		
		
		DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
		analyzer.dependencyArbiter.addArbiterRule("groupId=com.green winningVersion=.*patched");
		Map<String, MavenDependency> processedDeps = analyzer.processDependencies(inputDependencies, false);
		
		assertEquals(2, processedDeps.size());
		assertVersion(processedDeps, "1.2.3", dep1);
		assertVersion(processedDeps, "7.8.9-patched", greenDep);
	}
	
	// INTERNAL
	private void assertVersion(Map<String, MavenDependency> processedDeps, String version, MavenDependency dep) {
		MavenDependency pDep = processedDeps.get(dep.getLogicalName()); 
		assertEquals(version, pDep.version.label);
	}
}
