package com.zst.zsearch.auth.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginVo {

    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserVo user;
}