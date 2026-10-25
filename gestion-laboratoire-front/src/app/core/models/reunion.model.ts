export type StatutReunion = 'PLANIFIEE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE' | 'ARCHIVEE';

export interface ReunionMembre {
  id: number;
  nom: string;
  prenom: string;
  photo?: string | null;
  equipe?: string | null;
}

export interface ReunionDocument {
  id: number;
  nomFichier: string;
  type: string;
  dateDepot?: string | null;
}

export interface ReunionPvHistorique {
  id: number;
  nomFichier: string;
  dateDepot?: string | null;
  ajoutePar?: ReunionMembre | null;
}

export interface Reunion {
  id: number;
  titre: string;
  ordreDuJour: string;
  dateDebut: string;
  dateFin?: string | null;
  lieu: string;
  statut: StatutReunion;
  organisateur?: ReunionMembre | null;
  participants: ReunionMembre[];
  pv?: ReunionDocument | null;
  historiquePv: ReunionPvHistorique[];
  archivee: boolean;
  creePar?: ReunionMembre | null;
  dateCreation?: string | null;
  modifiePar?: ReunionMembre | null;
  dateModification?: string | null;
  statutChangePar?: ReunionMembre | null;
  dateChangementStatut?: string | null;
  peutModifier: boolean;
  peutGererPv: boolean;
  peutChangerStatut: boolean;
  estParticipant: boolean;
}

export interface ReunionRequest {
  titre: string;
  ordreDuJour: string;
  dateDebut: string;
  dateFin: string;
  lieu: string;
  participantsIds: number[];
}
