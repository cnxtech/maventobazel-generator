/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.bazel.migration;

import java.io.File;
import java.util.*;

/**
 * Entry point for command line tools that iterate over the output of mvn dependency:list
 * and generate various Bazel configuration files. 
 */
public class MavenToBazelGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        Set<MigrationOptions> options = parseCommandLine(args);
        
        try {
            doMigration("inputs", "outputs", options);
        } catch (Exception anyE) {
            anyE.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Main entry point for doing the migration work
     * 
     * @param inputDirectoryPath the relative path to find all the input files containing existing Bazel deps, rules, and new Maven deps
     * @param outputDirectoryPath the relative path where the output file will be written
     * @param options the options to use during the proceessing
     * @throws Exception
     */
    protected static void doMigration(String inputDirectoryPath, String outputDirectoryPath, Set<MigrationOptions> options) throws Exception {
        File inputDirectoryFile = new File(inputDirectoryPath);
        String inputDirectoryAbsolutePath = inputDirectoryFile.getAbsolutePath();
        if (!inputDirectoryFile.exists()) {
            System.err.println("Input directory ["+inputDirectoryAbsolutePath+"] does not exist, exiting...");
            System.exit(1);
        }
        if (!inputDirectoryFile.isDirectory()) {
            System.err.println("Input directory ["+inputDirectoryAbsolutePath+"] is not a directory, exiting...");
            System.exit(1);
        }
        File outputDirectoryFile = new File(outputDirectoryPath);
        String outputDirectoryAbsolutePath = outputDirectoryFile.getAbsolutePath();
        if (!outputDirectoryFile.exists()) {
            System.err.println("Output directory ["+outputDirectoryAbsolutePath+"] does not exist, exiting...");
            System.exit(1);
        }
        
        MavenDependencyArbiter arbiter = new MavenDependencyArbiter();
        DependenciesParser parser = new DependenciesParser(arbiter);
        List<MavenDependency> deps = new ArrayList<>();
        for (File candidateFile : inputDirectoryFile.listFiles()) {
	        System.out.println("Loading dependency input file ["+candidateFile+"]");
	        deps.addAll(parser.parseFile(candidateFile));
        }
        System.out.println("Loaded ["+deps.size()+"] dependencies (some may be dupes), now analyzing and will dedupe the list...");
        
        DependencyAnalyzer analyzer = new DependencyAnalyzer(arbiter);
        Map<String, MavenDependency> computedDeps = analyzer.processDependencies(deps, options.contains(MigrationOptions.DROP_TEST_SCOPE_DEPS));

        System.out.println("Analyzed the dependencies, the final list contains ["+computedDeps.size()+"] entries.");
        
        for (MavenDependency dep : computedDeps.values()) {
            System.out.println(dep.getLogicalName());
        }
        
        if (options.contains(MigrationOptions.GENERATE_WORKSPACE)) {
            BazelWorkspaceGenerator bazelWorkspace = new BazelWorkspaceGenerator();
            bazelWorkspace.writeDependenciesAsWorkspaceFile(computedDeps, new File("outputs/external_deps.bzl.out"));
        }
        
        if (options.contains(MigrationOptions.GENERATE_BUILD)) {
            BazelBuildDependenciesGenerator bazelBuild = new BazelBuildDependenciesGenerator();
            bazelBuild.writeDependenciesAsBuildFile(computedDeps, new File("outputs/BUILD.out"));
        }
    }
    
    protected static Set<MigrationOptions> parseCommandLine(String[] args) {
        Set<MigrationOptions> options = new HashSet<>();
        for (String arg : args) {
            switch (arg) {
            case "--ignoretestdeps":
                    options.add(MigrationOptions.DROP_TEST_SCOPE_DEPS);
                    System.out.println(" option: ignoring test scoped dependencies");
                    break;
            case "--build":
                options.add(MigrationOptions.GENERATE_BUILD);
                System.out.println(" option: generating a BUILD.out file");
                break;
            case "--workspace":
                options.add(MigrationOptions.GENERATE_WORKSPACE);
                System.out.println(" option: generating a WORKSPACE.out file");
                break;
            case "--help":
                printUsage();
                break;
            }
        }
        return options;
    }
    
    private static void printUsage() {
        System.out.println(" See the README for docs.\n java -jar maventobazel-generator.jar [options]\n Options:  --ignoretestdeps --build --workspace");
    }
    
    protected static enum MigrationOptions {
        DROP_TEST_SCOPE_DEPS,
        GENERATE_BUILD,
        GENERATE_WORKSPACE
    }
}
