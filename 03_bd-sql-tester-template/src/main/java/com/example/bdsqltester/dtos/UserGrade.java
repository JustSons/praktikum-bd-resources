package com.example.bdsqltester.dtos;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class UserGrade {

    private final SimpleStringProperty username;
    private final SimpleIntegerProperty grade;

    public UserGrade(String username, int grade) {
        this.username = new SimpleStringProperty(username);
        this.grade = new SimpleIntegerProperty(grade);
    }

    public String getUsername() {
        return username.get();
    }

    public SimpleStringProperty usernameProperty() {
        return username;
    }

    public int getGrade() {
        return grade.get();
    }

    public SimpleIntegerProperty gradeProperty() {
        return grade;
    }
}
