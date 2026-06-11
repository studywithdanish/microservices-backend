package com.danish.blog.services.impl;

import com.danish.blog.entities.Role;
import com.danish.blog.entities.User;
import com.danish.blog.exceptions.ResourceNotFoundException;
import com.danish.blog.payloads.AppConstants;
import com.danish.blog.payloads.UserDto;
import com.danish.blog.repositories.RoleRepo;
import com.danish.blog.repositories.UserRepo;
import com.danish.blog.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepo roleRepo;

    @Override
    public UserDto registerUser(UserDto userDto) {

        User user = modelMapper.map(userDto, User.class);
        //encode the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        //roles
        Role role = roleRepo.findById(AppConstants.NORMAL_USER).get();
        user.getRoles().add(role);
        return modelMapper.map(userRepo.save(user), UserDto.class);
    }

    @Override
    public UserDto createUser(UserDto userDto) {

        User user = this.dtoToUser(userDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = this.userRepo.save(user);
        return this.userToDto(savedUser);
    }

    @Override
    public UserDto updateUser(UserDto userDto, Integer userId) {

        User user = this.userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setAbout(userDto.getAbout());

        User updatedUser = this.userRepo.save(user);

        return this.userToDto(updatedUser);
    }

    @Override
    public UserDto getUserById(Integer userId) {

        User user = this.userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        logger.debug("Fetched user by id: {}", userId);
        return this.userToDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {

        List<User> allUsers = userRepo.findAll();

        return allUsers.stream().map(user -> this.userToDto(user)).collect(Collectors.toList());
       /* UserDto userDto = new UserDto();
        List<UserDto> userDtos = new ArrayList<>();
        for(User user : allUsers) {
            userDto=this.userToDto(user);
            userDtos.add(userDto);
        }*/

       /* return allUserDtos;*/
    }

    @Override
    public void deleteUser(Integer userId) {

        User user=userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
        userRepo.delete(user);
    }

    private User dtoToUser(UserDto userDto) {

        User user = this.modelMapper.map(userDto,User.class);

        /*User user = new User();
        user.setId(userDto.getId());
        user.setName(userDto.getName());
        user.setAbout(userDto.getAbout());
        user.setEmail(userDto.getEmail());
        user.setPassword(userDto.getPassword());*/
        return user;
    }

    private UserDto userToDto(User user) {

        return this.modelMapper.map(user, UserDto.class);

        /*UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setPassword(user.getPassword());
        userDto.setEmail(user.getEmail());
        userDto.setAbout(user.getAbout());
        return userDto;*/
    }
}
