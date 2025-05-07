package com.example.bdsqltester.dtos;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
    public long id;
    public String username;
    public String password;
    public String role;

    public User(long id, String name, String password, String role) {
        this.id = id;
        this.username = name;
        this.password = password;
        this.role = role;
    }

    public User(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.username = rs.getString("username");
        this.password = rs.getString("password");
        this.role = rs.getString("role");
    }

    public long getId() {
        return id;
    }
}
