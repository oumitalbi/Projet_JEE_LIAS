export type TypeEvenement = 'CONFERENCE' | 'SEMINAIRE' | 'WORKSHOP';
export type StatutEvenement = 'ANNULE' | 'A_VENIR' | 'EN_COURS' | 'PASSE';

export interface MembreResume {
  id: number;
  nom: string;
  prenom: string;
  photo?: string | null;
  equipe?: string | null;
}

export interface Evenement {
  id: number;
  titre: string;
  description?: string | null;
  type?: TypeEvenement | null;
  statut?: StatutEvenement | null;
  edition?: string | null;
  annee?: number | null;
  dateDebut?: string | null;
  dateFin?: string | null;
  lieu?: string | null;
  organisateurs?: MembreResume[] | null;
  participants?: MembreResume[] | null;
  nbParticipants?: number | null;
  estInscrit?: boolean;
  peutModifier?: boolean;
  peutAnnuler?: boolean;
  motifAnnulation?: string | null;
  annulePar?: MembreResume | null;
  dateAnnulation?: string | null;
  creePar?: MembreResume | null;
  dateCreation?: string | null;
  modifiePar?: MembreResume | null;
  dateModification?: string | null;
}

export interface EvenementPublic {
  id: number;
  titre: string;
  description?: string | null;
  type?: TypeEvenement | null;
  dateDebut?: string | null;
  dateFin?: string | null;
  lieu?: string | null;
  edition?: string | null;
}

export interface EvenementRequest {
  titre: string;
  description: string;
  type: TypeEvenement;
  dateDebut: string;
  dateFin: string;
  lieu: string;
  edition: string | null;
  organisateursIds: number[];
}
