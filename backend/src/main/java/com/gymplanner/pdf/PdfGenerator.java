package com.gymplanner.pdf;

import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Component
public class PdfGenerator {

    public byte[] htmlToPdf(String xhtml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new PdfGenerationException("Error generando PDF: " + e.getMessage(), e);
        }
    }
}
