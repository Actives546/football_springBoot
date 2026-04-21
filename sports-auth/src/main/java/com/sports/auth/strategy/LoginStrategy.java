package com.sports.auth.strategy;

import com.sports.auth.dto.LoginDTO;
import com.sports.auth.dto.LoginResponseDTO;

public interface LoginStrategy {

    LoginResponseDTO login(LoginDTO loginDTO);

    String getLoginType();
}
