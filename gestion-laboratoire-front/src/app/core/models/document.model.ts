export type TypeDocument =
  | 'DEMANDE_FINANCEMENT'
  | 'PROGRAMME'
  | 'ATTESTATION'
  | 'RAPPORT'
  | 'PV'
  | 'CONVENTION'
  | 'DOCUMENT_ADMINISTRATIF';

export interface DocumentArchive {
  id: number;
  nomFichier: string;
  contentType?: string | null;
  taille?: number | null;
  typeDocument: TypeDocument;
  evenementId?: number | null;
  evenementTitre?: string | null;
  dateUpload?: string | null;
  deposePar?: { id: number; nom: string; prenom: string } | null;
  peutArchiver: boolean;
  estArchive: boolean;
  dateArchivage?: string | null;
  archivePar?: { id: number; nom: string; prenom: string } | null;
}
