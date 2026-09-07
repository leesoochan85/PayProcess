package com.payflow.user;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String name){
        User user = new User(name);
        return userRepository.save(user);
    }
    public List<User> getUsers(){
        return userRepository.findAll();
    }
}
