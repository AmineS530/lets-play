package com.letsplay.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Field("name")
    private String name;
    
    @Indexed(unique = true)
    @Field("email")
    private String email;
    
    @Field("password")
    private String password;
    
    @Field("role")
    private String role; // "USER" or "ADMIN"

    public String getPublicId() {
        return id;
    }
}
