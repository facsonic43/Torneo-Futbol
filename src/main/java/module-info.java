module com.example.frontendchampionship {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.frontendchampionship to javafx.fxml;
    exports com.example.frontendchampionship;
}