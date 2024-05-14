package no.sintef.ocean.maritim.ciiservice;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseDataPoint {
    private double score;
    private String rating;
    private double durationHours;
    private int ciiYear;
    private String alarm;
    private Map<String, Double> ratingLimits;
    private double requiredScore;
}
