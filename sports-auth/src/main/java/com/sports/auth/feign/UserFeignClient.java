package com.sports.auth.feign;

import com.sports.common.dto.UserDTO;
import com.sports.common.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "sports-user", path = "/user")
public interface UserFeignClient {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/getByUsername")
    Result<UserDTO> getByUsername(@RequestParam String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    @GetMapping("/getByPhone")
    Result<UserDTO> getByPhone(@RequestParam String phone);

    /**
     * 保存用户信息
     *
     * @param userDTO 用户信息
     * @return 是否保存成功
     */
    @PostMapping("/save")
    Result<Boolean> saveUser(@RequestBody UserDTO userDTO);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/getById")
    Result<UserDTO> getById(@RequestParam Long id);
}
