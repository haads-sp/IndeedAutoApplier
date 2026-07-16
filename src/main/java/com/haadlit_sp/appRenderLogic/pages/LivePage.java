package com.haadlit_sp.appRenderLogic.pages;


/** A page with live contents. {@code App}'s single UI timer refreshes the visible one on the EDT. */
public interface LivePage {

    /** Pull the latest state from the facade and update widgets. Called on the EDT. */
    void refresh();
}
