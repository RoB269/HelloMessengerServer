package com.github.rob269;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

public class UserAccount extends User {
    private transient String passwordHash;

    public UserAccount(String id, String password) {
        super(id);
        passwordHash = Hashing.sha256().hashString(password, StandardCharsets.UTF_8).toString();
    }
}
