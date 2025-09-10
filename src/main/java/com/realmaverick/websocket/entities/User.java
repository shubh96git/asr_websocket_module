package com.realmaverick.websocket.entities;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User {


    private Integer userId;


    private String firstName;

    private String lastName;

    private String username;

    private String password;

    private String email;

    private String mobile;

    private String role;

}