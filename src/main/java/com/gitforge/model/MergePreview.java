package com.gitforge.model;

/**
 * In-memory merge preview used by the Merge Management center panel.
 */
public class MergePreview {

    private final String sourceBranchName;
    private final String targetBranchName;
    private final String sourceLatestCommit;
    private final String targetLatestCommit;
    private final int commitsToMerge;
    private final String expectedMergeCommit;
    private final String conflictStatus;
    private final String strategy;

    public MergePreview(String sourceBranchName,
                        String targetBranchName,
                        String sourceLatestCommit,
                        String targetLatestCommit,
                        int commitsToMerge,
                        String expectedMergeCommit,
                        String conflictStatus,
                        String strategy) {
        this.sourceBranchName = sourceBranchName;
        this.targetBranchName = targetBranchName;
        this.sourceLatestCommit = sourceLatestCommit;
        this.targetLatestCommit = targetLatestCommit;
        this.commitsToMerge = commitsToMerge;
        this.expectedMergeCommit = expectedMergeCommit;
        this.conflictStatus = conflictStatus;
        this.strategy = strategy;
    }

    public String getSourceBranchName() {
        return sourceBranchName;
    }

    public String getTargetBranchName() {
        return targetBranchName;
    }

    public String getSourceLatestCommit() {
        return sourceLatestCommit;
    }

    public String getTargetLatestCommit() {
        return targetLatestCommit;
    }

    public String getLatestCommitDisplay() {
        String source = shortHash(sourceLatestCommit);
        String target = shortHash(targetLatestCommit);
        return "Source " + source + " → Target " + target;
    }

    public int getCommitsToMerge() {
        return commitsToMerge;
    }

    public String getExpectedMergeCommit() {
        return expectedMergeCommit;
    }

    public String getShortExpectedMergeCommit() {
        return shortHash(expectedMergeCommit);
    }

    public String getConflictStatus() {
        return conflictStatus;
    }

    public String getStrategy() {
        return strategy;
    }

    private static String shortHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return "—";
        }
        return hash.length() <= 8 ? hash : hash.substring(0, 8);
    }
}
