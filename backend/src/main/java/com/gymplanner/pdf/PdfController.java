package com.gymplanner.pdf;

import com.gymplanner.auth.CustomUserDetailsService.GymPrincipal;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PdfController {
    private final RoutinePdfService routinePdfService;
    private final WhatsAppTextService whatsAppTextService;

    @GetMapping("/api/routines/{id}/pdf")
    ResponseEntity<byte[]> getPdf(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        String filename = routinePdfService.buildFilename(id, principal.gymId());
        byte[] pdf = routinePdfService.generatePdf(id, principal.gymId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping(value = "/api/routines/{id}/text", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    ResponseEntity<String> getText(@AuthenticationPrincipal GymPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(whatsAppTextService.generateText(id, principal.gymId()));
    }
}
