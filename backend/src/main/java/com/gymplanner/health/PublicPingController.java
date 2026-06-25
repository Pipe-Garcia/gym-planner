package com.gymplanner.health;

import com.gymplanner.health.dto.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicPingController {

    @GetMapping("/api/public/ping")
    @Operation(summary = "Keep-alive publico liviano")
    @ApiResponse(responseCode = "200", description = "Backend disponible")
    PingResponse ping() {
        return new PingResponse("ok");
    }
}
