package com.gymplanner.routine;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymplanner.exercise.ExerciseRepository;
import com.gymplanner.exercise.ExerciseService;
import com.gymplanner.exercise.MeasurementType;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.routine.dto.CreateRoutineFromTemplateRequest;
import com.gymplanner.routine.dto.RoutineBlockResponse;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.dto.CreateStudentRequest;
import com.gymplanner.template.TrainingTemplateRepository;
import com.gymplanner.template.TemplateService;
import com.gymplanner.template.dto.CreateTemplateRequest;
import com.gymplanner.template.dto.TemplateBlockInput;
import com.gymplanner.template.dto.TemplateDayInput;
import com.gymplanner.template.dto.TemplateExerciseInput;
import com.gymplanner.template.dto.TemplateExerciseSetInput;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoutineFromTemplateServiceTest {
    @Autowired RoutineFromTemplateService fromTemplateService;
    @Autowired RoutineRepository routineRepository;
    @Autowired TrainingTemplateRepository templateRepository;
    @Autowired TemplateService templateService;
    @Autowired StudentService studentService;
    @Autowired ExerciseService exerciseService;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired EntityManager entityManager;

    @Test
    void shouldCreateRoutineWithDeepCopyOfTemplate() {
        Fixture fixture = fixture();
        var routine = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, null, LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        assertThat(routine.status()).isEqualTo(RoutineStatus.ACTIVE);
        assertThat(routine.sourceTemplateId()).isEqualTo(fixture.templateId);
        assertThat(routine.days()).hasSize(2);
        assertThat(routine.days()).extracting("name").containsExactly("Día 1", "Día 2");
        assertThat(blocks(routine)).hasSize(8);
        assertThat(blocks(routine)).extracting("title").containsExactly("Calor", "Fuerza", "Circuito", "Piramide", "Vuelta", "Entrada", "Traccion", "Salida");
        assertThat(blocks(routine).get(0).exercises().get(0).exerciseId()).isEqualTo(fixture.exerciseId);
        assertThat(blocks(routine).get(0).exercises().get(0).sets()).hasSize(2);
    }

    @Test
    void editingTemplateAfterCreatingRoutineShouldNotAffectRoutine() {
        Fixture fixture = fixture();
        var routine = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, null, LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        templateService.update(1L, fixture.templateId, new com.gymplanner.template.dto.UpdateTemplateRequest("Editada", null, null, null, null, null, null, true, List.of(day("Día editado",
                block("Calor", BlockStructuralType.STANDARD, BlockPurpose.ACTIVATION, null, fixture.exerciseId, SetKind.NORMAL),
                block("Nuevo", BlockStructuralType.STANDARD, BlockPurpose.ACCESSORY, null, fixture.exerciseId, SetKind.NORMAL),
                block("Final", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, fixture.exerciseId, SetKind.NORMAL)))));
        var reloaded = routineRepository.findByIdWithFullStructure(routine.id()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Plantilla base");
        assertThat(entityBlocks(reloaded)).hasSize(8);
        assertThat(entityBlocks(reloaded)).extracting("title").contains("Fuerza", "Circuito", "Piramide");
    }

    @Test
    void deletingTemplateShouldNotDeleteRoutine() {
        Fixture fixture = fixture();
        var routine = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, null, LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        entityManager.flush();
        entityManager.clear();
        templateRepository.delete(templateRepository.findById(fixture.templateId).orElseThrow());
        entityManager.flush();
        entityManager.clear();
        var reloaded = routineRepository.findByIdWithFullStructure(routine.id()).orElseThrow();
        assertThat(reloaded.getSourceTemplate()).isNull();
        assertThat(entityBlocks(reloaded)).hasSize(8);
    }

    @Test
    void creatingNewActiveRoutineForStudentShouldFinishPreviousActive() {
        Fixture fixture = fixture();
        var first = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, "Primera", LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        var second = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, "Segunda", LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        assertThat(routineRepository.findById(first.id()).orElseThrow().getStatus()).isEqualTo(RoutineStatus.FINISHED);
        assertThat(routineRepository.findById(second.id()).orElseThrow().getStatus()).isEqualTo(RoutineStatus.ACTIVE);
    }

    @Test
    void creatingDraftRoutineShouldNotFinishPreviousActive() {
        Fixture fixture = fixture();
        var first = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, "Primera", LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        var draft = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, "Draft", LocalDate.now(), null, null, RoutineStatus.DRAFT));
        assertThat(routineRepository.findById(first.id()).orElseThrow().getStatus()).isEqualTo(RoutineStatus.ACTIVE);
        assertThat(routineRepository.findById(draft.id()).orElseThrow().getStatus()).isEqualTo(RoutineStatus.DRAFT);
    }

    @Test
    void blockStructuralTypeAndPurposeAreCopiedCorrectly() {
        Fixture fixture = fixture();
        var routine = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, null, LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        assertThat(blocks(routine)).extracting("structuralType").contains(BlockStructuralType.CIRCUIT, BlockStructuralType.PYRAMID);
        assertThat(blocks(routine)).extracting("purpose").contains(BlockPurpose.CONDITIONING, BlockPurpose.MAIN_LIFT);
        assertThat(blocks(routine).get(2).totalDurationSeconds()).isEqualTo(720);
    }

    @Test
    void setsAreCopiedWithAllParameters() {
        Fixture fixture = fixture();
        var routine = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, null, LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        var set = blocks(routine).get(0).exercises().get(0).sets().get(0);
        assertThat(set.targetReps()).isEqualTo(8);
        assertThat(set.targetWeightKg()).isNull();
        assertThat(set.restAfterSeconds()).isEqualTo(90);
        assertThat(set.rpe()).isEqualTo(8);
        assertThat(set.tempo()).isEqualTo("3010");
        assertThat(set.setKind()).isEqualTo(SetKind.WARMUP);
    }

    @Test
    void inactiveExerciseStillAppearsInRoutineThatReferencesIt() {
        Fixture fixture = fixture();
        var routine = fromTemplateService.createFromTemplate(1L, 1L, new CreateRoutineFromTemplateRequest(fixture.studentId, fixture.templateId, null, LocalDate.now(), null, null, RoutineStatus.ACTIVE));
        exerciseService.deactivate(1L, fixture.exerciseId);
        entityManager.flush();
        entityManager.clear();
        var reloaded = routineRepository.findByIdWithFullStructure(routine.id()).orElseThrow();
        var exercise = entityBlocks(reloaded).get(0).getExercises().iterator().next().getExercise();
        assertThat(exercise.isActive()).isFalse();
        assertThat(exercise.getName()).isEqualTo("Sentadilla test");
    }

    private Fixture fixture() {
        Long studentId = studentService.create(1L, new CreateStudentRequest("Ana", "Rutina", null, "555", null, null, "Futbol", "Fuerza", "Intermedio", null, LocalDate.now())).id();
        Long exerciseId = exerciseService.create(1L, new CreateExerciseRequest("Sentadilla test", "Desc", "Notas", MeasurementType.REPS_WEIGHT, null, null, List.of())).id();
        Long exercise2Id = exerciseService.create(1L, new CreateExerciseRequest("Remo test", "Desc", "Notas", MeasurementType.REPS_WEIGHT, null, null, List.of())).id();
        Long templateId = templateService.create(1L, 1L, new CreateTemplateRequest("Plantilla base", "Desc", "Futbol", "Fuerza", "Intermedio", 60, "Notas", List.of(day("Día 1",
                block("Calor", BlockStructuralType.STANDARD, BlockPurpose.ACTIVATION, null, exerciseId, SetKind.WARMUP),
                block("Fuerza", BlockStructuralType.STANDARD, BlockPurpose.MAIN_LIFT, null, exerciseId, SetKind.WARMUP),
                block("Circuito", BlockStructuralType.CIRCUIT, BlockPurpose.CONDITIONING, 720, exercise2Id, SetKind.NORMAL),
                block("Piramide", BlockStructuralType.PYRAMID, BlockPurpose.MAIN_LIFT, null, exerciseId, SetKind.NORMAL),
                block("Vuelta", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, exercise2Id, SetKind.NORMAL)
        ), day("Día 2",
                block("Entrada", BlockStructuralType.STANDARD, BlockPurpose.ACTIVATION, null, exercise2Id, SetKind.NORMAL),
                block("Traccion", BlockStructuralType.STANDARD, BlockPurpose.ACCESSORY, null, exercise2Id, SetKind.NORMAL),
                block("Salida", BlockStructuralType.STANDARD, BlockPurpose.COOLDOWN, null, exerciseId, SetKind.NORMAL)
        )))).id();
        return new Fixture(studentId, exerciseId, templateId);
    }

    private TemplateBlockInput block(String title, BlockStructuralType type, BlockPurpose purpose, Integer duration, Long exerciseId, SetKind kind) {
        return new TemplateBlockInput(null, title, type, purpose, duration, null, "Notas bloque", List.of(new TemplateExerciseInput(exerciseId, null, "Notas ejercicio", List.of(
                new TemplateExerciseSetInput(null, kind, 8, null, null, new BigDecimal("60.50"), null, null, 90, "3010", 8, "Set 1", false),
                new TemplateExerciseSetInput(null, SetKind.NORMAL, 10, null, null, new BigDecimal("55.00"), null, null, 60, null, null, null, false)
        ))));
    }

    private TemplateDayInput day(String name, TemplateBlockInput... blocks) {
        return new TemplateDayInput(null, null, name, null, List.of(blocks));
    }

    private List<RoutineBlockResponse> blocks(com.gymplanner.routine.dto.RoutineResponse routine) {
        return routine.days().stream().flatMap(day -> day.blocks().stream()).toList();
    }

    private List<RoutineBlock> entityBlocks(Routine routine) {
        return routine.getDays().stream().flatMap(day -> day.getBlocks().stream()).toList();
    }

    private record Fixture(Long studentId, Long exerciseId, Long templateId) {}
}
