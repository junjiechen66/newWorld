package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "修改用户信息请求")
public class UpdateUserRequest {

    @Schema(description = "昵称")
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
