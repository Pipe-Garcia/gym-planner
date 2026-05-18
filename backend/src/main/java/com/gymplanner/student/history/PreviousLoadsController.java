package com.gymplanner.student.history;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.student.history.dto.PreviousLoadsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PreviousLoadsController {
    private final PreviousLoadsService previousLoadsService;

    @GetMapping("/api/students/{studentId}/exercises/{exerciseId}/previous-loads")
    @Operation(summary = "Consulta cargas previas de un ejercicio para un alumno",
            description = "Devuelve las ultimas apariciones no-borrador de un ejercicio en rutinas del alumno autenticado por gym.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial encontrado o respuesta vacia",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "exerciseId": 15,
                              "exerciseName": "Sentadilla Goblet",
                              "found": true,
                              "occurrences": [
                                {
                                  "routineId": 22,
                                  "routineName": "Plantilla Voley - Potencia",
                                  "routineStatus": "FINISHED",
                                  "assignedDate": "2026-05-12",
                                  "finishedDate": "2026-06-12",
                                  "dayOrderIndex": 1,
                                  "dayName": "Dia 1 - Potencia y Fuerza",
                                  "blockTitle": "Piramide Invertida - Sentadilla",
                                  "blockStructuralType": "REVERSE_PYRAMID",
                                  "blockPurpose": "MAIN_LIFT",
                                  "exerciseNotes": null,
                                  "measurementType": "REPS_WEIGHT",
                                  "sets": [
                                    {
                                      "setNumber": 1,
                                      "setKind": "NORMAL",
                                      "targetReps": 6,
                                      "targetRepsMin": null,
                                      "targetRepsMax": null,
                                      "targetWeightKg": 80,
                                      "targetTimeSeconds": null,
                                      "targetDistanceMeters": null,
                                      "restAfterSeconds": 90,
                                      "rpe": null,
                                      "toFailure": false
                                    }
                                  ]
                                }
                              ]
                            }
                            """))),
            @ApiResponse(responseCode = "401", description = "Autenticacion requerida"),
            @ApiResponse(responseCode = "404", description = "Alumno o ejercicio no encontrado")
    })
    PreviousLoadsResponse getPreviousLoads(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long studentId,
            @PathVariable Long exerciseId,
            @Parameter(description = "Rutina a excluir de la busqueda para evitar autorreferencias")
            @RequestParam(required = false) Long excludeRoutineId,
            @Parameter(description = "Cantidad de apariciones a devolver. Default 1, maximo 3.")
            @RequestParam(required = false) Integer limit) {
        return previousLoadsService.getPreviousLoads(principal.gymId(), studentId, exerciseId, excludeRoutineId, limit);
    }
}
