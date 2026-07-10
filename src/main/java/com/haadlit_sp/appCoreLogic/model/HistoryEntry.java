package com.haadlit_sp.appCoreLogic.model;

import java.time.Instant;


/** One applied (or attempted) posting, shown in the session history and used for dedup. */
public record HistoryEntry(String title, String company, Instant timestamp, String outcome) {
}
