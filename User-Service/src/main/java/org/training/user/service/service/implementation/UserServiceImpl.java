package org.training.user.service.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.training.user.service.exception.EmptyFields;
import org.training.user.service.exception.ResourceConflictException;
import org.training.user.service.exception.ResourceNotFound;
import org.training.user.service.model.Status;
import org.training.user.service.model.dto.CreateUser;
import org.training.user.service.model.dto.UserDto;
import org.training.user.service.model.dto.UserUpdate;
import org.training.user.service.model.dto.UserUpdateStatus;
import org.training.user.service.model.dto.response.Response;
import org.training.user.service.model.entity.User;
import org.training.user.service.model.entity.UserProfile;
import org.training.user.service.model.mapper.UserMapper;
import org.training.user.service.repository.UserRepository;
import org.training.user.service.service.UserService;
import org.training.user.service.utils.Constants;
import org.training.user.service.utils.FieldChecker;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper = new UserMapper();

    /**
     * Creates a new user.
     *
     * @param userDto The user data transfer object containing user information.
     * @return A response indicating the result of the user creation.
     * @throws ResourceConflictException If the emailId is already registered as a user.
     */
    @Override
    public Response createUser(CreateUser userDto) {

        if (userRepository.existsByEmailId(userDto.getEmailId())) {
            log.error("This emailId is already registered as a user");
            throw new ResourceConflictException("This emailId is already registered as a user");
        }

        UserProfile userProfile = UserProfile.builder()
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName()).build();

        User user = User.builder()
                .emailId(userDto.getEmailId())
                .contactNo(userDto.getContactNumber())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .status(Status.PENDING).userProfile(userProfile)
                .authId(UUID.randomUUID().toString())
                .identificationNumber(UUID.randomUUID().toString()).build();

        userRepository.save(user);
        return Response.builder()
                .responseMessage("User created successfully")
                .responseCode(Constants.SUCCESS).build();
    }

    /**
     * Retrieves all users and their corresponding details.
     *
     * @return a list of UserDto objects containing the user information
     */
    @Override
    public List<UserDto> readAllUsers() {

        return userRepository.findAll().stream()
                .map(userMapper::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Reads a user from the database using the provided authId.
     *
     * @param authId the authentication id of the user
     * @return the UserDto object representing the user
     * @throws ResourceNotFound if the user is not found on the server
     */
    @Override
    public UserDto readUser(String authId) {

        return userRepository.findUserByAuthId(authId)
                .map(userMapper::convertToDto)
                .orElseThrow(() -> new ResourceNotFound("User not found on the server"));
    }

    /**
     * Updates the status of a user.
     *
     * @param id The ID of the user.
     * @param userUpdate The updated user status.
     * @return The response indicating the success of the update.
     * @throws ResourceNotFound If the user is not found.
     * @throws EmptyFields If the user has empty fields.
     */
    @Override
    public Response updateUserStatus(Long id, UserUpdateStatus userUpdate) {

        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFound("User not found on the server"));

        if (FieldChecker.hasEmptyFields(user)) {
            log.error("User is not updated completely");
            throw new EmptyFields("please updated the user", Constants.NOT_FOUND);
        }

        user.setStatus(userUpdate.getStatus());
        userRepository.save(user);

        return Response.builder()
                .responseMessage("User updated successfully")
                .responseCode(Constants.SUCCESS).build();
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the ID of the user to retrieve
     * @return the UserDto object representing the user
     * @throws ResourceNotFound if the user is not found
     */
    @Override
    public UserDto readUserById(Long userId) {

        return userRepository.findById(userId)
                .map(userMapper::convertToDto)
                .orElseThrow(() -> new ResourceNotFound("User not found on the server"));
    }

    /**
     * Updates a user with the given ID.
     *
     * @param id The ID of the user to update.
     * @param userUpdate The updated information for the user.
     * @return The response indicating the success or failure of the update operation.
     * @throws ResourceNotFound if the user with the given ID is not found.
     */
    @Override
    public Response updateUser(Long id, UserUpdate userUpdate) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("User not found on the server"));

        user.setContactNo(userUpdate.getContactNo());
        BeanUtils.copyProperties(userUpdate, user.getUserProfile());
        userRepository.save(user);

        return Response.builder()
                .responseCode(Constants.SUCCESS)
                .responseMessage("user updated successfully").build();
    }
}
