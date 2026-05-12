package com.gymplanner.routine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.Exercise;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class RoutineValidatorTest {

    private final RoutineValidator validator = new RoutineValidator();

    @Test
    void shouldFailIfNoWarmupBlock() {
        Routine routine = routine(RoutineStatus.ACTIVE, BlockPurpose.MAIN_LIFT, BlockPurpose.COOLDOWN);

        assertThatThrownBy(() -> validator.validate(routine))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El día 'Día 1' debe tener al menos un bloque de calentamiento");
    }

    @Test
    void shouldFailIfNoMainBlock() {
        Routine routine = routine(RoutineStatus.ACTIVE, BlockPurpose.ACTIVATION, BlockPurpose.COOLDOWN);

        assertThatThrownBy(() -> validator.validate(routine))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El día 'Día 1' debe tener al menos un bloque de parte principal");
    }

    @Test
    void shouldFailIfNoCooldownBlock() {
        Routine routine = routine(RoutineStatus.ACTIVE, BlockPurpose.WARMUP, BlockPurpose.MAIN_LIFT);

        assertThatThrownBy(() -> validator.validate(routine))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El día 'Día 1' debe tener al menos un bloque de vuelta a la calma");
    }

    @Test
    void shouldFailIfBlockHasNullPurpose() {
        Routine routine = routine(RoutineStatus.ACTIVE, BlockPurpose.WARMUP, null, BlockPurpose.COOLDOWN);

        assertThatThrownBy(() -> validator.validate(routine))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Bloque sin propósito en día 'Día 1'");
    }

    @Test
    void shouldPassWithAllThreeSections() {
        Routine routine = routine(RoutineStatus.ACTIVE, BlockPurpose.ACTIVATION, BlockPurpose.MAIN_LIFT, BlockPurpose.COOLDOWN);

        assertThatCode(() -> validator.validate(routine)).doesNotThrowAnyException();
    }

    @Test
    void shouldSkipValidationForDraftRoutine() {
        Routine routine = new Routine();
        routine.setStatus(RoutineStatus.DRAFT);

        assertThatCode(() -> validator.validate(routine)).doesNotThrowAnyException();
    }

    private Routine routine(RoutineStatus status, BlockPurpose... purposes) {
        Routine routine = new Routine();
        routine.setStatus(status);
        RoutineDay day = new RoutineDay();
        day.setRoutine(routine);
        day.setOrderIndex(1);
        day.setName("Día 1");
        int index = 1;
        for (BlockPurpose purpose : purposes) {
            RoutineBlock block = new RoutineBlock();
            block.setDay(day);
            block.setOrderIndex(index);
            block.setTitle("Bloque " + index);
            block.setStructuralType(BlockStructuralType.STANDARD);
            block.setPurpose(purpose);
            RoutineExercise exercise = new RoutineExercise();
            exercise.setBlock(block);
            exercise.setOrderIndex(1);
            exercise.setExercise(exercise("Ejercicio " + index));
            RoutineExerciseSet set = new RoutineExerciseSet();
            set.setRoutineExercise(exercise);
            set.setSetNumber(1);
            set.setSetKind(SetKind.NORMAL);
            set.setTargetReps(10);
            exercise.getSets().add(set);
            block.getExercises().add(exercise);
            day.getBlocks().add(block);
            index++;
        }
        routine.getDays().add(day);
        return routine;
    }

    private Exercise exercise(String name) {
        Exercise exercise = new Exercise();
        exercise.setName(name);
        return exercise;
    }
}
