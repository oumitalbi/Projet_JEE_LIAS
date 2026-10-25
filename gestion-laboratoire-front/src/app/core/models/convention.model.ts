export type StatutConvention = 'BROUILLON' | 'A_VENIR' | 'ACTIVE' | 'EXPIREE' | 'ARCHIVEE';
export type TypePartenaire = 'ACADEMIQUE' | 'INDUSTRIEL' | 'INSTITUTIONNEL';

export interface Partenaire {
  id: number;
  nom: string;
  type: TypePartenaire;
  pays?: string | null;
  contactEmail?: string | null;
  archive?: boolean;
  archivePar?: string | null;
  dateArchivage?: string | null;
}

export interface ConventionDocument {
  id: number;
  nomFichier: string;
  type: string;
  dateDepot?: string | null;
}

export interface ConventionActivite {
  id: number;
  titre: string;
  type?: string | null;
  statut?: string | null;
  dateDebut?: string | null;
  dateFin?: string | null;
}

export interface Convention {
  id: number;
  titre: string;
  description?: string | null;
  dateSignature?: string | null;
  dateExpiration?: string | null;
  statut: StatutConvention;
  archivee: boolean;
  partenaire: Partenaire;
  document?: ConventionDocument | null;
  historiqueDocuments: ConventionDocument[];
  activites: ConventionActivite[];
  creePar?: { id: number; nom: string; prenom: string } | null;
  dateCreation?: string | null;
  modifiePar?: { id: number; nom: string; prenom: string } | null;
  dateModification?: string | null;
  archivePar?: { id: number; nom: string; prenom: string } | null;
  dateArchivage?: string | null;
  peutModifier: boolean;
  peutArchiver: boolean;
}

export interface ConventionRequest {
  titre: string;
  description: string;
  dateSignature: string;
  dateExpiration: string;
  partenaireId: number;
}

export interface PartenaireRequest {
  nom: string;
  type: TypePartenaire;
  pays: string;
  contactEmail: string;
}
