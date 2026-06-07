package com.gymplanner.routine;

import com.gymplanner.routine.dto.CreateRoutineFromTemplateRequest;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.student.Student;
import com.gymplanner.student.StudentService;
import com.gymplanner.template.TemplateBlock;
import com.gymplanner.template.TemplateDay;
import com.gymplanner.template.TemplateExercise;
import com.gymplanner.template.TemplateExerciseSet;
import com.gymplanner.template.TemplateService;
import com.gymplanner.template.TrainingTemplate;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoutineFromTemplateService {
    private final TemplateService templateService;
    private final StudentService studentService;
    private final RoutineRepository routineRepository;
    private final RoutineService routineService;
    private final RoutineValidator validator;
    private final RoutineMapper mapper;
    private final UserRepository userRepository;

    @Transactional
    public RoutineResponse createFromTemplate(Long gymId, Long userId, CreateRoutineFromTemplateRequest request) {
        TrainingTemplate template = templateService.getFull(gymId, request.templateId());
        Student student = studentService.getEntity(gymId, request.studentId());
        RoutineStatus status = request.status() == null ? RoutineStatus.ACTIVE : request.status();
        User currentUser = userRepository.getReferenceById(userId);
        if (status == RoutineStatus.ACTIVE) {
            routineService.finishPreviousActive(gymId, student.getId(), null, currentUser);
        }
        Routine routine = new Routine();
        routine.setStudent(student);
        routine.setName(StringUtils.hasText(request.name()) ? request.name().trim() : template.getName());
        routine.setObjective(template.getObjective());
        routine.setSourceTemplate(template);
        routine.setStatus(status);
        routine.setAssignedDate(request.assignedDate() == null ? LocalDate.now() : request.assignedDate());
        routine.setGeneralNotes(StringUtils.hasText(request.generalNotes()) ? request.generalNotes().trim() : template.getGeneralNotes());
        routine.setInternalNotes(clean(request.internalNotes()));
        routine.setCreatedByUser(currentUser);
        for (TemplateDay sourceDay : template.getDays()) {
            routine.getDays().add(copyDay(sourceDay, routine));
        }
        if (routine.getStatus() != RoutineStatus.DRAFT) {
            validator.validate(routine);
        }
        return mapper.toResponse(routineRepository.save(routine));
    }

    private RoutineDay copyDay(TemplateDay source, Routine routine) {
        RoutineDay day = new RoutineDay();
        day.setRoutine(routine);
        day.setOrderIndex(source.getOrderIndex());
        day.setName(source.getName());
        day.setNotes(source.getNotes());
        for (TemplateBlock sourceBlock : source.getBlocks()) {
            day.getBlocks().add(copyBlock(sourceBlock, day));
        }
        return day;
    }

    private RoutineBlock copyBlock(TemplateBlock source, RoutineDay day) {
        RoutineBlock block = new RoutineBlock();
        block.setDay(day);
        block.setOrderIndex(source.getOrderIndex());
        block.setTitle(source.getTitle());
        block.setStructuralType(source.getStructuralType());
        block.setPurpose(source.getPurpose());
        block.setTotalDurationSeconds(source.getTotalDurationSeconds());
        block.setTargetRounds(source.getTargetRounds());
        block.setRoundRestSeconds(source.getRoundRestSeconds());
        block.setBlockNotes(source.getBlockNotes());
        for (TemplateExercise sourceExercise : source.getExercises()) {
            RoutineExercise exercise = new RoutineExercise();
            exercise.setBlock(block);
            exercise.setExercise(sourceExercise.getExercise());
            exercise.setOrderIndex(sourceExercise.getOrderIndex());
            exercise.setExerciseNotes(sourceExercise.getExerciseNotes());
            for (TemplateExerciseSet sourceSet : sourceExercise.getSets()) {
                RoutineExerciseSet set = new RoutineExerciseSet();
                set.setRoutineExercise(exercise);
                set.setSetNumber(sourceSet.getSetNumber());
                set.setSetKind(sourceSet.getSetKind());
                set.setTargetReps(sourceSet.getTargetReps());
                set.setTargetRepsMin(sourceSet.getTargetRepsMin());
                set.setTargetRepsMax(sourceSet.getTargetRepsMax());
                set.setTargetWeightKg(sourceSet.getTargetWeightKg());
                set.setTargetTimeSeconds(sourceSet.getTargetTimeSeconds());
                set.setTargetDistanceMeters(sourceSet.getTargetDistanceMeters());
                set.setRestAfterSeconds(sourceSet.getRestAfterSeconds());
                set.setTempo(sourceSet.getTempo());
                set.setExecutionCue(sourceSet.getExecutionCue());
                set.setRpe(sourceSet.getRpe());
                set.setNotes(sourceSet.getNotes());
                set.setToFailure(sourceSet.isToFailure());
                exercise.getSets().add(set);
            }
            block.getExercises().add(exercise);
        }
        return block;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
