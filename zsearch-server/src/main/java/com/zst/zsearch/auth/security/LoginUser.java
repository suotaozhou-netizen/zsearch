package com.zst.zsearch.auth.security;

import java.security.Principal;

public record LoginUser(Integer id, String username, String nickname) implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public String displayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }
}