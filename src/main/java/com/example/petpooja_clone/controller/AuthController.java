package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.config.CognitoConfig;
import com.example.petpooja_clone.entity.User;
import com.example.petpooja_clone.repository.UserRepository;
import com.example.petpooja_clone.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CognitoConfig cognitoConfig;

    private CognitoIdentityProviderClient getCognitoClient() {
        var builder = CognitoIdentityProviderClient.builder()
                .region(Region.of(cognitoConfig.getRegion()));
        
        // Use static credentials if provided, otherwise use default credential chain
        if (cognitoConfig.getAwsCredentials().isConfigured()) {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                cognitoConfig.getAwsCredentials().getAccessKeyId(),
                cognitoConfig.getAwsCredentials().getSecretAccessKey()
            );
            builder.credentialsProvider(StaticCredentialsProvider.create(awsCreds));
        } else {
            // Try default credential chain (environment variables, AWS CLI config, IAM role, etc.)
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        
        return builder.build();
    }

    private String calculateSecretHash(String username) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    cognitoConfig.getClientSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);
            mac.update(username.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(cognitoConfig.getClientId().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating secret hash", e);
        }
    }

    /**
     * Safely assign user to group - creates group if it doesn't exist
     */
    private void assignUserToGroup(CognitoIdentityProviderClient cognitoClient, String email, String role) {
        if (role == null || role.isEmpty()) {
            return;
        }

        String groupName = role.toUpperCase();
        
        try {
            // First, try to get the group to see if it exists
            try {
                var getGroupRequest = software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest.builder()
                        .userPoolId(cognitoConfig.getUserPoolId())
                        .groupName(groupName)
                        .build();
                cognitoClient.getGroup(getGroupRequest);
                // Group exists, proceed to add user
            } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException e) {
                // Group doesn't exist, create it
                try {
                    var createGroupRequest = software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest.builder()
                            .userPoolId(cognitoConfig.getUserPoolId())
                            .groupName(groupName)
                            .description("Role group for " + groupName)
                            .build();
                    cognitoClient.createGroup(createGroupRequest);
                } catch (Exception createEx) {
                    // If creation fails (e.g., group already exists from another request), continue
                    System.err.println("Warning: Could not create group " + groupName + ": " + createEx.getMessage());
                }
            }

            // Now add user to group
            AdminAddUserToGroupRequest addToGroupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(cognitoConfig.getUserPoolId())
                    .username(email)
                    .groupName(groupName)
                    .build();
            cognitoClient.adminAddUserToGroup(addToGroupRequest);
        } catch (Exception e) {
            // Log but don't fail - group assignment is not critical for user creation
            System.err.println("Warning: Could not add user to group " + groupName + ": " + e.getMessage());
        }
    }

    @PostMapping("/cognito/signup")
    public ResponseEntity<Map<String, Object>> cognitoSignUp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            String password = request.get("password");
            String username = request.get("username");
            String role = request.get("role");

            if (email == null || password == null || username == null) {
                response.put("error", "Email, password, and username are required");
                return ResponseEntity.badRequest().body(response);
            }

            CognitoIdentityProviderClient cognitoClient = getCognitoClient();
            
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);
            if (cognitoConfig.getClientSecret() != null && !cognitoConfig.getClientSecret().isEmpty()) {
                authParams.put("SECRET_HASH", calculateSecretHash(email));
            }

            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(cognitoConfig.getClientId())
                    .username(email)
                    .password(password)
                    .userAttributes(
                            AttributeType.builder().name("email").value(email).build(),
                            AttributeType.builder().name("name").value(username).build()
                    )
                    .build();

            if (cognitoConfig.getClientSecret() != null && !cognitoConfig.getClientSecret().isEmpty()) {
                signUpRequest = signUpRequest.toBuilder()
                        .secretHash(calculateSecretHash(email))
                        .build();
            }

            SignUpResponse signUpResponse = cognitoClient.signUp(signUpRequest);

            // Always require confirmation (don't auto-confirm)
            // User must confirm via email confirmation code
            response.put("message", "User registered successfully. Please check your email for confirmation code.");
            response.put("confirmationRequired", true);
            response.put("userSub", signUpResponse.userSub());
            response.put("confirmed", false);
            return ResponseEntity.ok(response);

        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException e) {
            response.put("error", "User Pool not found. Please verify the User Pool ID and region in your configuration.");
            response.put("details", "User Pool ID: " + cognitoConfig.getUserPoolId() + ", Region: " + cognitoConfig.getRegion());
            response.put("suggestion", "Check your Cognito User Pool ID in AWS Console and update application.properties");
            return ResponseEntity.badRequest().body(response);
        } catch (UsernameExistsException e) {
            response.put("error", "User already exists");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("error", "Registration failed: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            if (e.getMessage().contains("does not exist")) {
                response.put("suggestion", "The User Pool ID '" + cognitoConfig.getUserPoolId() + "' may be incorrect. Please verify it in AWS Console.");
            }
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/cognito/signin")
    public ResponseEntity<Map<String, Object>> cognitoSignIn(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            String password = request.get("password");

            if (email == null || password == null) {
                response.put("error", "Email and password are required");
                return ResponseEntity.badRequest().body(response);
            }

            CognitoIdentityProviderClient cognitoClient = getCognitoClient();

            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);
            if (cognitoConfig.getClientSecret() != null && !cognitoConfig.getClientSecret().isEmpty()) {
                authParams.put("SECRET_HASH", calculateSecretHash(email));
            }

            // Try ADMIN_NO_SRP_AUTH first (requires auth flow to be enabled)
            AdminInitiateAuthResponse authResponse = null;
            Exception lastException = null;
            
            try {
                AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                        .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                        .userPoolId(cognitoConfig.getUserPoolId())
                        .clientId(cognitoConfig.getClientId())
                        .authParameters(authParams)
                        .build();
                authResponse = cognitoClient.adminInitiateAuth(authRequest);
            } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException e) {
                if (e.getMessage() != null && e.getMessage().contains("Auth flow not enabled")) {
                    // Try alternative: Use InitiateAuth with USER_PASSWORD_AUTH (if enabled)
                    lastException = e;
                    try {
                        InitiateAuthRequest initiateRequest = InitiateAuthRequest.builder()
                                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                                .clientId(cognitoConfig.getClientId())
                                .authParameters(authParams)
                                .build();
                        var initiateResponse = cognitoClient.initiateAuth(initiateRequest);
                        // Convert to AdminInitiateAuthResponse format
                        if (initiateResponse.authenticationResult() != null) {
                            response.put("idToken", initiateResponse.authenticationResult().idToken());
                            response.put("accessToken", initiateResponse.authenticationResult().accessToken());
                            response.put("refreshToken", initiateResponse.authenticationResult().refreshToken());
                            response.put("message", "Login successful");
                            return ResponseEntity.ok(response);
                        }
                    } catch (Exception e2) {
                        lastException = e2;
                    }
                } else {
                    throw e;
                }
            }
            
            if (authResponse == null) {
                throw lastException != null ? lastException : new RuntimeException("Authentication failed");
            }

            if (authResponse.authenticationResult() != null) {
                response.put("idToken", authResponse.authenticationResult().idToken());
                response.put("accessToken", authResponse.authenticationResult().accessToken());
                response.put("refreshToken", authResponse.authenticationResult().refreshToken());
                response.put("message", "Login successful");
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Authentication failed");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException e) {
            response.put("error", "User Pool not found. Please verify the User Pool ID and region in your configuration.");
            response.put("details", "User Pool ID: " + cognitoConfig.getUserPoolId() + ", Region: " + cognitoConfig.getRegion());
            response.put("suggestion", "Check your Cognito User Pool ID in AWS Console and update application.properties");
            return ResponseEntity.badRequest().body(response);
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException e) {
            if (e.getMessage() != null && e.getMessage().contains("Auth flow not enabled")) {
                response.put("error", "Authentication flow not enabled for this app client.");
                response.put("details", e.getMessage());
                response.put("suggestion", "Enable 'ALLOW_ADMIN_USER_PASSWORD_AUTH' or 'ALLOW_USER_PASSWORD_AUTH' in your Cognito App Client settings.");
                response.put("instructions", "Go to AWS Console → Cognito → Your User Pool → App integration → App clients → Edit your app client → Enable the required auth flows");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("error", "Invalid parameter: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (NotAuthorizedException e) {
            response.put("error", "Invalid credentials");
            return ResponseEntity.badRequest().body(response);
        } catch (UserNotConfirmedException e) {
            response.put("error", "User not confirmed. Please confirm your account using the confirmation code sent to your email.");
            response.put("confirmationRequired", true);
            response.put("suggestion", "Use /api/auth/cognito/confirm endpoint with the confirmation code from your email");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("error", "Login failed: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            if (e.getMessage() != null) {
                if (e.getMessage().contains("does not exist")) {
                    response.put("suggestion", "The User Pool ID '" + cognitoConfig.getUserPoolId() + "' may be incorrect. Please verify it in AWS Console.");
                } else if (e.getMessage().contains("Auth flow not enabled")) {
                    response.put("suggestion", "Enable 'ALLOW_ADMIN_USER_PASSWORD_AUTH' or 'ALLOW_USER_PASSWORD_AUTH' in your Cognito App Client settings.");
                }
            }
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/cognito/confirm")
    public ResponseEntity<Map<String, Object>> confirmSignUp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            String confirmationCode = request.get("confirmationCode");
            String role = request.get("role"); // Optional - role from frontend

            if (email == null || confirmationCode == null) {
                response.put("error", "Email and confirmation code are required");
                return ResponseEntity.badRequest().body(response);
            }

            CognitoIdentityProviderClient cognitoClient = getCognitoClient();

            ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
                    .clientId(cognitoConfig.getClientId())
                    .username(email)
                    .confirmationCode(confirmationCode)
                    .build();

            if (cognitoConfig.getClientSecret() != null && !cognitoConfig.getClientSecret().isEmpty()) {
                confirmRequest = confirmRequest.toBuilder()
                        .secretHash(calculateSecretHash(email))
                        .build();
            }

            cognitoClient.confirmSignUp(confirmRequest);

            // After confirmation, assign user to group if role is provided
            if (role != null && !role.isEmpty()) {
                assignUserToGroup(cognitoClient, email, role);
            }
            
            response.put("message", "User confirmed successfully. You can now login.");
            response.put("confirmed", true);
            return ResponseEntity.ok(response);

        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException e) {
            response.put("error", "Invalid confirmation code. Please check the code from your email.");
            return ResponseEntity.badRequest().body(response);
        } catch (software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException e) {
            response.put("error", "Confirmation code has expired. Please request a new code.");
            response.put("suggestion", "Use /api/auth/cognito/resend-code to get a new confirmation code");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("error", "Confirmation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/cognito/resend-code")
    public ResponseEntity<Map<String, Object>> resendConfirmationCode(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");

            if (email == null) {
                response.put("error", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }

            CognitoIdentityProviderClient cognitoClient = getCognitoClient();

            ResendConfirmationCodeRequest resendRequest = ResendConfirmationCodeRequest.builder()
                    .clientId(cognitoConfig.getClientId())
                    .username(email)
                    .build();

            if (cognitoConfig.getClientSecret() != null && !cognitoConfig.getClientSecret().isEmpty()) {
                resendRequest = resendRequest.toBuilder()
                        .secretHash(calculateSecretHash(email))
                        .build();
            }

            cognitoClient.resendConfirmationCode(resendRequest);

            response.put("message", "Confirmation code has been resent to your email.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", "Failed to resend confirmation code: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "User already exists!";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "User registered successfully!";
    }

    @PostMapping("/login")
    public String login(@RequestBody User loginReq) {
        Optional<User> user = userRepository.findByEmail(loginReq.getEmail());
        if (user.isPresent() && passwordEncoder.matches(loginReq.getPassword(), user.get().getPassword())) {
            String token = jwtUtil.generateToken(user.get().getEmail(), user.get().getRole());
            return token;
        }
        return "Invalid credentials!";
    }

}

