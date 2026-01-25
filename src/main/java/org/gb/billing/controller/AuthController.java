package org.gb.billing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.gb.billing.config.JwtProperties;
import org.gb.billing.dto.request.LoginRequest;
import org.gb.billing.dto.request.RegisterRequest;
import org.gb.billing.dto.response.AuthResponse;
import org.gb.billing.dto.response.ErrorResponse;
import org.gb.billing.entity.User;
import org.gb.billing.service.AuthenticationService;
import org.gb.billing.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Handles user registration and login operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration and login endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final JwtProperties jwtProperties;

    public AuthController(UserService userService, AuthenticationService authenticationService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Register a new user.
     * 
     * @param request registration request containing email, password, role, and optional tenant/name info
     * @return the created user (without password)
     */
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with the specified role (ROLE_ADMIN or ROLE_USER). " +
                     "ROLE_USER requires a tenantName for multi-tenant isolation."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User successfully registered",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email already registered",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Registration request received for email: {}", request.getEmail());

        User user = userService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getRole(),
                request.getTenantName(),
                request.getFirstName(),
                request.getLastName()
        );

        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getTenantId(),
                user.getFirstName(),
                user.getLastName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate a user and return a JWT token.
     * 
     * @param request login request containing email and password
     * @return authentication response with JWT token
     */
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates a user with email and password, returning a JWT token for subsequent API calls."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials or account disabled",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for email: {}", request.getEmail());

        String token = authenticationService.login(request.getEmail(), request.getPassword());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setExpiresIn(jwtProperties.getExpiration());

        return ResponseEntity.ok(response);
    }

    /**
     * DTO for user response (registration).
     * Excludes sensitive information like password.
     */
    public static class UserResponse {
        private Long id;
        private String email;
        private String role;
        private Long tenantId;
        private String firstName;
        private String lastName;

        public UserResponse(Long id, String email, String role, Long tenantId, String firstName, String lastName) {
            this.id = id;
            this.email = email;
            this.role = role;
            this.tenantId = tenantId;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        // Getters
        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public Long getTenantId() { return tenantId; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
    }
}
