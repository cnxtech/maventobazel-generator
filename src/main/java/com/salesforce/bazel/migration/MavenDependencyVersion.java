/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

/**
 * Encapsulates the version of a Maven dependency.
 * 
 * @author plaird
 */
public class MavenDependencyVersion {
    public String label;
    public String groupIdArtifactIdForLog;
    
    // does this version follow numeric SemVer form? (major.minor.patch.hotfix)
    public boolean isSemVer = true;
    
    // these numerics are only used to try to compute what is the 'latest' version
    public int majorVersion = 0;
    public int minorVersion = 0;
    public int patchVersion = 0;
    public int hotfixVersion = 0;
    
    /**
     * Ctor
     * @param label String such as "1.3.0", "1.2.3.4-SNAPSHOT", "my-weird-version"
     */
    public MavenDependencyVersion(String label) {
        this(label, null);
    }

    /**
     * Ctor
     * @param label String such as "1.3.0", "1.2.3.4-SNAPSHOT", "my-weird-version"
     * @param groupIdArtifactIdForLog a good identifier for this artifact for logging
     */
    public MavenDependencyVersion(String label, String groupIdArtifactIdForLog) {
        this.label = label;
        this.groupIdArtifactIdForLog = groupIdArtifactIdForLog;
        
        String[] tokens = label.split("\\.");
        majorVersion = numericToken(tokens, 0);
        minorVersion = numericToken(tokens, 1);
        patchVersion = numericToken(tokens, 2);
        hotfixVersion = numericToken(tokens, 3);
        if (tokens.length > 4) {
        	// even if the additional tokens are numeric, we are flagging this one as non SemVer so a human will 
        	// have to manually look at if there are multiple versions of the same dependency
        	isSemVer = false;
        }
    }
    
    /**
     * Copy ctor
     * @param clone
     */
    public MavenDependencyVersion(int major, int minor, int patch) {
    	this(major, minor, patch, 0);
    }

    /**
     * Ctor
     */
    public MavenDependencyVersion(int major, int minor, int patch, int hotfix) {
        this.majorVersion = major;
        this.minorVersion = minor;
        this.patchVersion = patch;
        this.hotfixVersion = hotfix;
        this.label = "" + major + "." + minor + "." + patch + "." + hotfix;
    }
    
    /**
     * Ctor
     */
    public MavenDependencyVersion(MavenDependencyVersion clone) {
        this.majorVersion = clone.majorVersion;
        this.minorVersion = clone.minorVersion;
        this.patchVersion = clone.patchVersion;
        this.hotfixVersion = clone.hotfixVersion;
        this.label = clone.label;
        this.groupIdArtifactIdForLog = clone.groupIdArtifactIdForLog;
    }

    /**
     * Helper for parsing the label into tokenized SemVer values
     * @param tokens
     * @param index
     * @return
     */
    int numericToken(String[] tokens, int index) {
    	if (tokens.length <= index) {
    		// This version label does not have fully specific SemVer (e.g. 1.2 not 1.2.3.4)
    		// That is ok, just right pad the empty fields with 0's
    		return 0;
    	}
        try {
            String token = tokens[index];
            return Integer.parseInt(token);
        } catch (Exception anyE) {
        	// there is something non-numeric about this token, which means this is not SemVer.
        	isSemVer = false;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hotfixVersion;
        result = prime * result + majorVersion;
        result = prime * result + minorVersion;
        result = prime * result + patchVersion;
        result = prime * result + label.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MavenDependencyVersion other = (MavenDependencyVersion)obj;
        if (hotfixVersion != other.hotfixVersion) return false;
        if (majorVersion != other.majorVersion) return false;
        if (minorVersion != other.minorVersion) return false;
        if (patchVersion != other.patchVersion) return false;
        if (!label.equals(other.label)) return false;
        return true;
    }

    
    @Override
    public String toString() {
    	return this.label;
    }
}
