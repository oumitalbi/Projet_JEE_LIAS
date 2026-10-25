import { Membre } from './membre.model';

export type TypePublication = 'ARTICLE_JOURNAL' | 'CONFERENCE' | 'CHAPITRE_LIVRE' | 'THESE' | 'BREVET';

export interface Publication {
  id: number;
  titre: string;
  resume?: string | null;
  datePublication: string;
  lienDoi?: string | null;
  type: TypePublication;
  estArchive: boolean;
  auteurs: Membre[];
  dateCreation?: string | null;
  dateMiseAJour?: string | null;
}

export interface PublicationCreateRequest {
  publication: Partial<Publication>;
  auteursIds: number[];
}

export function getTypePublicationLabel(type: TypePublication): string {
  const labels: Record<TypePublication, string> = {
    ARTICLE_JOURNAL: 'Article de journal',
    CONFERENCE: 'Conférence',
    CHAPITRE_LIVRE: 'Chapitre de livre',
    THESE: 'Thèse',
    BREVET: 'Brevet'
  };
  return labels[type] || type;
}

export function getTypePublicationBadgeClass(type: TypePublication): string {
  const classes: Record<TypePublication, string> = {
    ARTICLE_JOURNAL: 'badge-blue',
    CONFERENCE: 'badge-purple',
    CHAPITRE_LIVRE: 'badge-orange',
    THESE: 'badge-green',
    BREVET: 'badge-gray'
  };
  return classes[type] || 'badge-gray';
}
