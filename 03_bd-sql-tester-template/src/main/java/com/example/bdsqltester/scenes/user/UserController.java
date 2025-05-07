package com.example.bdsqltester.scenes.user;
import com.example.bdsqltester.HelloApplication;
import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
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
import java.util.Arrays;
import java.util.Collections;

public class UserController {

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

        nameField.setEditable(false);
        nameField.setMouseTransparent(true);
        nameField.setFocusTraversable(false);

        instructionsField.setEditable(false);
        instructionsField.setMouseTransparent(true);
        instructionsField.setFocusTraversable(false);

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
                    answerKeyField.clear();
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
            int userId = HelloApplication.getApplicationInstance().getUserId(); // Ambil user_id yang disimpan saat login

            double storedGrade = getGradeFromDatabase(userId, Integer.parseInt(idField.getText())); // Ambil grade berdasarkan user_id dan assignment_id

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Hasil Penilaian");
            alert.setHeaderText(null);
            alert.setContentText("Nilai Anda: " + storedGrade);
            alert.showAndWait();
        }
    }
    int userId = HelloApplication.getApplicationInstance().getUserId();

    @FXML
    void onTestButtonClick(ActionEvent event) {
        Stage stage = new Stage();
        stage.setTitle("Query Results");

        TableView<ObservableList<String>> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Agar kolom menyesuaikan lebar

        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(answerKeyField.getText())) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 1. Buat kolom dinamis berdasarkan hasil query
            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1;

                // Gunakan nama kolom dari ResultSetMetaData
                String columnName = metaData.getColumnLabel(i);
                if (columnName == null || columnName.isEmpty()) {
                    columnName = "Column " + i; // Fallback jika nama kolom kosong
                }

                TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
                column.setCellValueFactory(param -> {
                    ObservableList<String> row = param.getValue();
                    return new SimpleStringProperty(row.get(columnIndex));
                });

                // Atur lebar kolom otomatis
                column.setPrefWidth(150);
                tableView.getColumns().add(column);
            }

            // 2. Isi data
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i) != null ? rs.getString(i) : "NULL");
                }
                data.add(row);
            }

            if (data.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No Results",
                        "Query executed but returned no data", "");
                return;
            }

            tableView.setItems(data);

            // 3. Tambahkan scroll pane untuk hasil yang panjang
            ScrollPane scrollPane = new ScrollPane(tableView);
            scrollPane.setFitToWidth(true);

            Scene scene = new Scene(scrollPane, 800, 600);
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Failed to execute query", e.getMessage());
        }
    }

    // Helper method untuk menampilkan alert
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void onSubmitClick(ActionEvent event) {
        // Ambil assignment_id dari idField (Assignment yang dipilih)
        int assignmentId = Integer.parseInt(idField.getText());

        // Ambil hasil dari query yang dijalankan oleh user
        String userQueryResult = executeQuery(answerKeyField.getText());

        // Ambil kunci jawaban dari database berdasarkan assignment_id
        String answerKeyResult = executeQuery(getAnswerKeyFromDatabase(assignmentId));

        // Bandingkan hasil query user dengan kunci jawaban
        int grade = compareResults(userQueryResult, answerKeyResult);
        // Debugging: Cetak nilai grade yang dihitung
        System.out.println("Grade: " + grade);


        // Menyimpan nilai ke tabel jika perlu
        saveGradeToDatabase(grade);
    }

    private String getAnswerKeyFromDatabase(int assignmentId) {
        String answerKey = "";

        // Query untuk mengambil kunci jawaban dari database berdasarkan assignment_id
        try (Connection conn = GradingDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT answer_key FROM assignments WHERE id = ?")) {

            stmt.setInt(1, assignmentId);  // Set assignment_id yang dipilih oleh user
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                answerKey = rs.getString("answer_key");  // Ambil answer_key dari hasil query
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return answerKey;  // Kembalikan kunci jawaban
    }



    // Fungsi untuk menjalankan query dan mendapatkan hasilnya
    private String executeQuery(String query) {
        StringBuilder result = new StringBuilder();

        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Mengambil semua kolom dan menambahkannya ke hasil
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    result.append(rs.getString(i)).append("\t");
                }
                result.append("\n");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            result.append("Error executing query: ").append(e.getMessage());
        }
        // Debugging: Cetak hasil query yang dieksekusi
        System.out.println("User Query Result: " + result.toString());
        return result.toString();
    }

    // Fungsi untuk membandingkan hasil query dan kunci jawaban
    private int compareResults(String userResult, String answerKeyResult) {
        // Menghapus spasi tambahan, newline, dan tab di dalam hasil query dan kunci jawaban
        String[] userRows = cleanString(userResult).split("\n");
        String[] answerKeyRows = cleanString(answerKeyResult).split("\n");

        // Debugging: Cetak hasil untuk melihat apakah mereka sesuai dengan yang diharapkan
        System.out.println("User Result: " + Arrays.toString(userRows));
        System.out.println("Answer Key: " + Arrays.toString(answerKeyRows));

        // Memeriksa apakah isi dan urutan sama
        if (Arrays.equals(userRows, answerKeyRows)) {
            return 100; // Isi dan urutan cocok
        }

        // Memeriksa apakah hanya isi yang cocok (tidak peduli urutan)
        ArrayList<String> userList = new ArrayList<>(Arrays.asList(userRows));
        ArrayList<String> answerKeyList = new ArrayList<>(Arrays.asList(answerKeyRows));

        // Periksa jika hanya isi yang cocok tetapi urutan tidak
        if (userList.size() == answerKeyList.size() && userList.containsAll(answerKeyList)) {
            return 50; // Isi cocok tetapi urutan tidak cocok
        }

        return 0; // Tidak cocok
    }

    // Fungsi untuk membersihkan hasil query dan kunci jawaban dari karakter tambahan
    private String cleanString(String str) {
        return str.replaceAll("[\\n\\t\\r]+", " ").trim(); // Mengganti tab, newline dengan spasi
    }



    // Fungsi untuk menyimpan nilai ke dalam database (atau logika penyimpanan lain)
    private void saveGradeToDatabase(int grade) {
        // Ambil assignment_id dari idField (misalnya, ID dari assignment yang sedang dipilih)
        int assignmentId = Integer.parseInt(idField.getText());  // Pastikan idField berisi assignment_id yang benar

        // Logika untuk menyimpan nilai ke dalam tabel grade
        try (Connection conn = GradingDataSource.getConnection()) {
            String sql = "INSERT INTO grades (user_id, assignment_id, grade) " +
                    "VALUES (?, ?, ?) " +
                    "ON CONFLICT (user_id, assignment_id) DO UPDATE SET grade = EXCLUDED.grade";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Set parameter untuk query insert
                stmt.setInt(1, userId);         // user_id
                stmt.setInt(2, assignmentId);      // assignment_id
                stmt.setInt(3, grade);             // grade

                // Menjalankan query
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private double getGradeFromDatabase(int userId, int assignmentId) {
        double storedGrade = 0;
        try (Connection conn = GradingDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT grade FROM grades WHERE user_id = ? AND assignment_id = ?")) {

            stmt.setInt(1, userId);
            stmt.setInt(2, assignmentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                storedGrade = rs.getDouble("grade");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return storedGrade;
    }


    @FXML
    private void onShowYourFinalGradeClick(ActionEvent event) {

        // Ambil rata-rata nilai berdasarkan user_id
        double finalGrade = getFinalGradeFromDatabase(userId);

        // Menampilkan rata-rata nilai dalam sebuah Alert atau popup
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Your Final Grade");
        alert.setHeaderText(null);
        alert.setContentText("Your final grade is: " + finalGrade);
        alert.showAndWait();
    }

    private double getFinalGradeFromDatabase(int userId) {
        double finalGrade = 0;

        // Query untuk menghitung rata-rata nilai
        String query = "SELECT COALESCE(SUM(g.grade), 0) / (SELECT COUNT(id) FROM assignments) AS average_grade " +
                "FROM grades g " +
                "JOIN assignments a ON g.assignment_id = a.id " +
                "WHERE g.user_id = ?";

        try (Connection conn = GradingDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);  // Set user_id yang login

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                finalGrade = rs.getDouble("average_grade");  // Ambil rata-rata nilai
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return finalGrade;
    }


}
