/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import java.io.StringReader;
import java.util.Properties;

/**
 * A rule that defines how to preprocess the dependencies and to arbitrate between two versions of the 
 * same dependency.
 * <p>
 * <b>Examples:</b>
 * <ul>
 * <li>groupId=org.green artifactId=.*-transport pinnedVersion=1.5.0 (for any dep from org.green with artifactId ending in -transport, use version 1.5.0)
 * <li>groupId=org.red winningVersion=.*-patched  (for any dep from org.red, always pick a version with -patched if available)
 * </ul>
 * Rules always have to have a <i>groupId</i>.
 * Rules are processed in 'discovered' order. 
 * 
 * @author plaird
 */
public class MavenDependencyArbiterRule {
	public String groupId;
	public String artifactId;

	// preprocess rules
	public String pinnedVersion;
	//public MavenDependencyVersion maxVersion;

	// arbitrage rules
	public String winningVersion;
	
	public MavenDependencyArbiterRule() {}
	
	public MavenDependencyArbiterRule(String ruleLine) {
		parseRuleLine(ruleLine);
	}
	
	/**
	 * Parses a text rule line and configures this rule to match the definition.
	 * 
	 * @param ruleLine the String containing the rule def
	 */
	public void parseRuleLine(String ruleLine) {
		if (ruleLine == null || ruleLine.trim().length() == 0) {
			throw new IllegalArgumentException("Invalid empty rule line.");
		}
		ruleLine = ruleLine.trim();
		ruleLine = ruleLine.replace(" ", "\n");
		Properties ruleProps = new Properties();
		try {
			ruleProps.load(new StringReader(ruleLine));
		} catch (Exception anyE) {
			throw new IllegalArgumentException("Unparseable rule line: "+ruleLine);
		}

		groupId = ruleProps.getProperty("groupId");
		if (groupId == null) {
			throw new IllegalArgumentException("Invalid rule, must always have a groupId: "+ruleLine);
		}
		if (groupId.equals("*")) {
			groupId = ".*";
		}
		artifactId = ruleProps.getProperty("artifactId", ".*");
		if (artifactId.equals("*")) {
			artifactId = ".*";
		}
		pinnedVersion = ruleProps.getProperty("pinnedVersion");
		winningVersion = ruleProps.getProperty("winningVersion");
	}
	
	/**
	 * Passes the dependency through all of the available preprocess style rules (currently just pinnedVersion)
	 * @param dep
	 * @return the dependency, possibly an updated copy if a rule was fired, null if rule didn't fire
	 */
	public MavenDependency preprocess(MavenDependency dep) {
		MavenDependency processedDep = null;
		if (pinnedVersion != null) {
			if (matches(dep)) {
				processedDep = new MavenDependency(dep);
				processedDep.version = new MavenDependencyVersion(pinnedVersion, dep.getLogicalName());
				System.out.println("   RULE MATCH: "+processedDep+" RULE(pinnedVersion): "+this);
			}
		}
		return processedDep;
	}
	
	/**
	 * Passes the dependencies through all of the available comparison style rules (currently just winningVersion).
	 * Both passed dependencies must be for the same logical artifact (group + artifact + classifier).
	 * 
	 * @param dep1
	 * @param dep2
	 * @return the preferred dependency, either dep1 or dep2 depending on which rule/algorithm won out
	 */
	public MavenDependency checkForPreference(MavenDependency dep1, MavenDependency dep2) {
		if (matches(dep1)) {
			if (winningVersion != null) {
				boolean dep1Match = dep1.version.label.matches(winningVersion);
				boolean dep2Match = dep2.version.label.matches(winningVersion);
				if (dep1Match && !dep2Match) {
					// dep1 matches the regex and dep2 doesn't, so prefer dep1
					System.out.println("   RULE MATCH: "+dep1+" RULE(winningVersion): "+this);
					return dep1;
				}
				if (dep2Match && !dep1Match) {
					// dep2 matches the regex and dep1 doesn't, so prefer dep2
					System.out.println("   RULE MATCH: "+dep2+" RULE(winningVersion): "+this);
					return dep2;
				}
				// either both dep1 and dep2 match, or neither matches, so the rule doesn't prefer one over the other
				// so return null
				System.out.println("   RULE NO WINNING PREF: "+this);
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "Rule groupId="+this.groupId+" artifactId="+this.artifactId+" pinnedVersion="+this.pinnedVersion+" winningVersion="+this.winningVersion;
	}
	
	
	// INTERNALS
	
	boolean matches(MavenDependency dep) {
		return dep.groupId.matches(groupId) && dep.artifactId.matches(artifactId); 
	}
}
