package com.sports.user.controller;

import com.sports.common.dto.UserDTO;
import com.sports.common.entity.Result;
import com.sports.common.util.BeanConvertUtil;
import com.sports.user.entity.User;
import com.sports.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/")
@Api(tags = "用户管理接口")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/getByUsername")
    @ApiOperation("根据用户名查询用户")
    public Result<UserDTO> getByUsername(
            @ApiParam(value = "用户名", required = true)
            @RequestParam("username") String username) {
        User user = userService.getByUsername(username);
        UserDTO userDTO = BeanConvertUtil.convert(user, UserDTO.class);
        if (userDTO != null && user != null) {
            userDTO.setPassword(user.getPassword());
        }
        return Result.success(userDTO);
    }

    @GetMapping("/getByPhone")
    @ApiOperation("根据手机号查询用户")
    public Result<UserDTO> getByPhone(
            @ApiParam(value = "手机号", required = true)
            @RequestParam("phone") String phone) {
        User user = userService.getByPhone(phone);
        UserDTO userDTO = BeanConvertUtil.convert(user, UserDTO.class);
        if (userDTO != null && user != null) {
            userDTO.setPassword(user.getPassword());
        }
        return Result.success(userDTO);
    }

    @PostMapping("/save")
    @ApiOperation("保存用户信息")
    public Result<Boolean> saveUser(
            @ApiParam(value = "用户信息", required = true)
            @RequestBody UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        if (userDTO.getId() != null) {
            user.setId(userDTO.getId());
        }
        boolean result = userService.saveOrUpdate(user);
        return Result.success(result);
    }

    @GetMapping("/getById")
    @ApiOperation("根据ID查询用户")
    public Result<UserDTO> getById(
            @ApiParam(value = "用户ID", required = true)
            @RequestParam("id") Long id) {
        User user = userService.getById(id);
        UserDTO userDTO = BeanConvertUtil.convert(user, UserDTO.class);
        if (userDTO != null && user != null) {
            userDTO.setPassword(user.getPassword());
        }
        return Result.success(userDTO);
    }
}
