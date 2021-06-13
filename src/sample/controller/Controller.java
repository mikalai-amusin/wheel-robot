package sample.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import sample.model.TimeSeriesDTO;
import sample.service.*;
import sample.utils.Constants;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public Button upButton;
    public Button leftButton;
    public Button rightButton;
    public Button downButton;
    public Button stopButton;
    public Button connectButton;
    public Button getDataButton;
    public TextField ipField;
    public Button startMonitoringButton;

    private ESP32Service esp32Service;
    private ArduinoService arduinoService;
    private MonitoringService monitoringService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObjectMapper objectMapper = new ObjectMapper();
        arduinoService = new ArduinoServiceImpl();
        esp32Service = new ESP32ServiceImpl(objectMapper);
        monitoringService = new MonitoringServiceImpl(arduinoService, esp32Service, objectMapper);
        try {
            String arduinoUrl = arduinoService.scanDevice();
            arduinoService.connect(arduinoUrl);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Health check");
            alert.setContentText("Connection successful, url: " + arduinoUrl);
            alert.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Health check");
            alert.setContentText("Connection failed: " + e.getMessage());
            alert.show();
        }
    }

    public void up() {
        arduinoService.sendData(Constants.FORWARD);
    }

    public void left() {
        arduinoService.sendData(Constants.LEFT);
    }

    public void right() {
        arduinoService.sendData(Constants.RIGHT);
    }

    public void down() {
        arduinoService.sendData(Constants.BACK);
    }

    public void stop() {
        arduinoService.sendData(Constants.STOP);
    }

    public void connect() {
        String ip = ipField.getText();

        if (esp32Service.healthy(ip)) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Connection");
            alert.setContentText("Connection successful");
            alert.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Results");
            alert.setContentText("Connection failed!");
            alert.show();
        }
    }

    public void getData() {
        String ip = ipField.getText();
        List<TimeSeriesDTO> data = esp32Service.getData(ip);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Results");
        alert.setContentText("Data retrieved: " + data);
        alert.show();
    }

    public void startMonitoring() {
        monitoringService.startMonitoring(ipField.getText());
    }
}