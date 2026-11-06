package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void delete(Long id) {
        this.userRepository.deleteById(Objects.requireNonNull(id));
    }

    public User findById(Long id) {
        return this.userRepository.findById(Objects.requireNonNull(id)).orElse(null);
    }
}
