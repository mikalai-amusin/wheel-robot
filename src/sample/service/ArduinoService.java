package sample.service;

public interface ArduinoService {
    String scanDevice();
    void connect(String deviceUrl);
    void sendData(String data);
}
