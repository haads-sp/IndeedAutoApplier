package com.haadlit_sp.appCoreLogic.session;


/** Builds the login strategy. Indeed no longer accepts email+password, so manual is the only variant. */
public final class LoginStrategyFactory {

    private LoginStrategyFactory() {}

    public static LoginStrategy manual() {
        return new ManualLoginStrategy();
    }
}
