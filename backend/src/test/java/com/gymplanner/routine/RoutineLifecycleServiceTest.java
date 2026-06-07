package com.gymplanner.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.routine.dto.CreateRoutineFromScratchRequest;
import com.gymplanner.routine.dto.CreateNextRoutineRequest;
import com.gymplanner.routine.dto.FinishAndCreateNextRequest;
import com.gymplanner.routine.dto.FinishRoutineRequest;
import com.gymplanner.routine.dto.RoutineBlockInput;
import com.gymplanner.routine.dto.RoutineDayInput;
import com.gymplanner.routine.dto.RoutineExerciseInput;
import com.gymplanner.routine.dto.RoutineExerciseSetInput;
import com.gymplanner.routine.dto.WeightAdjustmentInput;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.BusinessRuleException;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import jakarta.persistence.EntityManager;
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
class RoutineLifecycleServiceTest {
    @Autowired RoutineLifecycleService lifecycleService;
    @Autowired RoutineService routineService;
    @Autowired RoutineRepository routineRepository;
    @Autowired RoutineWeightAdjustService weightAdjustService;
    @Autowired StudentService studentService;
    @Autowired ExerciseService exerciseService;
    @Autowired EntityManager entityManager;

    @Test
    void finishRoutine_setsStatusAndTimestamp() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        routineService.finishRoutine(1L, 1L, fixture.routineId, new FinishRoutineRequest("Cierre prolijo"));

        Routine routine = routineRepository.findById(fixture.routineId).orElseThrow();
        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(routine.getFinishedAt()).isNotNull();
        assertThat(routine.getFinishedByUser().getId()).isEqualTo(1L);
        assertThat(routine.getClosureNotes()).isEqualTo("Cierre prolijo");
    }

    @Test
    void finishRoutine_failsIfAlreadyFinished() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));
        routineService.finishRoutine(1L, 1L, fixture.routineId, new FinishRoutineRequest(null));

        assertThatThrownBy(() -> routineService.finishRoutine(1L, 1L, fixture.routineId, new FinishRoutineRequest(null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Esta rutina ya está finalizada o archivada.");
    }

    @Test
    void activateRoutine_finishesPreviousActive() {
        Long studentId = student("Ana", "Activate");
        Long exerciseId = exercise();
        Long activeId = createRoutine(studentId, "Activa", RoutineStatus.ACTIVE, exerciseId, new BigDecimal("80.00"), "Notas", "Internas");
        Long draftId = createRoutine(studentId, "Borrador", RoutineStatus.DRAFT, exerciseId, new BigDecimal("85.00"), null, null);

        routineService.activateRoutine(1L, 1L, draftId);

        assertThat(routineRepository.findById(activeId).orElseThrow().getStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(routineRepository.findById(activeId).orElseThrow().getFinishedAt()).isNotNull();
        assertThat(routineRepository.findById(draftId).orElseThrow().getStatus()).isEqualTo(RoutineStatus.ACTIVE);
    }

    @Test
    void activateRoutine_failsIfNotDraft() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));
        routineService.finishRoutine(1L, 1L, fixture.routineId, new FinishRoutineRequest(null));

        assertThatThrownBy(() -> routineService.activateRoutine(1L, 1L, fixture.routineId))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deleteRoutine_hardDeleteIfDraft() {
        Fixture fixture = fixture(RoutineStatus.DRAFT, new BigDecimal("80.00"));

        routineService.delete(1L, fixture.routineId);
        entityManager.flush();

        assertThat(routineRepository.findById(fixture.routineId)).isEmpty();
    }

    @Test
    void deleteRoutine_failsIfNotDraft() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        assertThatThrownBy(() -> routineService.delete(1L, fixture.routineId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Solo se pueden eliminar rutinas en borrador. Para finalizar o archivar, usá los endpoints correspondientes.");
    }

    @Test
    void finishAndCreateNext_createsDeepCopyWithPreviousRoutineId() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        var response = lifecycleService.finishAndCreateNext(1L, 1L, request(fixture.routineId, "DRAFT", null, true, false, null));
        entityManager.flush();
        entityManager.clear();

        Routine original = routineRepository.findByIdWithFullStructure(fixture.routineId).orElseThrow();
        Routine next = routineRepository.findByIdWithFullStructure(response.newRoutine().id()).orElseThrow();
        assertThat(original.getStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(next.getStatus()).isEqualTo(RoutineStatus.DRAFT);
        assertThat(next.getPreviousRoutine().getId()).isEqualTo(original.getId());
        assertThat(next.getSourceTemplate()).isNull();
        assertThat(next.getDays()).hasSize(original.getDays().size());
        assertThat(blockIds(next)).hasSize(blockIds(original).size());
        assertThat(exerciseIds(next)).hasSize(exerciseIds(original).size());
        assertThat(setIds(next)).hasSize(setIds(original).size());
        assertThat(setIds(next)).doesNotContainAnyElementsOf(setIds(original));

        firstSet(next).setTargetWeightKg(new BigDecimal("99.00"));
        entityManager.flush();
        entityManager.clear();

        assertThat(firstSet(routineRepository.findByIdWithFullStructure(original.getId()).orElseThrow()).getTargetWeightKg())
                .isEqualByComparingTo("80.00");
    }

    @Test
    void finishAndCreateNext_appliesWeightAdjustmentToNewRoutine() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        var response = lifecycleService.finishAndCreateNext(1L, 1L, request(fixture.routineId, "DRAFT", null, true, false, new WeightAdjustmentInput(5.0, 2.5)));
        Routine original = routineRepository.findByIdWithFullStructure(fixture.routineId).orElseThrow();
        Routine next = routineRepository.findByIdWithFullStructure(response.newRoutine().id()).orElseThrow();

        assertThat(response.weightSetsAdjusted()).isEqualTo(6);
        assertThat(firstSet(original).getTargetWeightKg()).isEqualByComparingTo("80.00");
        assertThat(firstSet(next).getTargetWeightKg()).isEqualByComparingTo("85.00");
    }

    @Test
    void finishAndCreateNext_copiesExecutionCue() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        var response = lifecycleService.finishAndCreateNext(1L, 1L, request(fixture.routineId, "DRAFT", null, true, false, null));
        Routine original = routineRepository.findByIdWithFullStructure(fixture.routineId).orElseThrow();
        Routine next = routineRepository.findByIdWithFullStructure(response.newRoutine().id()).orElseThrow();

        assertThat(firstSet(original).getExecutionCue()).isEqualTo("Recorrido completo");
        assertThat(firstSet(next).getExecutionCue()).isEqualTo("Recorrido completo");
    }

    @Test
    void finishAndCreateNext_copiesGroupedSetFields() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        var response = lifecycleService.finishAndCreateNext(1L, 1L, request(fixture.routineId, "DRAFT", null, true, false, null));
        Routine next = routineRepository.findByIdWithFullStructure(response.newRoutine().id()).orElseThrow();
        RoutineBlock grouped = next.getDays().stream()
                .flatMap(day -> day.getBlocks().stream())
                .filter(block -> block.getStructuralType() == BlockStructuralType.GROUPED_SET)
                .findFirst()
                .orElseThrow();

        assertThat(grouped.getTargetRounds()).isEqualTo(3);
        assertThat(grouped.getRoundRestSeconds()).isEqualTo(90);
        assertThat(next.getDays().stream().flatMap(day -> day.getBlocks().stream()).filter(block -> block.getStructuralType() != BlockStructuralType.GROUPED_SET).toList())
                .extracting("roundRestSeconds")
                .containsOnlyNulls();
    }

    @Test
    void weightAdjustment_roundsNearestWithHalfUp() {
        assertAdjustedWeight("30.00", 5.0, 2.5, "32.50", 1);
        assertAdjustedWeight("40.00", 5.0, 2.5, "42.50", 1);
        assertAdjustedWeight("60.00", 5.0, 2.5, "62.50", 1);
        assertAdjustedWeight("80.00", 5.0, 2.5, "85.00", 1);
        assertAdjustedWeight("80.00", 5.0, 1.0, "84.00", 1);
        assertAdjustedWeight("80.00", 5.0, null, "84.00", 1);
        assertAdjustedWeight("100.00", -10.0, 2.5, "90.00", 1);
        assertAdjustedWeight(null, 5.0, 2.5, null, 0);
    }

    @Test
    void weightAdjustmentLeavesExecutionCueIntact() {
        Routine routine = routineWithWeight(new BigDecimal("80.00"));
        firstSet(routine).setExecutionCue("Parcial corto");

        int adjusted = weightAdjustService.applyAdjustment(routine, RoutineWeightAdjustmentScopeType.ROUTINE, 5.0, 2.5);

        assertThat(adjusted).isEqualTo(1);
        assertThat(firstSet(routine).getTargetWeightKg()).isEqualByComparingTo("85.00");
        assertThat(firstSet(routine).getExecutionCue()).isEqualTo("Parcial corto");
    }

    @Test
    void finishAndCreateNext_withNewStatusActive_doesNotLeaveDoubleActive() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        var response = lifecycleService.finishAndCreateNext(1L, 1L, request(fixture.routineId, "ACTIVE", null, true, false, null));

        assertThat(routineRepository.findById(fixture.routineId).orElseThrow().getStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(routineRepository.findById(response.newRoutine().id()).orElseThrow().getStatus()).isEqualTo(RoutineStatus.ACTIVE);
        assertThat(routineRepository.findByStudentAndStatuses(fixture.studentId, 1L, List.of(RoutineStatus.ACTIVE))).hasSize(1);
    }

    @Test
    void finishAndCreateNext_copiesNotesAccordingToFlags() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        var response = lifecycleService.finishAndCreateNext(1L, 1L, request(fixture.routineId, "DRAFT", null, true, false, null));
        Routine next = routineRepository.findById(response.newRoutine().id()).orElseThrow();

        assertThat(next.getGeneralNotes()).isEqualTo("Notas generales");
        assertThat(next.getInternalNotes()).isNull();
    }

    @Test
    void createNext_failsOnActiveRoutine() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));

        assertThatThrownBy(() -> lifecycleService.createNextFromExisting(1L, 1L, fixture.routineId, createNextRequest("DRAFT", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("rutinas activas");
    }

    @Test
    void createNext_failsOnDraftRoutine() {
        Fixture fixture = fixture(RoutineStatus.DRAFT, new BigDecimal("80.00"));

        assertThatThrownBy(() -> lifecycleService.createNextFromExisting(1L, 1L, fixture.routineId, createNextRequest("DRAFT", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("borrador");
    }

    @Test
    void createNext_fromFinished_createsLinkedCopy() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));
        routineService.finishRoutine(1L, 1L, fixture.routineId, new FinishRoutineRequest("Cierre"));

        var response = lifecycleService.createNextFromExisting(1L, 1L, fixture.routineId, createNextRequest("DRAFT", null));
        Routine next = routineRepository.findByIdWithFullStructure(response.newRoutine().id()).orElseThrow();

        assertThat(response.sourceRoutine().id()).isEqualTo(fixture.routineId);
        assertThat(next.getStatus()).isEqualTo(RoutineStatus.DRAFT);
        assertThat(next.getPreviousRoutine().getId()).isEqualTo(fixture.routineId);
        assertThat(next.getSourceTemplate()).isNull();
        assertThat(next.getDays()).hasSize(2);
    }

    @Test
    void createNext_fromArchived_createsLinkedCopy() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));
        routineService.archiveRoutine(1L, 1L, fixture.routineId);

        var response = lifecycleService.createNextFromExisting(1L, 1L, fixture.routineId, createNextRequest("DRAFT", null));
        Routine next = routineRepository.findByIdWithFullStructure(response.newRoutine().id()).orElseThrow();

        assertThat(next.getPreviousRoutine().getId()).isEqualTo(fixture.routineId);
        assertThat(next.getStatus()).isEqualTo(RoutineStatus.DRAFT);
    }

    @Test
    void createNext_withNewStatusActive_finishesPreviousActiveOfStudent() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));
        routineService.finishRoutine(1L, 1L, fixture.routineId, new FinishRoutineRequest(null));
        Long otherActiveId = createRoutine(fixture.studentId, "Otra activa", RoutineStatus.ACTIVE, fixture.exerciseId, new BigDecimal("70.00"), null, null);

        var response = lifecycleService.createNextFromExisting(1L, 1L, fixture.routineId, createNextRequest("ACTIVE", null));

        assertThat(routineRepository.findById(otherActiveId).orElseThrow().getStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(routineRepository.findById(response.newRoutine().id()).orElseThrow().getStatus()).isEqualTo(RoutineStatus.ACTIVE);
        assertThat(routineRepository.findByStudentAndStatuses(fixture.studentId, 1L, List.of(RoutineStatus.ACTIVE))).hasSize(1);
    }

    @Test
    void createNext_appliesWeightAdjustmentCorrectly() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("40.00"));
        routineService.finishRoutine(1L, 1L, fixture.routineId, new FinishRoutineRequest(null));

        var response = lifecycleService.createNextFromExisting(1L, 1L, fixture.routineId, createNextRequest("DRAFT", new WeightAdjustmentInput(5.0, 2.5)));
        Routine next = routineRepository.findByIdWithFullStructure(response.newRoutine().id()).orElseThrow();

        assertThat(response.weightSetsAdjusted()).isEqualTo(6);
        assertThat(firstSet(next).getTargetWeightKg()).isEqualByComparingTo("42.50");
    }

    @Test
    void createNext_doesNotModifySourceRoutine() {
        Fixture fixture = fixture(RoutineStatus.ACTIVE, new BigDecimal("80.00"));
        routineService.finishRoutine(1L, 1L, fixture.routineId, new FinishRoutineRequest("Cierre original"));

        lifecycleService.createNextFromExisting(1L, 1L, fixture.routineId, createNextRequest("DRAFT", new WeightAdjustmentInput(5.0, 2.5)));
        Routine source = routineRepository.findByIdWithFullStructure(fixture.routineId).orElseThrow();

        assertThat(source.getStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(source.getClosureNotes()).isEqualTo("Cierre original");
        assertThat(firstSet(source).getTargetWeightKg()).isEqualByComparingTo("80.00");
    }

    private Fixture fixture(RoutineStatus status, BigDecimal firstWeight) {
        Long studentId = student("Ana", "Lifecycle");
        Long exerciseId = exercise();
        Long routineId = createRoutine(studentId, "Rutina base", status, exerciseId, firstWeight, "Notas generales", "Notas internas");
        return new Fixture(studentId, exerciseId, routineId);
    }

    private Long student(String firstName, String lastName) {
        return studentService.create(1L, new CreateStudentRequest(firstName, lastName, null, null, null, null, "Futbol", "Fuerza", "Intermedio", null, LocalDate.now())).id();
    }

    private Long exercise() {
        return exerciseService.create(1L, new CreateExerciseRequest("Sentadilla lifecycle", "Desc", null, MeasurementType.REPS_WEIGHT, null, null, List.of())).id();
    }

    private Long createRoutine(Long studentId, String name, RoutineStatus status, Long exerciseId, BigDecimal firstWeight, String generalNotes, String internalNotes) {
        return routineService.createFromScratch(1L, 1L, new CreateRoutineFromScratchRequest(
                studentId,
                name,
                "Fuerza",
                status,
                LocalDate.now(),
                generalNotes,
                internalNotes,
                List.of(
                        day("Dia 1", exerciseId, firstWeight),
                        day("Dia 2", exerciseId, firstWeight.add(new BigDecimal("5.00")))
                ))).id();
    }

    private FinishAndCreateNextRequest request(Long routineId, String status, String name, boolean copyGeneralNotes, boolean copyInternalNotes, WeightAdjustmentInput adjustment) {
        return new FinishAndCreateNextRequest(routineId, "Cierre", name, LocalDate.now().plusWeeks(4), status, copyGeneralNotes, copyInternalNotes, adjustment);
    }

    private CreateNextRoutineRequest createNextRequest(String status, WeightAdjustmentInput adjustment) {
        return new CreateNextRoutineRequest(null, LocalDate.now().plusWeeks(4), status, true, false, adjustment);
    }

    private RoutineDayInput day(String name, Long exerciseId, BigDecimal firstWeight) {
        return new RoutineDayInput(null, null, name, null, List.of(
                block("Entrada", BlockPurpose.ACTIVATION, exerciseId, firstWeight),
                groupedBlock("Fuerza", exerciseId, firstWeight),
                block("Salida", BlockPurpose.COOLDOWN, exerciseId, firstWeight)
        ));
    }

    private RoutineBlockInput block(String title, BlockPurpose purpose, Long exerciseId, BigDecimal weight) {
        return new RoutineBlockInput(null, title, BlockStructuralType.STANDARD, purpose, null, null, null, List.of(
                new RoutineExerciseInput(exerciseId, null, "Notas ejercicio", List.of(
                        new RoutineExerciseSetInput(null, SetKind.NORMAL, 8, null, null, weight, null, null, 90, "3010", "Recorrido completo", 8, "Set pesado", false)
                ))
        ));
    }

    private RoutineBlockInput groupedBlock(String title, Long exerciseId, BigDecimal weight) {
        return new RoutineBlockInput(null, title, BlockStructuralType.GROUPED_SET, BlockPurpose.MAIN_LIFT, null, 3, 90, null, List.of(
                new RoutineExerciseInput(exerciseId, null, "Notas ejercicio", List.of(
                        new RoutineExerciseSetInput(null, SetKind.NORMAL, 8, null, null, weight, null, null, null, "3010", "Recorrido completo", 8, "Set pesado", false)
                ))
        ));
    }

    private RoutineExerciseSet firstSet(Routine routine) {
        return routine.getDays().stream().findFirst().orElseThrow()
                .getBlocks().stream().findFirst().orElseThrow()
                .getExercises().stream().findFirst().orElseThrow()
                .getSets().stream().findFirst().orElseThrow();
    }

    private void assertAdjustedWeight(String initialWeight, double percentage, Double roundingStep, String expectedWeight, int expectedCount) {
        Routine routine = routineWithWeight(initialWeight == null ? null : new BigDecimal(initialWeight));
        int adjusted = weightAdjustService.applyAdjustment(routine, RoutineWeightAdjustmentScopeType.ROUTINE, percentage, roundingStep);
        assertThat(adjusted).isEqualTo(expectedCount);
        BigDecimal weight = firstSet(routine).getTargetWeightKg();
        if (expectedWeight == null) {
            assertThat(weight).isNull();
        } else {
            assertThat(weight).isEqualByComparingTo(expectedWeight);
        }
    }

    private Routine routineWithWeight(BigDecimal weight) {
        Routine routine = new Routine();
        RoutineDay day = new RoutineDay();
        day.setRoutine(routine);
        RoutineBlock block = new RoutineBlock();
        block.setDay(day);
        RoutineExercise exercise = new RoutineExercise();
        exercise.setBlock(block);
        RoutineExerciseSet set = new RoutineExerciseSet();
        set.setRoutineExercise(exercise);
        set.setTargetWeightKg(weight);
        exercise.getSets().add(set);
        block.getExercises().add(exercise);
        day.getBlocks().add(block);
        routine.getDays().add(day);
        return routine;
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

    private record Fixture(Long studentId, Long exerciseId, Long routineId) {}
}
