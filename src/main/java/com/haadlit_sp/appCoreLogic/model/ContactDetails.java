package com.haadlit_sp.appCoreLogic.model;


/**
 * The personal details every application asks for, collected once upfront so the run rarely has to
 * pause. Seeded from the resume, confirmed by the user, and used to fill the contact/location steps.
 */
public record ContactDetails(String firstName, String lastName, String email, String phone,
                             String streetAddress, String city, String region,
                             String postalCode, String country) {

    public ContactDetails {
        firstName = nz(firstName);
        lastName = nz(lastName);
        email = nz(email);
        phone = nz(phone);
        streetAddress = nz(streetAddress);
        city = nz(city);
        region = nz(region);
        postalCode = nz(postalCode);
        country = nz(country);
    }

    public static ContactDetails empty() {
        return new ContactDetails("", "", "", "", "", "", "", "", "");
    }

    /** A first guess from the resume facts; the user confirms it on the details page. */
    public static ContactDetails fromFacts(ProfileFacts facts) {
        String first = "";
        String last = "";
        if (facts.fullName() != null && !facts.fullName().isBlank()) {
            String[] parts = facts.fullName().trim().split("\\s+");
            first = parts[0];
            if (parts.length > 1) {
                last = parts[parts.length - 1];
            }
        }
        return new ContactDetails(first, last, nz(facts.email()), nz(facts.phone()),
                "", "", "", "", "");
    }

    /** The essentials the contact module needs; used to know if the step is complete. */
    public boolean hasBasics() {
        return !firstName.isBlank() && !lastName.isBlank() && !email.isBlank() && !phone.isBlank();
    }

    public boolean isEmpty() {
        return firstName.isBlank() && lastName.isBlank() && email.isBlank() && phone.isBlank()
                && streetAddress.isBlank() && city.isBlank() && region.isBlank()
                && postalCode.isBlank() && country.isBlank();
    }

    /** "City, Province" for the location step's single locality field. */
    public String cityRegion() {
        if (city.isBlank()) {
            return region;
        }
        return region.isBlank() ? city : city + ", " + region;
    }

    private static String nz(String value) {
        return value == null ? "" : value.strip();
    }
}
