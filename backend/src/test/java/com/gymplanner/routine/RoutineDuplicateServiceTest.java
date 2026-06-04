package com.gymplanner.routine;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.DuplicateRoutineRequest;
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoutineDuplicateServiceTest {
    @Autowired RoutineService routineService;
    @Autowired RoutineRepository routineRepository;
    @Autowired StudentService studentService;
    @Autowired ExerciseService exerciseService;

    @Test
    void duplicateRoutineToAnotherStudentCopiesAllDaysBlocksExercisesSets() {
        Fixture fixture = fixture();

        var copy = routineService.duplicate(1L, 1L, fixture.originalRoutineId, new DuplicateRoutineRequest(fixture.targetStudentId, null, LocalDate.now(), RoutineStatus.DRAFT));

        assertThat(copy.studentId()).isEqualTo(fixture.targetStudentId);
        assertThat(copy.name()).isEqualTo("Rutina ajustada (copia)");
        assertThat(copy.status()).isEqualTo(RoutineStatus.DRAFT);
        assertThat(copy.days()).hasSize(2);
        assertThat(copy.days()).flatExtracting("blocks").hasSize(6);
        assertThat(copy.days()).flatExtracting("blocks").flatExtracting("exercises").hasSize(6);
        assertThat(copy.days()).flatExtracting("blocks").flatExtracting("exercises").flatExtracting("sets").hasSize(12);
    }

    @Test
    void duplicateRoutineKeepsWeights() {
        Fixture fixture = fixture();

        var copy = routineService.duplicate(1L, 1L, fixture.originalRoutineId, new DuplicateRoutineRequest(fixture.targetStudentId, null, LocalDate.now(), RoutineStatus.DRAFT));

        assertThat(copy.days().get(0).blocks().get(1).exercises().get(0).sets().get(0).targetWeightKg()).isEqualByComparingTo("72.50");
        assertThat(copy.days().get(0).blocks().get(1).exercises().get(0).sets().get(1).targetWeightKg()).isEqualByComparingTo("67.00");
    }

    @Test
    void routineSetStoresAndReturnsExecutionCue() {
        Fixture fixture = fixture();

        var routine = routineService.get(1L, fixture.originalRoutineId);

        assertThat(routine.days().get(0).blocks().get(0).exercises().get(0).sets().get(0).executionCue())
                .isEqualTo("Pausa abajo");
    }

    @Test
    void duplicateRoutineCopiesExecutionCue() {
        Fixture fixture = fixture();

        var copy = routineService.duplicate(1L, 1L, fixture.originalRoutineId, new DuplicateRoutineRequest(fixture.targetStudentId, null, LocalDate.now(), RoutineStatus.DRAFT));

        assertThat(copy.days().get(0).blocks().get(0).exercises().get(0).sets().get(0).executionCue())
                .isEqualTo("Pausa abajo");
    }

    @Test
    void duplicateRoutineDefaultDraftDoesNotFinishTargetActive() {
        Fixture fixture = fixture();
        Long targetActiveId = routineService.createFromScratch(1L, 1L, routineRequest(fixture.targetStudentId, "Activa destino", RoutineStatus.ACTIVE, fixture.exerciseId, new BigDecimal("35.00"))).id();

        routineService.duplicate(1L, 1L, fixture.originalRoutineId, new DuplicateRoutineRequest(fixture.targetStudentId, null, LocalDate.now(), null));

        assertThat(routineRepository.findById(targetActiveId).orElseThrow().getStatus()).isEqualTo(RoutineStatus.ACTIVE);
    }

    @Test
    void duplicateRoutineActiveFinishesTargetActive() {
        Fixture fixture = fixture();
        Long targetActiveId = routineService.createFromScratch(1L, 1L, routineRequest(fixture.targetStudentId, "Activa destino", RoutineStatus.ACTIVE, fixture.exerciseId, new BigDecimal("35.00"))).id();

        var copy = routineService.duplicate(1L, 1L, fixture.originalRoutineId, new DuplicateRoutineRequest(fixture.targetStudentId, "Nueva activa", LocalDate.now(), RoutineStatus.ACTIVE));

        assertThat(routineRepository.findById(targetActiveId).orElseThrow().getStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(copy.status()).isEqualTo(RoutineStatus.ACTIVE);
    }

    @Test
    void duplicateRoutineDoesNotShareIdsWithOriginal() {
        Fixture fixture = fixture();

        var copy = routineService.duplicate(1L, 1L, fixture.originalRoutineId, new DuplicateRoutineRequest(fixture.targetStudentId, null, LocalDate.now(), RoutineStatus.DRAFT));
        Routine originalEntity = routineRepository.findByIdWithFullStructure(fixture.originalRoutineId).orElseThrow();
        Routine copyEntity = routineRepository.findByIdWithFullStructure(copy.id()).orElseThrow();

        assertThat(ids(originalEntity.getDays())).doesNotContainAnyElementsOf(ids(copyEntity.getDays()));
        assertThat(blockIds(originalEntity)).doesNotContainAnyElementsOf(blockIds(copyEntity));
        assertThat(exerciseIds(originalEntity)).doesNotContainAnyElementsOf(exerciseIds(copyEntity));
        assertThat(setIds(originalEntity)).doesNotContainAnyElementsOf(setIds(copyEntity));
    }

    private Fixture fixture() {
        Long sourceStudentId = student("Ana", "Origen");
        Long targetStudentId = student("Bruno", "Destino");
        Long exerciseId = exerciseService.create(1L, new CreateExerciseRequest("Sentadilla duplicable", "Desc", null, MeasurementType.REPS_WEIGHT, null, null, List.of())).id();
        Long originalRoutineId = routineService.createFromScratch(1L, 1L, routineRequest(sourceStudentId, "Rutina ajustada", RoutineStatus.DRAFT, exerciseId, new BigDecimal("72.50"))).id();
        return new Fixture(sourceStudentId, targetStudentId, exerciseId, originalRoutineId);
    }

    private Long student(String firstName, String lastName) {
        return studentService.create(1L, new CreateStudentRequest(firstName, lastName, null, null, null, null, "Futbol", "Fuerza", "Intermedio", null, LocalDate.now())).id();
    }

    private CreateRoutineFromScratchRequest routineRequest(Long studentId, String name, RoutineStatus status, Long exerciseId, BigDecimal firstWeight) {
        return new CreateRoutineFromScratchRequest(studentId, name, "Fuerza", status, LocalDate.now(), "Notas alumno", "Notas internas", List.of(
                day("Dia 1", exerciseId, firstWeight),
                day("Dia 2", exerciseId, firstWeight.add(new BigDecimal("5.00")))
        ));
    }

    private RoutineDayInput day(String name, Long exerciseId, BigDecimal firstWeight) {
        return new RoutineDayInput(null, null, name, null, List.of(
                block("Calentamiento", BlockPurpose.ACTIVATION, exerciseId, firstWeight),
                block("Fuerza", BlockPurpose.MAIN_LIFT, exerciseId, firstWeight),
                block("Vuelta", BlockPurpose.COOLDOWN, exerciseId, firstWeight)
        ));
    }

    private RoutineBlockInput block(String title, BlockPurpose purpose, Long exerciseId, BigDecimal firstWeight) {
        return new RoutineBlockInput(null, title, BlockStructuralType.STANDARD, purpose, null, null, null, List.of(
                new RoutineExerciseInput(exerciseId, null, "Notas ejercicio", List.of(
                        new RoutineExerciseSetInput(null, SetKind.NORMAL, 8, null, null, firstWeight, null, null, 90, "3010", "Pausa abajo", 8, "Set pesado", false),
                        new RoutineExerciseSetInput(null, SetKind.NORMAL, 10, null, null, new BigDecimal("67.00"), null, null, 60, null, null, "Set liviano", false)
                ))
        ));
    }

    private Set<Long> ids(Set<RoutineDay> days) {
        return days.stream().map(RoutineDay::getId).collect(Collectors.toSet());
    }

    private Set<Long> blockIds(Routine routine) {
        return routine.getDays().stream().flatMap(day -> day.getBlocks().stream()).map(RoutineBlock::getId).collect(Collectors.toSet());
    }

    private Set<Long> exerciseIds(Routine routine) {
        return routine.getDays().stream().flatMap(day -> day.getBlocks().stream()).flatMap(block -> block.getExercises().stream()).map(RoutineExercise::getId).collect(Collectors.toSet());
    }

    private Set<Long> setIds(Routine routine) {
        return routine.getDays().stream().flatMap(day -> day.getBlocks().stream()).flatMap(block -> block.getExercises().stream()).flatMap(exercise -> exercise.getSets().stream()).map(RoutineExerciseSet::getId).collect(Collectors.toSet());
    }

    private record Fixture(Long sourceStudentId, Long targetStudentId, Long exerciseId, Long originalRoutineId) {}
}
