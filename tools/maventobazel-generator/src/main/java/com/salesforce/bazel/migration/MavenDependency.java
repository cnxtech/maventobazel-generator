/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import java.util.Comparator;

/**
 * Encapsulates a Maven dependency as a POJO. These are normally constructed for you by the
 * DependenciesParser.
 * 
 * @author plaird
 */
public class MavenDependency implements Comparator<MavenDependency> {
    public String originalLine;
    public String groupId;
    public String artifactId;
    public Scope scope;
    public MavenDependencyVersion version;
    public String classifier;
    
    /**
     * @param originalLine the line this dep was parsed from, only used in logging output
     * @param groupId the Maven groupId
     * @param artifactId the Maven artifactId
     * @param scope the Maven scope, "compile", "provided", "test"
     * @param version the Maven version string, usually SemVer but not always. See MavenDependencyVersion for details.
     */
    public MavenDependency(String originalLine, String groupId, String artifactId, String scope, String version) {
        this(originalLine, groupId, artifactId, scope, version, null);
    }
    
    /**
     * @param originalLine the line this dep was parsed from, only used in logging output
     * @param groupId the Maven groupId
     * @param artifactId the Maven artifactId
     * @param scope the Maven scope, "compile", "provided", "test"
     * @param version the Maven version string, usually SemVer but not always. See MavenDependencyVersion for details.
     * @param classifier  the Maven classifier (not common), as in "test-jar", "idl", etc
     */
    public MavenDependency(String originalLine, String groupId, String artifactId, String scope, String version, String classifier) {
        this.originalLine = originalLine;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.scope = Scope.valueOf(scope.toUpperCase());
        this.version = new MavenDependencyVersion(version, ga(groupId, artifactId));
        this.classifier = classifier;
    }
    
    /**
     * Copy ctor
     * @param clone
     */
    public MavenDependency(MavenDependency clone) {
        this.originalLine = clone.originalLine;
        this.groupId = clone.groupId;
        this.artifactId = clone.artifactId;
        this.scope = clone.scope;
        this.version = new MavenDependencyVersion(clone.version);
        this.classifier = clone.classifier;
    }
    
    /**
     * Name that identifies the dependency, regardless of version. This is used to determine if two dependency POJOs are pointed
     * to the same logical artifact (but, perhaps different versions).
     * <p>
     * If there is no classifier, this is group:artifact.
     * If there is a classifier, this is group:artifact:classifier
     * @return the name
     */
    public String getLogicalName() {
        if (this.classifier == null) {
            return ga(this.groupId, this.artifactId);
        }
        return ga(this.groupId, this.artifactId)+":"+this.classifier;
    }
    
    public static enum Scope {
        COMPILE,
        RUNTIME,
        PROVIDED,
        TEST;
    }
    
    /**
     * The Comparator implementation is for lexical ordering in the output. Specifically,
     * to minimize diffs when updating a BUILD or WORKSPACE file, we want consistent ordering when
     * we write the dependencies to file. That is what this method is for.
     * <p> 
     * This method is <b>NOT</b> for determining which version of a dependency is 'newer'. 
     * See {@link #MavenDependencyArbiter.choosePreferredVersionOfDependency} for that functionality.
     * 
     * @param o1
     * @param o2
     * @return
     */
	@Override
	public int compare(MavenDependency dep1, MavenDependency dep2) {
		// we sort on the Bazel name, since when we write files that is the key for each entry
		String name1 = BazelNamer.computeBazelName(dep1);
		String name2 = BazelNamer.computeBazelName(dep2);
		return name1.compareTo(name2);
	}
	
	@Override
	public String toString() {
		return this.groupId+":"+this.artifactId+":"+this.version;
	}

    private static String ga(String groupId, String artifactId) {
        return groupId+":"+artifactId;
    }
}
