package com.realmaverick.websocket.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestParam String username,
                                     @RequestParam String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            String token = jwtTokenService.generateToken(username);

            return Map.of("token", token, "type", "Bearer");
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            // Bad username/password
            return Map.of("token", "In valid creds");
        } catch (Exception ex) {
            // Any other error
            return Map.of("token", "App failed");
        }
    }



    @GetMapping("/hello")
    public String hello() {
        return "Hello, secured endpoint!";
    }
}
