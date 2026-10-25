export type StatutAdhesion = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';
export type StatutCandidat = 'PERMANENT' | 'ASSOCIE' | 'DOCTORANT';
export type EquipeCandidat = 'ISDIAC' | 'SIMA' | 'SDTIC' | 'ILIAS';

export interface DemandeAdhesion {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  statutSouhaite: StatutCandidat;
  etablissementOrigine: string;
  laboratoireOrigine: string | null;
  equipeSouhaitee: EquipeCandidat;
  lettreMotivation: string;
  centresInteret: string | null;
  biographie: string | null;
  consentementDonnees: boolean;
  consentementDate: string | null;
  statut: StatutAdhesion;
  dateDemande: string;
  dateTraitement: string | null;
  motifDecision: string | null;
  traitePar: { id: number; nom: string; prenom: string } | null;
  membreCree: { id: number; nom: string; prenom: string } | null;
  cvDisponible: boolean;
  lienTelechargementCv: string | null;
}

export interface DemandeAdhesionReceipt {
  id: number;
  statut: StatutAdhesion;
  dateDemande: string;
  message: string;
}

export interface TraitementAdhesionRequest {
  estAcceptee: boolean;
  statutAccorde?: StatutCandidat;
  equipeAccordee?: EquipeCandidat;
  roleInitial?: 'MEMBRE_EQUIPE';
  motif?: string;
}

export interface TraitementAdhesionResponse {
  demande: DemandeAdhesion;
  notificationEnvoyee: boolean;
  message: string;
}
