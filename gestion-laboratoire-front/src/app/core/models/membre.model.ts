export interface Membre {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  role: string;
  statut: string;
  equipeLaboratoire: string;
  titreAcademique?: string | null;
  photo?: string | null;
  biographie?: string | null;
  etablissementOrigine?: string | null;
  dateNaissance?: string | null;
  dateEmbauche?: string | null;
  dateAffiliation?: string | null;
  materielsEnPossession?: any[] | null;
  mesDemandes?: any[] | null;
  roleSysteme?: string | null;
  compteActif?: boolean | null;
  compteGele?: boolean | null;
  dateCreation?: string | null;
  dateMiseAJour?: string | null;
}
