package com.example.bdsqltester.dtos;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;


public class UserGrade {
    private SimpleStringProperty username;
    private SimpleIntegerProperty grade;
    private SimpleDoubleProperty averageGrade;
    private SimpleIntegerProperty totalAssignments;

    public UserGrade(String username, int grade) {
        this.username = new SimpleStringProperty(username);
        this.grade = new SimpleIntegerProperty(grade);
    }

    public UserGrade(String username, int totalAssignments, double averageGrade) {
        this.username = new SimpleStringProperty(username);
        this.totalAssignments = new SimpleIntegerProperty(totalAssignments);
        this.averageGrade = new SimpleDoubleProperty(averageGrade);
    }

    public int getTotalAssignments() {
        return totalAssignments.get();
    }

    public SimpleIntegerProperty totalAssignmentsProperty() {
        return totalAssignments;
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

    public double getAverageGrade() {
        return averageGrade.get();
    }

    public SimpleDoubleProperty averageGradeProperty() {
        return averageGrade;
    }
}
