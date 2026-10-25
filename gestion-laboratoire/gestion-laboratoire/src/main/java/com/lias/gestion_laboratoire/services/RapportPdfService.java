package com.lias.gestion_laboratoire.services;

import com.lias.gestion_laboratoire.dto.RapportAnnuelDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class RapportPdfService {

    public byte[] genererPdf(RapportAnnuelDTO rapport) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 48, 48);
            PdfWriter.getInstance(document, output);
            document.open();
            Font titre = police(18, Font.BOLD, new java.awt.Color(17, 42, 80));
            Font sousTitre = police(11, Font.NORMAL, new java.awt.Color(37, 149, 181));
            Font section = police(12, Font.BOLD, new java.awt.Color(17, 42, 80));
            Font texte = police(9, Font.NORMAL, java.awt.Color.DARK_GRAY);

            document.add(new Paragraph("Rapport annuel d'activité LIAS", titre));
            document.add(new Paragraph("Année " + rapport.getAnnee() + "  •  Version " + rapport.getVersion(), sousTitre));
            document.add(new Paragraph("Généré le "
                    + rapport.getDateGeneration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " par " + rapport.getGenerePar(), texte));
            document.add(Chunk.NEWLINE);
            ajouterSection(document, "Synthèse", section, Map.of(
                    "Événements", rapport.getNbEvenements(),
                    "Publications", rapport.getNbPublications(),
                    "Réunions", rapport.getNbReunions(),
                    "Conventions actives", rapport.getNbConventionsActives(),
                    "Demandes logistiques", rapport.getNbDemandesLogistiques(),
                    "Mandats", rapport.getNbMandats(),
                    "Nouveaux membres", rapport.getNbNouveauxMembres(),
                    "Demandes d'adhésion", rapport.getNbDemandesAdhesion()));
            ajouterSection(document, "Événements par type", section, rapport.getEvenementsParType());
            ajouterSection(document, "Publications par type", section, rapport.getPublicationsParType());
            ajouterSection(document, "Publications par équipe", section, rapport.getPublicationsParEquipe());
            ajouterSection(document, "Logistique par statut", section, rapport.getLogistiqueParStatut());
            ajouterSection(document, "Mandats par rôle", section, rapport.getMandatsParRole());
            ajouterSection(document, "Mouvements de membres par statut", section, rapport.getMembresParStatut());
            ajouterSection(document, "Décisions d'adhésion", section, rapport.getAdhesionsParDecision());
            document.close();
            return output.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("La génération du PDF annuel a échoué.", exception);
        }
    }

    private Font police(float size, int style, java.awt.Color color) throws DocumentException, IOException {
        BaseFont base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        return new Font(base, size, style, color);
    }

    private void ajouterSection(Document document, String titre, Font policeTitre, Map<String, Long> donnees)
            throws DocumentException {
        document.add(new Paragraph(titre, policeTitre));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 1});
        table.setSpacingBefore(4);
        table.setSpacingAfter(10);
        Font cellule = new Font(Font.HELVETICA, 9, Font.NORMAL, java.awt.Color.DARK_GRAY);
        Font enTete = new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE);
        PdfPCell libelle = new PdfPCell(new Phrase("Catégorie", enTete));
        PdfPCell valeur = new PdfPCell(new Phrase("Total", enTete));
        libelle.setBackgroundColor(new java.awt.Color(17, 42, 80));
        valeur.setBackgroundColor(new java.awt.Color(17, 42, 80));
        table.addCell(libelle);
        table.addCell(valeur);
        if (donnees != null) {
            donnees.forEach((nom, total) -> {
                table.addCell(new Phrase(nom, cellule));
                table.addCell(new Phrase(String.valueOf(total), cellule));
            });
        }
        document.add(table);
    }
}
