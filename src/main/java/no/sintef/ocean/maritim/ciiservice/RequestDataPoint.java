package no.sintef.ocean.maritim.ciiservice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestDataPoint {
    private int ciiYear;
    private double durationHours;
    private double sailedDistanceNm;
    private double mdo;
    private double ifo;
    private double hfo;
    private double meCons;
}
