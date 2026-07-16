package com.haadlit_sp.appCoreLogic.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;


/** Pulls plain text out of a PDF with PDFBox. Fails soft: an unreadable file yields "". */
public class PdfTextExtractor {

    private static final Logger LOG = System.getLogger(PdfTextExtractor.class.getName());

    /** Extracted text, or "" when the file is missing, encrypted, image-only, or not a PDF. */
    public String extract(Path pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            return text == null ? "" : text;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not read PDF " + pdf, e);
            return "";
        }
    }
}
