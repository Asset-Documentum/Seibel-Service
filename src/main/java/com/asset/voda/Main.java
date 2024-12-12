package com.asset.voda;

public class Main {
    public static void main(String[] args) throws Exception {
        // Create and start the FolderWatcher
        FolderWatcher watcher = new FolderWatcher();
        watcher.watchFolder();  // This starts monitoring the "to-be-processed" folder

    }
}

