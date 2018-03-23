/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import java.util.List;
import java.util.TreeMap;

/**
 * Iterates over a list of Dependency objects and processes them. It will by default choose the later version
 * of each dependency if it can, and can optionally defer to other logic. 
 */
public class DependencyAnalyzer {
	
	MavenDependencyArbiter dependencyArbiter;
    
	public DependencyAnalyzer(MavenDependencyArbiter dependencyArbiter) {
		this.dependencyArbiter = dependencyArbiter;
	}
	
	/**
	 * Iterate through the list and use different techniques to choose the better one.
	 * @param inputDependencies  the raw list of dependencies
	 * @param dropTestScopeDeps true, if test scoped deps should be dropped
	 * @return the list of processed dependencies
	 */
    public TreeMap<String, MavenDependency> processDependencies(List<MavenDependency> inputDependencies, boolean dropTestScopeDeps) {
        TreeMap<String, MavenDependency> finalDependencies = new TreeMap<>();
        
        for (MavenDependency candidateDep : inputDependencies) {
            if (dropTestScopeDeps && candidateDep.scope == MavenDependency.Scope.TEST) {
                System.out.println("  Ignoring test scope dependency ["+candidateDep.getLogicalName()+"]");
                continue;
            }
            System.out.println("  ANALYZE: "+candidateDep);
            
            String key = candidateDep.getLogicalName();
            MavenDependency processedDep = dependencyArbiter.preprocessDependency(candidateDep);
            if (processedDep != null) {
            	// the arbiter made the decision already
                finalDependencies.put(key, processedDep);
                System.out.println("   SELECT: duped deps, chose "+processedDep);
                continue;
            }
            MavenDependency existingDep = finalDependencies.get(key);
            
            if (existingDep != null) {
            	// we have already seen this dependency, so we might have to do version arbitrage if the versions don't match
                if (!existingDep.version.label.equals(candidateDep.version.label)) {
                    // two different versions, need to choose one.
                	processedDep = dependencyArbiter.choosePreferredVersionOfDependency(existingDep, candidateDep);
                    finalDependencies.put(key, processedDep);
                    System.out.println("   SELECT: duped deps, chose "+processedDep);
                } else {
                	// the version we saw before is the same as this one, so no arbitrage needed
                }
            } else {
                finalDependencies.put(key, candidateDep);
                System.out.println("   NEWDEP: "+candidateDep);
            }
        }
        
        return finalDependencies;
    }
}
