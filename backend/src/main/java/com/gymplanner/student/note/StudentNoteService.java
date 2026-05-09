package com.gymplanner.student.note;

import com.gymplanner.shared.exception.NotFoundException;
import com.gymplanner.student.Student;
import com.gymplanner.student.StudentService;
import com.gymplanner.student.note.dto.CreateNoteRequest;
import com.gymplanner.student.note.dto.NoteResponse;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import com.gymplanner.user.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentNoteService {

    private final StudentNoteRepository noteRepository;
    private final StudentService studentService;
    private final UserRepository userRepository;
    private final StudentNoteMapper noteMapper;

    @Transactional(readOnly = true)
    public List<NoteResponse> list(Long gymId, Long studentId) {
        studentService.getEntity(gymId, studentId);
        return noteRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(noteMapper::toResponse)
                .toList();
    }

    @Transactional
    public NoteResponse create(Long gymId, Long studentId, Long authorUserId, CreateNoteRequest request) {
        Student student = studentService.getEntity(gymId, studentId);
        User author = userRepository.findById(authorUserId)
                .filter(user -> user.getGym().getId().equals(gymId))
                .orElseThrow(() -> new NotFoundException("User not found."));

        StudentNote note = new StudentNote();
        note.setStudent(student);
        note.setAuthorUser(author);
        note.setContent(request.content().trim());
        return noteMapper.toResponse(noteRepository.save(note));
    }

    @Transactional
    public void delete(Long gymId, Long studentId, Long noteId, Long requesterUserId, UserRole requesterRole) {
        studentService.getEntity(gymId, studentId);
        StudentNote note = noteRepository.findByIdAndStudentId(noteId, studentId)
                .orElseThrow(() -> new NotFoundException("Student note not found."));
        if (!note.getAuthorUser().getId().equals(requesterUserId) && requesterRole != UserRole.OWNER) {
            throw new AccessDeniedException("Only the note author or an owner can delete this note.");
        }
        noteRepository.delete(note);
    }
}
