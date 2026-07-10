package com.haadlit_sp.appCoreLogic.model;

import java.nio.file.Path;
import java.util.List;


/** The PDFs the user attached for this session. Cover letter and supporting docs are optional. */
public record SessionDocuments(Path resume, Path coverLetter, List<Path> supporting) {

    public SessionDocuments {
        supporting = supporting == null ? List.of() : List.copyOf(supporting);
    }

    public static SessionDocuments empty() {
        return new SessionDocuments(null, null, List.of());
    }

    public boolean hasResume() { return resume != null; }
}
