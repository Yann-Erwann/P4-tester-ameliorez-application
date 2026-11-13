package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.exception.UnauthorizedOperationException;
import com.openclassrooms.starterjwt.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void delete(Long id, String requestingEmail) {
        User user = getRequiredById(id);
        if (!Objects.equals(user.getEmail(), requestingEmail)) {
            throw new UnauthorizedOperationException();
        }
        this.userRepository.delete(user);
    }

    public User getRequiredById(Long id) {
        return this.userRepository.findById(Objects.requireNonNull(id)).orElseThrow(NotFoundException::new);
    }

    public User findById(Long id) {
        return this.userRepository.findById(Objects.requireNonNull(id)).orElse(null);
    }
}
