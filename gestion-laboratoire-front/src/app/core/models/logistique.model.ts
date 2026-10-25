export type StatutDemandeMateriel = 'EN_ATTENTE' | 'VALIDEE' | 'REFUSEE' | 'LIVREE' | 'RETOURNEE';

export interface Materiel {
  id: number;
  nom: string;
  description?: string | null;
  numeroInventaire: string;
  categorie: string;
  etat: string;
  dateAcquisition?: string | null;
  quantiteTotale: number;
  quantiteDisponible: number;
  consommable: boolean;
  archivee: boolean;
  disponible: boolean;
  creePar?: string | null;
  dateCreation?: string | null;
  modifiePar?: string | null;
  dateModification?: string | null;
  archivePar?: string | null;
  dateArchivage?: string | null;
  restaurePar?: string | null;
  dateRestauration?: string | null;
}

export interface MembreLogistique {
  id: number;
  nom: string;
  prenom: string;
  photo?: string | null;
  equipe?: string | null;
}

export interface DemandeMateriel {
  id: number;
  descriptionBesoin: string;
  nomBesoin?: string | null;
  quantiteDemandee: number;
  quantiteRetournee: number;
  motifDemande?: string | null;
  motifRefus?: string | null;
  statut: StatutDemandeMateriel;
  dateDemande?: string | null;
  dateReponse?: string | null;
  dateRetour?: string | null;
  demandeur: MembreLogistique;
  traitePar?: MembreLogistique | null;
  retournePar?: MembreLogistique | null;
  materiel?: Pick<Materiel, 'id' | 'nom' | 'categorie' | 'consommable'> | null;
  peutValider: boolean;
  peutRetourner: boolean;
}

export interface MaterielRequest {
  nom: string;
  description: string;
  numeroInventaire: string;
  categorie: string;
  etat: string;
  dateAcquisition: string | null;
  quantiteInitiale: number;
  consommable: boolean;
}

export interface ArrivageMateriel {
  id: number;
  materielId: number;
  materielNom: string;
  quantite: number;
  dateArrivage: string;
  fournisseur?: string | null;
  bonLivraison?: string | null;
  enregistrePar: string;
  dateCreation?: string | null;
}

export interface ArrivageRequest {
  materielId: number;
  quantite: number;
  dateArrivage: string;
  fournisseur: string;
  bonLivraison: string;
}

export interface DistributionEquipe {
  membreId: number;
  nom: string;
  prenom: string;
  equipe: string;
  quantiteDistribuee: number;
}
