export type TypeNotification =
  | 'EVENEMENT'
  | 'DOCUMENT'
  | 'REUNION'
  | 'CONVENTION'
  | 'LOGISTIQUE'
  | 'MANDAT'
  | 'ADHESION'
  | 'PUBLICATION'
  | 'MESSAGE'
  | 'SYSTEME';

export interface NotificationItem {
  id: number;
  titre: string;
  contenu: string | null;
  typeNotification: TypeNotification;
  cibleType: string | null;
  cibleId: number | null;
  lue: boolean;
  archivee: boolean;
  dateCreation: string;
}

export interface MessageMember {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  photo: string | null;
  role: string;
  statut: string;
}

export interface MessageItem {
  id: number;
  typeMessage: 'DIRECT' | 'EQUIPE' | 'EVENEMENT';
  contenu: string;
  expediteur: MessageMember;
  destinataire: MessageMember | null;
  equipeDestinataire: string | null;
  evenementId: number | null;
  evenementTitre: string | null;
  lu: boolean;
  archivee: boolean;
  dateEnvoi: string;
}

export interface ConversationSummary {
  autreMembreId: number;
  autreMembreNom: string;
  autreMembrePrenom: string;
  autreMembreEmail: string;
  autreMembrePhoto: string | null;
  autreMembreRole: string;
  dernierMessage: string;
  dateDernierMessage: string;
  nonLus: number;
}

export interface UnreadCounts {
  notifications: number;
  messagesTotal: number;
  messagesDirect: number;
  messagesEquipe: number;
  messagesEvenement: number;
}
