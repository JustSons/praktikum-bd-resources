package com.example.bdsqltester.scenes.admin;

import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import com.example.bdsqltester.dtos.UserGrade;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;

public class AdminController {

    @FXML
    private TextArea answerKeyField;

    @FXML
    private ListView<Assignment> assignmentList = new ListView<>();

    @FXML
    private TextField idField;

    @FXML
    private TextArea instructionsField;

    @FXML
    private TextField nameField;

    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        // Set idField to read-only
        idField.setEditable(false);
        idField.setMouseTransparent(true);
        idField.setFocusTraversable(false);

        // Populate the ListView with assignment names
        refreshAssignmentList();

        assignmentList.setCellFactory(param -> new ListCell<Assignment>() {
            @Override
            protected void updateItem(Assignment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name);
                }
            }

            // Bind the onAssignmentSelected method to the ListView
            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected) {
                    onAssignmentSelected(getItem());
                }
            }
        });
    }

    void refreshAssignmentList() {
        // Clear the current list
        assignments.clear();

        // Re-populate the ListView with assignment names
        try (Connection c = MainDataSource.getConnection()) {
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM assignments");

            while (rs.next()) {
                // Create a new assignment object
                assignments.addAll(new Assignment(rs));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Database Error");
            alert.setContentText(e.toString());
        }

        // Set the ListView to display assignment names
        assignmentList.setItems(assignments);

        // Set currently selected to the id inside the id field
        // This is inefficient, you can optimize this.
        try {
            if (!idField.getText().isEmpty()) {
                long id = Long.parseLong(idField.getText());
                for (Assignment assignment : assignments) {
                    if (assignment.id == id) {
                        assignmentList.getSelectionModel().select(assignment);
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            // Ignore, idField is empty
        }
    }

    void onAssignmentSelected(Assignment assignment) {
        // Set the id field
        idField.setText(String.valueOf(assignment.id));

        // Set the name field
        nameField.setText(assignment.name);

        // Set the instructions field
        instructionsField.setText(assignment.instructions);

        // Set the answer key field
        answerKeyField.setText(assignment.answerKey);
    }

    @FXML
    void onNewAssignmentClick(ActionEvent event) {
        // Clear the contents of the id field
        idField.clear();

        // Clear the contents of all text fields
        nameField.clear();
        instructionsField.clear();
        answerKeyField.clear();
    }

    @FXML
    void onSaveClick(ActionEvent event) {
        // If id is set, update, else insert
        if (idField.getText().isEmpty()) {
            // Insert new assignment
            try (Connection c = MainDataSource.getConnection()) {
                PreparedStatement stmt = c.prepareStatement("INSERT INTO assignments (name, instructions, answer_key) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, nameField.getText());
                stmt.setString(2, instructionsField.getText());
                stmt.setString(3, answerKeyField.getText());
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    // Get generated id, update idField
                    idField.setText(String.valueOf(rs.getLong(1)));
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Database Error");
                alert.setContentText(e.toString());
            }
        } else {
            // Update existing assignment
            try (Connection c = MainDataSource.getConnection()) {
                PreparedStatement stmt = c.prepareStatement("UPDATE assignments SET name = ?, instructions = ?, answer_key = ? WHERE id = ?");
                stmt.setString(1, nameField.getText());
                stmt.setString(2, instructionsField.getText());
                stmt.setString(3, answerKeyField.getText());
                stmt.setInt(4, Integer.parseInt(idField.getText()));
                stmt.executeUpdate();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Database Error");
                alert.setContentText(e.toString());
            }
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Save");
        alert.setHeaderText("Tugas " + nameField.getText() +" Berhasil disave!");
        alert.showAndWait();
        // Refresh the assignment list
        refreshAssignmentList();
    }

    @FXML
    void onShowGradesClick(ActionEvent event) {
        // Make sure id is set
        if (idField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Assignment Selected");
            alert.setContentText("Please select an assignment to view grades.");
            alert.showAndWait();
            return;
        }else {
            // Ambil assignment_id dari assignment yang dipilih
            int assignmentId = Integer.parseInt(idField.getText());

            // Panggil method untuk menampilkan window baru
            showGradesWindow(assignmentId);
        }
    }

    private void showGradesWindow(int assignmentId) {
        Stage gradesStage = new Stage();
        gradesStage.setTitle("Grades for Assignment");

        TableView<UserGrade> gradesTable = new TableView<>();
        TableColumn<UserGrade, String> usernameColumn = new TableColumn<>("Username");
        TableColumn<UserGrade, Integer> gradeColumn = new TableColumn<>("Grade");

        // Set kolom untuk TableView
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        gradeColumn.setCellValueFactory(cellData -> cellData.getValue().gradeProperty().asObject());

        gradesTable.getColumns().add(usernameColumn);
        gradesTable.getColumns().add(gradeColumn);

        // Ambil data nilai user berdasarkan assignment_id
        ObservableList<UserGrade> grades = fetchUserGradesForAssignment(assignmentId);

        // Set data ke dalam table
        gradesTable.setItems(grades);

        StackPane root = new StackPane();
        root.getChildren().add(gradesTable);
        Scene scene = new Scene(root, 800, 600);
        gradesStage.setScene(scene);
        gradesStage.show();
    }

    @FXML
    void onDeleteClick(ActionEvent event) {
        // Pastikan assignment_id sudah ada
        if (idField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Assignment Selected");
            alert.setContentText("Please select an assignment to delete.");
            alert.showAndWait();
            return;
        }

        // Tampilkan konfirmasi sebelum menghapus
        Alert confirmDeleteAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDeleteAlert.setTitle("Delete Assignment");
        confirmDeleteAlert.setHeaderText("Are you sure you want to delete this assignment?");
        confirmDeleteAlert.setContentText("This action cannot be undone.");

        // Jika admin menekan "OK", lakukan penghapusan
        confirmDeleteAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int assignmentId = Integer.parseInt(idField.getText());
                deleteAssignment(assignmentId);
            }
        });
    }

    private void deleteAssignment(int assignmentId) {
        try (Connection c = MainDataSource.getConnection()) {
            // 1. Hapus nilai yang terkait dengan assignment terlebih dahulu
            PreparedStatement deleteGradesStmt = c.prepareStatement("DELETE FROM grades WHERE assignment_id = ?");
            deleteGradesStmt.setInt(1, assignmentId);
            int gradesDeleted = deleteGradesStmt.executeUpdate();
//            System.out.println("Deleted " + gradesDeleted + " grades for assignmentId: " + assignmentId);

            // 2. Hapus assignment setelah nilai terkait dihapus
            PreparedStatement deleteAssignmentStmt = c.prepareStatement("DELETE FROM assignments WHERE id = ?");
            deleteAssignmentStmt.setInt(1, assignmentId);
            int assignmentDeleted = deleteAssignmentStmt.executeUpdate();

            if (assignmentDeleted > 0) {
                // Menampilkan pesan sukses
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Assignment Deleted");
                alert.setContentText("Assignment has been successfully deleted.");
                alert.showAndWait();

                // Refresh list untuk memperbarui tampilan
                refreshAssignmentList();
            } else {
                // Jika tidak ada baris yang terhapus (misalnya, jika assignment_id tidak ditemukan)
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Delete Failed");
                alert.setContentText("Assignment could not be found or deleted.");
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Failed to delete assignment.");
            alert.setContentText("SQL Error: " + e.getMessage());
            alert.showAndWait();
        }
    }



    private ObservableList<UserGrade> fetchUserGradesForAssignment(int assignmentId) {
        ObservableList<UserGrade> grades = FXCollections.observableArrayList();

        try (Connection conn = MainDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT u.username, g.grade " +
                     "FROM grades g " +
                     "JOIN users u ON g.user_id = u.id " +
                     "WHERE g.assignment_id = ? ORDER BY g.user_id")) {

            stmt.setInt(1, assignmentId);  // Set assignment_id yang dipilih oleh admin
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                int grade = rs.getInt("grade");

                // Debugging output
//                System.out.println("Fetched Username: " + username + ", Grade: " + grade);

                // Tambahkan hasil query ke ObservableList
                grades.add(new UserGrade(username, grade));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Verifikasi apakah ObservableList telah terisi dengan benar
        if (grades.isEmpty()) {
//            System.out.println("No grades found for assignmentId: " + assignmentId);
        }

        return grades;
    }



    @FXML
    void onTestButtonClick(ActionEvent event) {
        // Display a window containing the results of the query.

        // Create a new window/stage
        Stage stage = new Stage();
        stage.setTitle("Query Results");

        // Display in a table view.
        TableView<ArrayList<String>> tableView = new TableView<>();

        ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
        ArrayList<String> headers = new ArrayList<>(); // To check if any columns were returned

        // Use try-with-resources for automatic closing of Connection, Statement, ResultSet
        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(answerKeyField.getText())) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 1. Get Headers and Create Table Columns
            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1; // Need final variable for lambda (0-based index for ArrayList)
                String headerText = metaData.getColumnLabel(i); // Use label for potential aliases
                headers.add(headerText); // Keep track of headers

                TableColumn<ArrayList<String>, String> column = new TableColumn<>(headerText);

                // Define how to get the cell value for this column from an ArrayList<String> row object
                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    // Ensure rowData exists and the index is valid before accessing
                    if (rowData != null && columnIndex < rowData.size()) {
                        return new SimpleStringProperty(rowData.get(columnIndex));
                    } else {
                        return new SimpleStringProperty(""); // Should not happen with current logic, but safe fallback
                    }
                });
                column.setPrefWidth(120); // Optional: set a preferred width
                tableView.getColumns().add(column);
            }

            // 2. Get Data Rows
            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    // Retrieve all data as String. Handle NULLs gracefully.
                    String value = rs.getString(i);
                    row.add(value != null ? value : ""); // Add empty string for SQL NULL
                }
                data.add(row);
            }

            // 3. Check if any results (headers or data) were actually returned
            if (headers.isEmpty() && data.isEmpty()) {
                // Handle case where query might be valid but returns no results
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Query Results");
                infoAlert.setHeaderText(null);
                infoAlert.setContentText("The query executed successfully but returned no data.");
                infoAlert.showAndWait();
                return; // Exit the method, don't show the empty table window
            }

            // 4. Set the data items into the table
            tableView.setItems(data);

            // 5. Create layout and scene
            StackPane root = new StackPane();
            root.getChildren().add(tableView);
            Scene scene = new Scene(root, 800, 600); // Adjust size as needed

            // 6. Set scene and show stage
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            // Log the error and show an alert to the user
            e.printStackTrace(); // Print stack trace to console/log for debugging
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Database Error");
            errorAlert.setHeaderText("Failed to execute query or retrieve results.");
            errorAlert.setContentText("SQL Error: " + e.getMessage());
            errorAlert.showAndWait();
        } catch (Exception e) {
            // Catch other potential exceptions (e.g., class loading if driver not found)
            e.printStackTrace(); // Print stack trace to console/log for debugging
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("An unexpected error occurred.");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        }
    } // End of onTestButtonClick method

    private ObservableList<UserGrade> fetchFinalGradesForUsers() {
        ObservableList<UserGrade> grades = FXCollections.observableArrayList();

        try (Connection conn = MainDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT u.username, " +
                             "(SELECT COUNT(id) FROM assignments) AS total_assignments, " +
                             "COALESCE(SUM(g.grade), 0) AS total_grade, " +
                             "CASE " +
                             "WHEN COUNT(DISTINCT a.id) = 0 THEN 0 " +
                             "ELSE COALESCE(SUM(g.grade), 0) / (SELECT COUNT(id) FROM assignments) " +
                             "END AS average_grade " +
                             "FROM users u " +
                             "LEFT JOIN grades g ON u.id = g.user_id " +
                             "LEFT JOIN assignments a ON g.assignment_id = a.id " +
                             "WHERE u.username != 'admin' " +
                             "GROUP BY u.username " +
                             "ORDER BY u.username")) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                double averageGrade = rs.getDouble("average_grade");
                int totalAssignments = rs.getInt("total_assignments");

                // Tambahkan hasil query ke ObservableList
                grades.add(new UserGrade(username, totalAssignments, averageGrade));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return grades;
    }

    @FXML
    void onShowFinalGradeClick(ActionEvent event) {
        Stage gradesStage = new Stage();
        gradesStage.setTitle("Final Grades for Users");

        TableView<UserGrade> finalGradesTable = new TableView<>();
        TableColumn<UserGrade, String> usernameColumn = new TableColumn<>("Username");
//        TableColumn<UserGrade, Integer> totalAssignmentsColumn = new TableColumn<>("Total Assignments");
        TableColumn<UserGrade, Double> averageGradeColumn = new TableColumn<>("Average Grade");

        // Set kolom untuk TableView
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
//        totalAssignmentsColumn.setCellValueFactory(cellData -> cellData.getValue().totalAssignmentsProperty().asObject());
        averageGradeColumn.setCellValueFactory(cellData -> cellData.getValue().averageGradeProperty().asObject());

        finalGradesTable.getColumns().add(usernameColumn);
//        finalGradesTable.getColumns().add(totalAssignmentsColumn);
        finalGradesTable.getColumns().add(averageGradeColumn);

        // Ambil data nilai rata-rata untuk semua user
        ObservableList<UserGrade> finalGrades = fetchFinalGradesForUsers();

        // Set data ke dalam table
        finalGradesTable.setItems(finalGrades);

        StackPane root = new StackPane();
        root.getChildren().add(finalGradesTable);
        Scene scene = new Scene(root, 800, 600);
        gradesStage.setScene(scene);
        gradesStage.show();
    }

    private double fetchAverageGradeForAssignment(int assignmentId) {
        double averageGrade = 0.0;

        // Query untuk menghitung rata-rata nilai dari semua user pada assignment yang dipilih
        try (Connection conn = MainDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COALESCE(AVG(g.grade), 0) AS average_grade " +
                             "FROM grades g " +
                             "WHERE g.assignment_id = ?")) {

            stmt.setInt(1, assignmentId); // Menyaring berdasarkan assignment_id yang dipilih
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                averageGrade = rs.getDouble("average_grade");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return averageGrade;
    }

    @FXML
    void onShowAverageAssignmentClick(ActionEvent event) {
        // Pastikan assignment_id sudah ada
        if (idField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Assignment Selected");
            alert.setContentText("Please select an assignment to view the average grade.");
            alert.showAndWait();
            return;
        }

        int assignmentId = Integer.parseInt(idField.getText());

        // Ambil rata-rata nilai dari assignment yang dipilih
        double averageGrade = fetchAverageGradeForAssignment(assignmentId);

        // Tampilkan rata-rata nilai dalam sebuah alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Average Grade for Assignment");
        alert.setHeaderText("Average Grade for Assignment");
        alert.setContentText("The average grade for this assignment is: " + averageGrade);
        alert.showAndWait();
    }
}
