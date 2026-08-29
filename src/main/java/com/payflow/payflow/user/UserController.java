package com.payflow.payflow.user;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController //Java 객체를 반환하면 Spring이 자동으로 JSON으로 바꿔준다.
@RequestMapping("/api/users") // /api/users를 기본 url로 만든다.
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public User createUser(@RequestBody CreateUserRequest request){
        return userService.createUser(request.name());
    }
    @GetMapping
    public List<User> getUsers(){
        return userService.getUsers();
    }
    public record CreateUserRequest(String name){
    }
}
