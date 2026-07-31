package com.haadlit_sp.appCoreLogic.store;

import com.haadlit_sp.appCoreLogic.model.ContactDetails;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


/**
 * Persists the user's {@link ContactDetails} as simple {@code key\tvalue} lines, so they are entered
 * once and reused. Fail-soft: a missing or unreadable file is treated as no details yet.
 *
 * <p>This is PII on disk (name, email, phone, address) — never credentials — kept only in the
 * per-user app directory. Deleting {@code ~/.indeedapplier/contact.tsv} removes it.
 */
public class ContactDetailsStore {

    private static final Logger LOG = System.getLogger(ContactDetailsStore.class.getName());
    private static final String SEP = "\t";

    private final Path file;

    public ContactDetailsStore() {
        this(AppPaths.contactFile());
    }

    ContactDetailsStore(Path file) {
        this.file = file;
    }

    public ContactDetails load() {
        if (!Files.exists(file)) {
            return ContactDetails.empty();
        }
        try {
            Map<String, String> values = new HashMap<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split(SEP, 2);
                if (parts.length == 2) {
                    values.put(parts[0], parts[1]);
                }
            }
            return new ContactDetails(
                    values.getOrDefault("firstName", ""), values.getOrDefault("lastName", ""),
                    values.getOrDefault("email", ""), values.getOrDefault("phone", ""),
                    values.getOrDefault("streetAddress", ""), values.getOrDefault("city", ""),
                    values.getOrDefault("region", ""), values.getOrDefault("postalCode", ""),
                    values.getOrDefault("country", ""));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read contact details; starting blank", e);
            return ContactDetails.empty();
        }
    }

    public void save(ContactDetails details) {
        StringBuilder out = new StringBuilder();
        line(out, "firstName", details.firstName());
        line(out, "lastName", details.lastName());
        line(out, "email", details.email());
        line(out, "phone", details.phone());
        line(out, "streetAddress", details.streetAddress());
        line(out, "city", details.city());
        line(out, "region", details.region());
        line(out, "postalCode", details.postalCode());
        line(out, "country", details.country());
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save contact details", e);
        }
    }

    private static void line(StringBuilder out, String key, String value) {
        out.append(key).append(SEP).append(clean(value)).append('\n');
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("[\t\r\n]", " ");
    }
}
