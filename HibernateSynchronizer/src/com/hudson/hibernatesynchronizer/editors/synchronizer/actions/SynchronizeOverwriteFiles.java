package com.hudson.hibernatesynchronizer.editors.synchronizer.actions;

/**
 * @author Joe Hudson
 */
public class SynchronizeOverwriteFiles extends SynchronizeFiles {

    protected boolean shouldForce() {
        return true;
    }
}
