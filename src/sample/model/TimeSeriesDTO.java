package sample.model;

import lombok.Data;

@Data
public class TimeSeriesDTO {
    private Double temp;
    private Double humid;
    private Double pressure;
    private String date;

}
