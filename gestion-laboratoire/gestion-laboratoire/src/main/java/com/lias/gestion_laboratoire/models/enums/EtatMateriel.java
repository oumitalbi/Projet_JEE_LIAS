package com.lias.gestion_laboratoire.models.enums;

/**
 * Enumération représentant l'état physique du matériel géré par le laboratoire.
 */
public enum EtatMateriel {
    /** Matériel neuf, récemment acquis. */
    NEUF,
    
    /** Matériel en bon état de fonctionnement. */
    BON_ETAT,
    
    /** Matériel usé mais toujours fonctionnel. */
    USE,
    
    /** Matériel en panne, nécessitant une réparation. */
    EN_PANNE,
    
    /** Matériel obsolète, destiné à être retiré de l'inventaire. */
    OBSOLETE
}
