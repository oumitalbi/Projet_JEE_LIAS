# LIAS - Application web de gestion du laboratoire

Projet JEE - Master Data Science & Big Data (2025-2026)  
Faculte des Sciences Ben M'Sik - Casablanca

Application de gestion du laboratoire **LIAS** : membres, publications, evenements, documents, logistique, reunions/PV, conventions, mandats, adhesions, notifications et rapports annuels.

## Technologies

| Partie | Stack |
|---|---|
| Backend | Java 21, Spring Boot 3.2.6, Spring Security (JWT), JPA/Hibernate, Maven Wrapper |
| Base de donnees | MySQL 8 |
| Frontend | Angular (standalone components) |
| Documentation API | Swagger / OpenAPI |

## Structure du depot

```text
Projet_JEE_LIAS/
|-- gestion-laboratoire/
|   `-- gestion-laboratoire/     # Module backend Spring Boot (pom.xml, mvnw.cmd)
|-- gestion-laboratoire-front/   # Frontend Angular
`-- README.md
```

## Prerequis

- JDK 21
- Node.js (LTS) et Angular CLI : `npm install -g @angular/cli`
- MySQL 8 (service demarre)

## Installation et lancement

### 1. Base de donnees et configuration

Creer la base MySQL :

```sql
CREATE DATABASE lias_db CHARACTER SET utf8mb4;
```

La configuration locale du backend se trouve dans `gestion-laboratoire/gestion-laboratoire/src/main/resources/application.properties`.
Copier `application.properties.example` en `application.properties`, puis renseigner les parametres MySQL (base, utilisateur et mot de passe). Le SMTP est facultatif pour demarrer l'application ; il faut cependant renseigner un compte SMTP valide pour envoyer les e-mails (par exemple les notifications de decision d'adhesion et les liens de reinitialisation). Sans SMTP fonctionnel, ces e-mails ne seront pas envoyes.

`JWT_SECRET` est facultative en developpement : sans variable d'environnement, l'application utilise une cle de demonstration de plus de 32 caracteres. Pour remplacer cette valeur, definir `JWT_SECRET` avec une cle privee encodee en Base64 d'au moins 32 octets. La cle de demonstration et les identifiants fictifs du fichier d'exemple ne doivent jamais etre utilises en production. Le fichier `application.properties` est exclu du depot Git.

### 2. Backend

Depuis `Projet_JEE_LIAS/gestion-laboratoire/gestion-laboratoire` :

```bash
./mvnw spring-boot:run
```

Sous Windows PowerShell :

```powershell
.\mvnw.cmd spring-boot:run
```

Pour definir une cle JWT personnalisee (recommande hors developpement), la variable d'environnement `JWT_SECRET` peut etre configuree avant cette commande.

Attendre le message `Started GestionLaboratoireApplication`.

- API : `http://localhost:8080`
- **Swagger** : `http://localhost:8080/swagger-ui/index.html`

> Si le port 8080 est occupe, modifier `server.port` dans le fichier local `application.properties` et `apiUrl` dans `gestion-laboratoire-front/src/environments/environment.ts` (ainsi que `environment.development.ts` si necessaire).

### 3. Frontend

Dans un second terminal, depuis `Projet_JEE_LIAS/gestion-laboratoire-front` :

```bash
npm install
npm start
```

Ouvrir `http://localhost:4200`.

## Comptes de demonstration

| Role | Email | Mot de passe initial |
|---|---|---|
| Directrice | f.benabbolias@gmail.com | `admin123` |
| Vice-directeur | a.belangour@lias.ma | `password123` |
| Cheffe d'equipe | n.sael@lias.ma | `password123` |
| Chef d'equipe | m.aitdaoud@lias.ma | `password123` |
| Cheffe d'equipe | s.ouahabi@lias.ma | `password123` |
| Chef d'equipe | a.ettaoufik@lias.ma | `password123` |
| Administrateur | admin@lias.ma | `password123` |

Ces identifiants sont reserves a la demonstration locale. Changez-les avant tout deploiement et n'utilisez pas ces mots de passe en production.

## Tester l'API avec Swagger

1. Envoyer `POST /api/auth/login` avec un compte ci-dessus.
2. Copier le token JWT renvoye.
3. Cliquer sur **Authorize** dans Swagger et coller le token.

## Regles metier principales

- Separation stricte **statut** (permanent, associe, doctorant...) / **role** (directeur, chef d'equipe...).
- Un membre modifie uniquement ses propres informations personnelles ; role, statut, equipe et compte relevent de l'administration.
- Aucune donnee n'est supprimee definitivement : archivage logique et historique conserve.
- Archivage d'une publication : reserve a ses auteurs ou au directeur/admin (controle cote backend, 403 sinon).
- Comptes retraites geles, anciens membres desactives, historique conserve.

## Auteur

OUMAIMA TALBI - Master Data Science & Big Data, FSBM  
Encadrant : Pr. ABDESSAMAD BELANGOUR
