package com.zst.zsearch.auth.vo;

import com.zst.zsearch.auth.model.entity.SysUser;
import com.zst.zsearch.auth.security.LoginUser;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserVo {

    private Integer id;
    private String username;
    private String nickname;

    public static UserVo from(SysUser user) {
        UserVo vo = new UserVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        return vo;
    }

    public static UserVo from(LoginUser user) {
        UserVo vo = new UserVo();
        vo.setId(user.id());
        vo.setUsername(user.username());
        vo.setNickname(user.nickname());
        return vo;
    }
}