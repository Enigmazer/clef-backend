package com.enigmazer.clef.dto.auth;

public sealed interface LoginResponse permits LoginSuccessResponse, TwoFactorRequiredResponse {}