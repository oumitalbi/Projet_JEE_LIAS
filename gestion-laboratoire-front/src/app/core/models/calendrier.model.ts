export type CalendrierSource = 'EVENEMENT' | 'REUNION';

export interface CalendrierItem {
  id: number;
  source: CalendrierSource;
  titre: string;
  type: string;
  dateDebut: string;
  dateFin: string | null;
  lieu: string | null;
  statut: string;
  annule: boolean;
  concerneMembre: boolean;
  description: string | null;
  organisateursIds: number[];
  participantsIds: number[];
}
