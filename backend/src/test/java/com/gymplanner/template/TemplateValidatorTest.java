package com.gymplanner.template;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymplanner.exercise.Exercise;
import com.gymplanner.shared.blocks.BlockPurpose;
import com.gymplanner.shared.blocks.BlockStructuralType;
import com.gymplanner.shared.blocks.SetKind;
import com.gymplanner.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class TemplateValidatorTest {

    private final TemplateValidator validator = new TemplateValidator();

    @Test
    void shouldFailIfNoWarmupBlock() {
        TrainingTemplate template = template(BlockPurpose.MAIN_LIFT, BlockPurpose.COOLDOWN);

        assertThatThrownBy(() -> validator.validate(template))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El día 'Día 1' debe tener al menos un bloque de calentamiento");
    }

    @Test
    void shouldFailIfNoMainBlock() {
        TrainingTemplate template = template(BlockPurpose.ACTIVATION, BlockPurpose.COOLDOWN);

        assertThatThrownBy(() -> validator.validate(template))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El día 'Día 1' debe tener al menos un bloque de parte principal");
    }

    @Test
    void shouldFailIfNoCooldownBlock() {
        TrainingTemplate template = template(BlockPurpose.WARMUP, BlockPurpose.MAIN_LIFT);

        assertThatThrownBy(() -> validator.validate(template))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El día 'Día 1' debe tener al menos un bloque de vuelta a la calma");
    }

    @Test
    void shouldFailIfBlockHasNullPurpose() {
        TrainingTemplate template = template(BlockPurpose.WARMUP, null, BlockPurpose.COOLDOWN);

        assertThatThrownBy(() -> validator.validate(template))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Bloque sin propósito en día 'Día 1'");
    }

    @Test
    void shouldPassWithAllThreeSections() {
        TrainingTemplate template = template(BlockPurpose.ACTIVATION, BlockPurpose.MAIN_LIFT, BlockPurpose.COOLDOWN);

        assertThatCode(() -> validator.validate(template)).doesNotThrowAnyException();
    }

    private TrainingTemplate template(BlockPurpose... purposes) {
        TrainingTemplate template = new TrainingTemplate();
        TemplateDay day = new TemplateDay();
        day.setTemplate(template);
        day.setOrderIndex(1);
        day.setName("Día 1");
        int index = 1;
        for (BlockPurpose purpose : purposes) {
            TemplateBlock block = new TemplateBlock();
            block.setDay(day);
            block.setOrderIndex(index);
            block.setTitle("Bloque " + index);
            block.setStructuralType(BlockStructuralType.STANDARD);
            block.setPurpose(purpose);
            TemplateExercise exercise = new TemplateExercise();
            exercise.setBlock(block);
            exercise.setOrderIndex(1);
            exercise.setExercise(exercise("Ejercicio " + index));
            TemplateExerciseSet set = new TemplateExerciseSet();
            set.setTemplateExercise(exercise);
            set.setSetNumber(1);
            set.setSetKind(SetKind.NORMAL);
            set.setTargetReps(10);
            exercise.getSets().add(set);
            block.getExercises().add(exercise);
            day.getBlocks().add(block);
            index++;
        }
        template.getDays().add(day);
        return template;
    }

    private Exercise exercise(String name) {
        Exercise exercise = new Exercise();
        exercise.setName(name);
        return exercise;
    }
}
