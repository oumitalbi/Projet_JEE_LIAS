export interface RapportAnnuel {
  id: number | null;
  annee: number;
  version: number;
  dateGeneration: string | null;
  generePar: string | null;
  nbEvenements: number;
  nbPublications: number;
  nbReunions: number;
  nbConventionsActives: number;
  nbDemandesLogistiques: number;
  nbMandats: number;
  nbNouveauxMembres: number;
  nbDemandesAdhesion: number;
  evenementsParType: Record<string, number> | null;
  publicationsParType: Record<string, number> | null;
  publicationsParEquipe: Record<string, number> | null;
  logistiqueParStatut: Record<string, number> | null;
  mandatsParRole: Record<string, number> | null;
  membresParStatut: Record<string, number> | null;
  adhesionsParDecision: Record<string, number> | null;
}

export type RapportArchiveSummary = Pick<RapportAnnuel, 'id' | 'annee' | 'version' | 'dateGeneration' | 'generePar'>;
