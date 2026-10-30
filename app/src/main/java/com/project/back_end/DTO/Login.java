package com.project.back_end.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Login {

    // Email for doctors/patients, username for admins.
    // The frontend sends this as "email", so that name is accepted too.
    @JsonAlias({"email", "username"})
    private String identifier;

    private String password;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
