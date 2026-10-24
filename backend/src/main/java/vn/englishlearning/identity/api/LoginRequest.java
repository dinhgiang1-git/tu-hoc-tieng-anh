package vn.englishlearning.identity.api;

import jakarta.validation.constraints.NotBlank;

/** Standard Login request adhering to openapi.yaml LoginRequest. */
public record LoginRequest(
    @NotBlank(message = "Tên đăng nhập không được để trống.") String username,
    @NotBlank(message = "Mật khẩu không được để trống.") String password) {}
