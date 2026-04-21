package com.sports.auth.service;

import com.sports.auth.dto.LoginByPhoneDTO;
import com.sports.auth.dto.LoginByUsernameDTO;
import com.sports.auth.dto.LoginDTO;
import com.sports.auth.dto.LoginResponseDTO;
import com.sports.auth.dto.RegisterDTO;

public interface AuthService {

    LoginResponseDTO login(LoginDTO loginDTO);

    LoginResponseDTO loginByUsername(LoginByUsernameDTO loginDTO);

    LoginResponseDTO loginByPhone(LoginByPhoneDTO loginDTO);

    Boolean register(RegisterDTO registerDTO);

    Boolean logout(String token);
}
