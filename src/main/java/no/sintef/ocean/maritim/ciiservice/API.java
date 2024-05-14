package no.sintef.ocean.maritim.ciiservice;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public interface API {
        @PostMapping(value = "/cii/", consumes = { "application/json" }, produces = { "application/json" })
        // BULK_CARRIER_LARGE, BULK_CARRIER_SMALL, GAS_CARRIER_LARGE, GAS_CARRIER_SMALL,
        // TANKER, CONTAINER_SHIP, GENERAL_CARGO_SHIP_LARGE, GENERAL_CARGO_SHIP_SMALL,
        // REFRIGERATED_CARGO_CARRIER, COMBINATION_CARRIER, LNG_CARRIER_LARGE,
        // LNG_CARRIER_MEDIUM, LNG_CARRIER_SMALL, RORO_CARGO_SHIP_VEHICLE_CARRIER_LARGE,
        // RORO_CARGO_SHIP_VEHICLE_CARRIER_MEDIUM,
        // RORO_CARGO_SHIP_VEHICLE_CARRIER_SMALL, RORO_CARGO_SHIP, RORO_PASSENGER_SHIP,
        // RORO_PASSENGER_SHIP_SOLASCHAPTERX, CRUISE_PASSENGER_SHIP
        @Operation(summary = "Calculate CII", description = "Based on ship type (BULK, GAS, TANKER, CONTAINER, GENERAL, REFRIGERATED, COMBINATION, LNG, RORO_VEHICLE, RORO_CARGO, RORO_PASSENGER, RORO_PASSENGER_SOLASCHAPTERX, CRUISE), distance sailed and fuel type (DIESEL, LFO, HFO, LPG_PROPANE, LPG_BUTANE, ETHANE, LNG, METHANOL, ETHANOL) and consumption, calculate the CII", security = {
                        @SecurityRequirement(name = "ApiKeyAuth") })
        @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "CII calculation success"),
                        @ApiResponse(responseCode = "403", description = "Access denied"),
                        @ApiResponse(responseCode = "422", description = "Unprocessable entity"),
                        @ApiResponse(responseCode = "500", description = "Internal server error") })
        ResponseEntity<?> calculateCII(
                        @Parameter(description = "Input data", required = true) @RequestBody CIIRequest ciiRequest,
                        @Parameter(description = "CII calculation horizon. 'short', 'medium', 'long' or 'year-to-date'", in = ParameterIn.QUERY) Optional<String> horizon);
}
