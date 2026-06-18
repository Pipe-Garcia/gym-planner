package com.gymplanner.exercise.tag;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import com.gymplanner.exercise.tag.dto.CreateTagRequest;
import com.gymplanner.exercise.tag.dto.TagResponse;
import com.gymplanner.exercise.tag.dto.TagUsageResponse;
import com.gymplanner.exercise.tag.dto.UpdateTagRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercise-tags")
@RequiredArgsConstructor
public class ExerciseTagController {

    private final ExerciseTagService tagService;

    @GetMapping
    List<TagResponse> list(@AuthenticationPrincipal GymPrincipal principal, @RequestParam(required = false) TagType type) {
        return tagService.list(principal.gymId(), type);
    }

    @GetMapping("/usage")
    @Operation(
            summary = "Lista tags de ejercicios con cantidad de usos",
            description = "Devuelve todos los tags del gimnasio autenticado y la cantidad de ejercicios asignados, con filtro opcional por tipo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tags y conteos obtenidos correctamente"),
            @ApiResponse(responseCode = "401", description = "Autenticacion requerida")
    })
    List<TagUsageResponse> listUsage(
            @AuthenticationPrincipal GymPrincipal principal,
            @RequestParam(required = false) TagType type) {
        return tagService.listUsage(principal.gymId(), type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un tag de ejercicio para el gimnasio autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tag creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "Autenticacion requerida"),
            @ApiResponse(responseCode = "409", description = "Ya existe un tag del mismo tipo con ese nombre")
    })
    TagResponse create(
            @AuthenticationPrincipal GymPrincipal principal,
            @Valid @RequestBody CreateTagRequest request) {
        return tagService.create(principal.gymId(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renombra un tag de ejercicio del gimnasio autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag renombrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "Autenticacion requerida"),
            @ApiResponse(responseCode = "404", description = "Tag no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya existe otro tag del mismo tipo con ese nombre")
    })
    TagResponse update(
            @AuthenticationPrincipal GymPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTagRequest request) {
        return tagService.update(principal.gymId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un tag de ejercicio del gimnasio autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tag eliminado; sus asignaciones se eliminan en cascada"),
            @ApiResponse(responseCode = "401", description = "Autenticacion requerida"),
            @ApiResponse(responseCode = "404", description = "Tag no encontrado")
    })
    void delete(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        tagService.delete(principal.gymId(), id);
    }
}
