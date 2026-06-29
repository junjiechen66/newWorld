package com.newworld.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "修改用户信息请求")
public class UpdateUserRequest {

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
