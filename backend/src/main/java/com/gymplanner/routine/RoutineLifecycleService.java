package com.gymplanner.routine;

import com.gymplanner.routine.dto.CreateNextRoutineRequest;
import com.gymplanner.routine.dto.CreateNextRoutineResponse;
import com.gymplanner.routine.dto.FinishAndCreateNextRequest;
import com.gymplanner.routine.dto.FinishAndCreateNextResponse;
import com.gymplanner.routine.dto.RoutineResponse;
import com.gymplanner.routine.dto.WeightAdjustmentInput;
import com.gymplanner.shared.exception.BusinessRuleException;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoutineLifecycleService {
    private static final String CYCLE_SEPARATOR = " \u2014 Ciclo ";
    private static final Pattern CYCLE_NAME_PATTERN = Pattern.compile("^(.*) \\u2014 Ciclo (\\d+)$");

    private final RoutineRepository routineRepository;
    private final RoutineService routineService;
    private final RoutineValidator validator;
    private final RoutineMapper mapper;
    private final UserRepository userRepository;
    private final RoutineWeightAdjustService weightAdjustService;

    @Transactional
    public FinishAndCreateNextResponse finishAndCreateNext(Long gymId, Long userId, FinishAndCreateNextRequest request) {
        Routine current = routineService.getFull(gymId, request.routineId());
        if (current.getStatus() != RoutineStatus.ACTIVE) {
            throw new BusinessRuleException("Solo se puede cerrar y crear el próximo ciclo desde una rutina activa.");
        }

        User currentUser = userRepository.getReferenceById(userId);
        Instant now = Instant.now();
        routineService.finishRoutineEntity(current, currentUser, now, request.closureNotes());

        CopyResult copy = createLinkedCopy(gymId, current, currentUser, request.newRoutineName(), request.newAssignedDate(), request.newStatus(), request.copyGeneralNotes(), request.copyInternalNotes(), request.weightAdjustment());
        Routine savedNext = routineRepository.save(copy.routine());
        RoutineResponse newRoutine = mapper.toResponse(savedNext);
        return new FinishAndCreateNextResponse(
                toSummary(current),
                newRoutine,
                copy.weightSetsAdjusted());
    }

    @Transactional
    public CreateNextRoutineResponse createNextFromExisting(Long gymId, Long userId, Long routineId, CreateNextRoutineRequest request) {
        Routine source = routineService.getFull(gymId, routineId);
        if (source.getStatus() == RoutineStatus.ACTIVE) {
            throw new BusinessRuleException("Usá 'Finalizar y crear próxima' para rutinas activas.");
        }
        if (source.getStatus() == RoutineStatus.DRAFT) {
            throw new BusinessRuleException("No se puede crear un ciclo siguiente desde un borrador.");
        }

        User currentUser = userRepository.getReferenceById(userId);
        RoutineStatus newStatus = parseNewStatus(request.newStatus());
        if (newStatus == RoutineStatus.ACTIVE) {
            routineService.finishPreviousActive(gymId, source.getStudent().getId(), source.getId(), currentUser);
        }

        CopyResult copy = createLinkedCopy(gymId, source, currentUser, request.newRoutineName(), request.newAssignedDate(), request.newStatus(), request.copyGeneralNotes(), request.copyInternalNotes(), request.weightAdjustment());
        Routine savedNext = routineRepository.save(copy.routine());
        return new CreateNextRoutineResponse(toSummary(source), mapper.toResponse(savedNext), copy.weightSetsAdjusted());
    }

    private String resolveNextName(Long gymId, Routine current, String requestedName) {
        if (StringUtils.hasText(requestedName)) {
            return requestedName.trim();
        }
        String baseName = baseCycleName(current.getName());
        int nextCycle = routineRepository.findByStudentIdAndStudentGymId(current.getStudent().getId(), gymId).stream()
                .map(Routine::getName)
                .map(CYCLE_NAME_PATTERN::matcher)
                .filter(Matcher::matches)
                .filter(matcher -> matcher.group(1).equals(baseName))
                .mapToInt(matcher -> Integer.parseInt(matcher.group(2)))
                .max()
                .orElse(1) + 1;
        return baseName + CYCLE_SEPARATOR + nextCycle;
    }

    private String baseCycleName(String name) {
        Matcher matcher = CYCLE_NAME_PATTERN.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    private RoutineStatus parseNewStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return RoutineStatus.DRAFT;
        }
        RoutineStatus status = RoutineStatus.valueOf(value.trim());
        if (status != RoutineStatus.DRAFT && status != RoutineStatus.ACTIVE) {
            throw new BusinessRuleException("newStatus debe ser DRAFT o ACTIVE.");
        }
        return status;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private CopyResult createLinkedCopy(
            Long gymId,
            Routine source,
            User currentUser,
            String newRoutineName,
            java.time.LocalDate newAssignedDate,
            String newStatus,
            Boolean copyGeneralNotes,
            Boolean copyInternalNotes,
            WeightAdjustmentInput weightAdjustment) {
        Routine next = new Routine();
        next.setStudent(source.getStudent());
        next.setCreatedByUser(currentUser);
        next.setPreviousRoutine(source);
        next.setName(resolveNextName(gymId, source, newRoutineName));
        next.setObjective(source.getObjective());
        next.setStatus(parseNewStatus(newStatus));
        next.setAssignedDate(newAssignedDate);
        next.setGeneralNotes(Boolean.FALSE.equals(copyGeneralNotes) ? null : clean(source.getGeneralNotes()));
        next.setInternalNotes(Boolean.TRUE.equals(copyInternalNotes) ? clean(source.getInternalNotes()) : null);
        next.getDays().addAll(routineService.copyDays(source, next));

        int adjustedSets = applyWeightAdjustment(next, weightAdjustment);
        if (next.getStatus() != RoutineStatus.DRAFT) {
            validator.validate(next);
        }
        return new CopyResult(next, adjustedSets);
    }

    private int applyWeightAdjustment(Routine routine, WeightAdjustmentInput weightAdjustment) {
        if (weightAdjustment == null) {
            return 0;
        }
        return weightAdjustService.applyAdjustment(
                routine,
                RoutineWeightAdjustmentScopeType.ROUTINE,
                weightAdjustment.percentage(),
                weightAdjustment.roundingStepKg());
    }

    private com.gymplanner.routine.dto.RoutineSummaryResponse toSummary(Routine routine) {
        return mapper.toSummary(routine, routineRepository.countDays(routine.getId()), routineRepository.countBlocks(routine.getId()), routineRepository.countExercises(routine.getId()));
    }

    private record CopyResult(Routine routine, int weightSetsAdjusted) {}
}
