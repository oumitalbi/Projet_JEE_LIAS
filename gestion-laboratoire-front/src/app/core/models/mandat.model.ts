import { Membre } from './membre.model';

export type FonctionMandat = 'DIRECTEUR' | 'VICE_DIRECTEUR' | 'CHEF_EQUIPE';
export type EquipeMandat = 'ISDIAC' | 'SIMA' | 'ILIAS' | 'SDTIC' | 'DIRECTION' | 'AUCUNE';

export interface Mandat {
  id: number;
  membre: Pick<Membre, 'id' | 'nom' | 'prenom' | 'photo' | 'equipeLaboratoire'>;
  responsabilite: FonctionMandat;
  equipe: EquipeMandat | null;
  dateDebut: string;
  dateFin: string | null;
  actif: boolean;
  description: string | null;
  attribuePar: Pick<Membre, 'id' | 'nom' | 'prenom' | 'photo' | 'equipeLaboratoire'> | null;
  dateCreation: string | null;
  cloturePar: Pick<Membre, 'id' | 'nom' | 'prenom' | 'photo' | 'equipeLaboratoire'> | null;
  dateCloture: string | null;
  motifCloture: string | null;
}

export interface MandatRequest {
  membreId: number;
  responsabilite: FonctionMandat;
  equipe: EquipeMandat | null;
  dateDebut: string;
  description?: string;
}

export interface HistoriqueRole {
  id: number;
  ancienRole: string | null;
  nouveauRole: string;
  ancienneEquipe: string | null;
  nouvelleEquipe: string | null;
  motif: string | null;
  dateChangement: string;
  membreId: number;
  auteur: string | null;
}

export interface PeriodeAffiliation {
  id: number;
  dateDebut: string;
  dateFin: string | null;
  statut: string;
  motif: string | null;
}

export interface HistoriqueProfil {
  id: number;
  membreId: number;
  action: string;
  details: string | null;
  auteur: string | null;
  dateAction: string;
}

export interface DateCreationLaboratoire {
  dateCreation: string;
}
