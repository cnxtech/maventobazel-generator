/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import java.util.ArrayList;
import java.util.List;

/**
 * When merging multiple sources of dependencies (e.g. an existing WORKSPACE file and a migrating Maven project's list of deps)
 * this class will choose the preferred version of the dependency when there is a discrepency.
 * <p>
 * <b>Example</b>
 * <p>
 * For example, if the WORKSPACE points to version 1.2.3.4 of <i>com.sample:foo</i>, and the Maven project's pom.xml points to
 * version 1.2.3.7, this class will automatically choose the 1.2.3.7 version.
 * <p>
 * <b>Overrides</b>
 * <p>
 * There are two cases for defining manual overrides for this logic.
 * In both these cases, you will list the override in the TODO file.
 * <p>
 * 1. If you wish for an older version of a dependency to win out (e.g. 1.2.3.4 wins over 1.2.3.7) you need to define an
 * override.<br/>
 * 2. If the dependency does not use SemVer (major.minor.patch.hotfix) versioning scheme, the algorithm cannot automaticlly choose
 * which dependency is newer. For example, version <i>1.2.bigshow.3</i> is not SemVer.
 * 
 * @author plaird
 *
 */
public class MavenDependencyArbiter {
	private List<MavenDependencyArbiterRule> rules = new ArrayList<>();
	
	public MavenDependencyArbiter() {
		
	}
	
	/**
	 * Adds a new rule to the arbiter. See {@link MavenDependencyArbiterRule} for the format of the String.
	 * @param ruleLine
	 */
    public MavenDependencyArbiterRule addArbiterRule(String ruleLine) {
    	MavenDependencyArbiterRule rule = new MavenDependencyArbiterRule(ruleLine);
    	rules.add(rule);
    	return rule;
    }
    
    /**
     * Process unary rules (e.g. pinned versions) that may apply to this dependency
     * @param dep
     * @return non-null if a rule fired, null if no rule applied
     */
    public MavenDependency preprocessDependency(MavenDependency dep) {
    	MavenDependency processedDep = null;
    	for (MavenDependencyArbiterRule rule : rules) {
    		processedDep = rule.preprocess(dep);
    		if (processedDep != null) {
    			break;
    		}
    	}
    	return processedDep;
    }
    
    /**
     * Picks the better version of the two versions that were found for the same logical dependency (groupid + artifactid + classifier)
     * It first checks if a rule has an opinion about it, otherwise it will try to use SemVer to pick the newer of the versions.
     *  
     * @param dep1
     * @param dep2
     * @return the preferred dep
     */
    public MavenDependency choosePreferredVersionOfDependency(MavenDependency dep1, MavenDependency dep2) {
        if (!dep2.getLogicalName().equals(dep1.getLogicalName())) {
            throw new IllegalStateException("Fatal bug, trying to find the later version of different dependencies ["+dep2.getLogicalName()+
                    "] and ["+dep1.getLogicalName()+"]");
        }
        
        // Look for a human decision for choosing this dependency (e.g. a pinned version).
        // These are cases where the operator specifically wants to pin the version of a particular library to a particular version.
        // This should be for cases in which the version name is not in SemVer form (e.g. my.chosen.version as opposed to something like 1.2.3.4),
        // or you need to use an older version for some reason (e.g. 1.0.0 and a migrating project wants to use 1.2.3.4).
        MavenDependency processedDep = null;
        for (MavenDependencyArbiterRule rule : rules) {
    		processedDep = rule.checkForPreference(dep1, dep2);
    		if (processedDep != null) {
    			return processedDep;
    		}
    	}
        
        // Use the automatic latest SemVer choosing algorithm
        if (chooseLaterVersionOfDependencyUsingSemVer(dep1.version, dep2.version) >= 0) {
            return dep1;
        }
        return dep2;
    }
    
    // INTERNALS
    
    static int chooseLaterVersionOfDependencyUsingSemVer(MavenDependencyVersion dep1, MavenDependencyVersion dep2) {
    	if (!dep1.isSemVer || !dep2.isSemVer) {
            throw new IllegalStateException("Could not determine the better version of dep ["+dep1.groupIdArtifactIdForLog+"]. Input file "+
                    "contains both version ["+dep1.label+"] and ["+dep2.label+"]. Please remove one of these lines from the "+
                    "input file because they cannot be automatically compared, or add a new Arbiter Rule."
             );
    	}
    	
        if (dep1.majorVersion > dep2.majorVersion) {
            return 1;
        } 
        if (dep2.majorVersion > dep1.majorVersion) {
            return -1;
        } 
        if (dep1.minorVersion > dep2.minorVersion) {
            return 1;
        } 
        if (dep2.minorVersion > dep1.minorVersion) {
            return -1;
        } 
        if (dep1.patchVersion > dep2.patchVersion) {
            return 1;
        } 
        if (dep2.patchVersion > dep1.patchVersion) {
            return -1;
        } 
        if (dep1.hotfixVersion > dep2.hotfixVersion) {
            return 1;
        } 
        if (dep2.hotfixVersion > dep1.hotfixVersion) {
            return -1;
        } 
        
        // At this point, both dependencies are computed to be equal, but scream loudly if the labels
        // don't match, because that means someone is using a nonstandard version format (e.g 1.2.3 vs 1.2.3-hotfix) 
        // and we can't be sure which one is later.
        // We need a human to adjust the input file to remove the unwanted version, or add a MavenDependencyArbiterRule
        if (!dep1.label.equals(dep2.label)) {
            throw new IllegalStateException("Could not determine the better version of dep ["+dep1.groupIdArtifactIdForLog+"]. Input file "+
                      "contains both version ["+dep1.label+"] and ["+dep2.label+"]. Please remove one of these lines from the "+
                      "input file because they cannot be automatically compared."
               );
        }
        
        // they are the same
        return 0;
    }
    
}
