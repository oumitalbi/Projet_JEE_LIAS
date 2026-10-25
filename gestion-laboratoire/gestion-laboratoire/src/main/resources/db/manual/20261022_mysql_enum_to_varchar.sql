-- Run once against lias_db before starting the application.
-- These conversions preserve current enum values while allowing new Java enum values.
ALTER TABLE demandes_adhesion MODIFY statut VARCHAR(50) NULL;
ALTER TABLE demandes_materiel MODIFY statut VARCHAR(50) NOT NULL;
ALTER TABLE documents MODIFY type_document VARCHAR(50) NOT NULL;
ALTER TABLE evenements MODIFY type VARCHAR(50) NULL;
ALTER TABLE historique_affiliations MODIFY statut VARCHAR(50) NOT NULL;
ALTER TABLE mandats MODIFY responsabilite VARCHAR(50) NOT NULL;
ALTER TABLE materiels MODIFY etat VARCHAR(50) NULL;
ALTER TABLE membres MODIFY equipe_laboratoire VARCHAR(50) NOT NULL;
ALTER TABLE membres MODIFY role VARCHAR(50) NOT NULL;
ALTER TABLE membres MODIFY statut VARCHAR(50) NOT NULL;
ALTER TABLE membres MODIFY role_systeme VARCHAR(50) NULL;
ALTER TABLE messages MODIFY equipe_destinataire VARCHAR(50) NULL;
ALTER TABLE partenaires MODIFY type VARCHAR(50) NULL;
ALTER TABLE publications MODIFY type_publication VARCHAR(50) NOT NULL;
ALTER TABLE reunions MODIFY statut VARCHAR(50) NULL;
ALTER TABLE reunions MODIFY statut_avant_archivage VARCHAR(50) NULL;
