package com.haadlit_sp.appCoreLogic.model;


/** A live snapshot of the automation run, polled by the UI. */
public record RunStatus(boolean running, boolean paused, String currentPosting,
                        int submitted, int skipped, int failed, String lastMessage) {

    public static RunStatus idle() {
        return new RunStatus(false, false, "—", 0, 0, 0, "");
    }
}
