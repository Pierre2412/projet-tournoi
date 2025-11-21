/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Fichier : MainConsoleTournoi.java
 * Gestion complète du tournoi en mode console.
 */
package fr.insa.toto.model;

import fr.insa.beuvron.utils.ConsoleFdB;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.beuvron.utils.list.ListUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MainConsoleTournoi {

    // =========================================================================
    // 1. MENU PRINCIPAL & CONNEXION
    // =========================================================================

    public static void menuPrincipal() {
        Connection con = null;
        try {
            con = ConnectionSimpleSGBD.defaultCon();
            
            System.out.println("========================================");
            System.out.println("   GESTION DE TOURNOI - PROJET 2025");
            System.out.println("========================================");

            // Login simplifié (Surnom) [cite: 36]
            String surnom = ConsoleFdB.entreeString("Veuillez vous identifier (Surnom) : ");
            Optional<Joueur> u = Joueur.findBySurnom(con, surnom);
            
            boolean isAdmin = false;
            if (u.isPresent()) {
                Joueur user = u.get();
                // Vérification du rôle ('A' = Admin) [cite: 37]
                if ("A".equals(user.getRole())) {
                    isAdmin = true;
                    System.out.println(">>> Bonjour ADMIN " + user.getSurnom() + " <<<");
                } else {
                    System.out.println(">>> Bonjour " + user.getSurnom() + " (Accès Standard) <<<");
                }
            } else {
                System.out.println(">>> Utilisateur inconnu. Accès INVITÉ (Lecture seule) <<<");
            }

            int rep = -1;
            while (rep != 0) {
                System.out.println("\n---------------- MENU PRINCIPAL ----------------");
                System.out.println("1) Menu Joueurs (Liste, Détails, Historique)");
                System.out.println("2) Classement Général (Somme des scores)");
                System.out.println("3) Menu Matchs & Résultats");
                
                if (isAdmin) {
                    System.out.println("--- Administration ---");
                    System.out.println("4) Gestion Tournois & Rondes");
                    System.out.println("5) Gestion Terrains");
                    System.out.println("6) Paramètres Globaux (Nb Terrains/Joueurs)");
                    System.out.println("7) Outils BDD (Reset, Import/Export CSV)");
                }
                System.out.println("0) Quitter");
                
                rep = ConsoleFdB.entreeEntier("Votre choix : ");
                
                try {
                    if (rep == 1) menuJoueurs(con, isAdmin);
                    else if (rep == 2) afficherClassement(con);
                    else if (rep == 3) menuResultats(con, isAdmin);
                    else if (isAdmin) {
                        if (rep == 4) menuTournoisRondes(con);
                        else if (rep == 5) menuTerrains(con);
                        else if (rep == 6) menuParametres(con);
                        else if (rep == 7) menuOutilsBdd(con);
                    }
                } catch (Exception ex) {
                    System.out.println("ERREUR : " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            con.close();
            System.out.println("Au revoir !");
            
        } catch (Exception ex) {
            System.out.println("Erreur fatale de connexion : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // 2. GESTION DES JOUEURS
    // =========================================================================

    private static void menuJoueurs(Connection con, boolean isAdmin) throws SQLException {
        int rep = -1;
        while (rep != 0) {
            System.out.println("\n--- MENU JOUEURS ---");
            System.out.println("1) Lister tous les joueurs [cite: 53]");
            System.out.println("2) Voir détails d'un joueur (Historique & Stats) [cite: 54, 57]");
            if (isAdmin) {
                System.out.println("3) Ajouter un joueur [cite: 43]");
                System.out.println("4) Supprimer un joueur [cite: 43]");
            }
            System.out.println("0) Retour");
            rep = ConsoleFdB.entreeEntier("Choix : ");

            if (rep == 1) {
                List<Joueur> tous = Joueur.tousLesJoueurs(con);
                if (tous.isEmpty()) System.out.println("Aucun joueur.");
                else tous.forEach(System.out::println);
            } 
            else if (rep == 2) {
                afficherDetailsJoueur(con);
            } 
            else if (isAdmin && rep == 3) {
                Joueur j = Joueur.entreeConsole();
                j.saveInDB(con);
                System.out.println("Joueur ajouté avec succès (ID " + j.getId() + ")");
            } 
            else if (isAdmin && rep == 4) {
                List<Joueur> lst = Joueur.tousLesJoueurs(con);
                Optional<Joueur> j = ListUtils.selectOneOrCancel("Sélectionnez le joueur à supprimer :", lst, Joueur::toString);
                if (j.isPresent()) {
                    j.get().deleteInDB(con); // Utilise la méthode ajoutée précédemment
                }
            }
        }
    }

    // Affichage détaillé demandé par le PDF (Source 57)
    private static void afficherDetailsJoueur(Connection con) throws SQLException {
        List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
        Optional<Joueur> choix = ListUtils.selectOneOrCancel("Voir détails de quel joueur ?", joueurs, Joueur::getSurnom);
        
        if (choix.isPresent()) {
            Joueur j = choix.get();
            System.out.println("\n=== FICHE JOUEUR : " + j.getSurnom() + " ===");
            System.out.println("Catégorie    : " + (j.getCategorie() != null ? j.getCategorie() : "N/A"));
            System.out.println("Taille       : " + (j.getTailleCm() != null ? j.getTailleCm() + " cm" : "N/A"));
            System.out.println("Score Total  : " + j.getTotalScore() + " points"); // [cite: 35]
            
            System.out.println("\n--- Historique des Matchs ---");
            // Requête pour récupérer Ronde, Statut, Score et Terrain pour ce joueur
            String sql = "SELECT r.NUMERO as RONDE_NUM, m.STATUT as M_STATUT, e.SCORE, t.NOM as TERRAIN " +
                         "FROM composition c " +
                         "JOIN equipe e ON c.IDEQUIPE = e.ID " +
                         "JOIN matchs m ON e.IDMATCH = m.ID " +
                         "JOIN ronde r ON m.ID_RONDE = r.ID " +
                         "JOIN terrain t ON m.ID_TERRAIN = t.ID " +
                         "WHERE c.IDJOUEUR = ? " +
                         "ORDER BY r.NUMERO";
                         
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, j.getId());
                try (ResultSet rs = pst.executeQuery()) {
                    System.out.printf("%-8s | %-12s | %-8s | %-15s%n", "Ronde", "Statut", "Score", "Terrain");
                    System.out.println("-------------------------------------------------------");
                    boolean aJoue = false;
                    while (rs.next()) {
                        aJoue = true;
                        System.out.printf("%-8d | %-12s | %-8d | %-15s%n", 
                            rs.getInt("RONDE_NUM"), 
                            rs.getString("M_STATUT"), 
                            rs.getInt("SCORE"), 
                            rs.getString("TERRAIN"));
                    }
                    if (!aJoue) System.out.println("   Aucun match joué pour le moment.");
                }
            }
            System.out.println("=============================================\n");
        }
    }

    private static void afficherClassement(Connection con) throws SQLException {
        // 1. Forcer le recalcul des scores avant affichage [cite: 16]
        Joueur.mettreAJourClassementGeneral(con);
        
        System.out.println("\n=== CLASSEMENT GÉNÉRAL (Somme des points) ===");
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT SURNOM, TOTAL_SCORE FROM joueur ORDER BY TOTAL_SCORE DESC")) {
            try (ResultSet rs = pst.executeQuery()) {
                int rang = 1;
                System.out.printf("%-5s | %-20s | %-10s%n", "Rang", "Surnom", "Points");
                System.out.println("-----------------------------------------");
                while (rs.next()) {
                    System.out.printf("%-5d | %-20s | %-10d%n", 
                        rang++, rs.getString("SURNOM"), rs.getInt("TOTAL_SCORE"));
                }
            }
        }
    }

    // =========================================================================
    // 3. GESTION TOURNOIS & RONDES (Algorithme de création)
    // =========================================================================
    
    private static void menuTournoisRondes(Connection con) throws SQLException {
        int rep = -1;
        while (rep != 0) {
            System.out.println("\n--- GESTION TOURNOIS & RONDES ---");
            System.out.println("1) Lister les tournois");
            System.out.println("2) Créer un nouveau tournoi [cite: 42]");
            System.out.println("3) Gérer les rondes d'un tournoi (Création/Visualisation)");
            System.out.println("0) Retour");
            rep = ConsoleFdB.entreeEntier("Choix : ");
            
            if (rep == 1) {
                Tournoi.tousLesTournois(con).forEach(System.out::println);
            } else if (rep == 2) {
                String nom = ConsoleFdB.entreeString("Nom du tournoi : ");
                // Pour simplifier en console, on met des dates nulles ou fictives
                Tournoi t = new Tournoi(nom, null, null); 
                t.saveInDB(con);
                System.out.println("Tournoi créé : " + t);
            } else if (rep == 3) {
                List<Tournoi> ts = Tournoi.tousLesTournois(con);
                Optional<Tournoi> t = ListUtils.selectOneOrCancel("Choisir un tournoi :", ts, Tournoi::getNom);
                if (t.isPresent()) {
                    menuRondesDuTournoi(con, t.get());
                }
            }
        }
    }
    
    private static void menuRondesDuTournoi(Connection con, Tournoi t) throws SQLException {
        System.out.println("\n--- RONDES DU TOURNOI : " + t.getNom() + " ---");
        System.out.println("1) Lister les rondes existantes");
        System.out.println("2) CRÉER UNE NOUVELLE RONDE (Génération automatique) [cite: 44, 46]");
        int r = ConsoleFdB.entreeEntier("Choix : ");
        
        if (r == 1) {
             try (PreparedStatement pst = con.prepareStatement("SELECT * FROM ronde WHERE ID_TOURNOI = ? ORDER BY NUMERO")) {
                 pst.setInt(1, t.getId());
                 try (ResultSet rs = pst.executeQuery()) {
                     boolean found = false;
                     while(rs.next()) {
                         found = true;
                         System.out.println("Ronde N°" + rs.getInt("NUMERO") + " - Etat : " + rs.getString("ETAT"));
                     }
                     if (!found) System.out.println("Aucune ronde créée.");
                 }
             }
        } else if (r == 2) {
            int num = ConsoleFdB.entreeInt("Numéro de la nouvelle ronde : ");
            Ronde ronde = new Ronde(num, t.getId());
            ronde.saveInDB(con); 
            
            // --- REMPLACEZ L'APPEL ICI ---
            ServiceTournoi.creerRondeAleatoire(con, ronde.getId());
            // -----------------------------
        }
    }


    // =========================================================================
    // 4. RESULTATS & MATCHS
    // =========================================================================

    private static void menuResultats(Connection con, boolean isAdmin) throws SQLException {
        int rep = -1;
        while (rep != 0) {
            System.out.println("\n--- MATCHS & RÉSULTATS ---");
            System.out.println("1) Voir les matchs en cours [cite: 48]");
            if (isAdmin) System.out.println("2) Saisir le score d'un match (Admin) [cite: 49, 50]");
            System.out.println("0) Retour");
            rep = ConsoleFdB.entreeEntier("Choix : ");
            
            if (rep == 1) {
                List<Matchs> ouverts = Matchs.matchsOuverts(con);
                if (ouverts.isEmpty()) System.out.println("Aucun match en cours.");
                else ouverts.forEach(System.out::println);
            } 
            else if (isAdmin && rep == 2) {
                List<Matchs> matchs = Matchs.matchsOuverts(con);
                Optional<Matchs> mOpt = ListUtils.selectOneOrCancel("Sélectionnez le match à noter :", matchs, Matchs::toString);
                
                if (mOpt.isPresent()) {
                    Matchs m = mOpt.get();
                    List<Equipe> eqs = Equipe.equipesPourMatch(con, m.getId());
                    
                    if (eqs.size() < 2) {
                        System.out.println("Erreur : Ce match n'a pas d'équipes valides.");
                        continue;
                    }

                    // Saisie des scores pour chaque équipe
                    for (Equipe e : eqs) {
                        System.out.print("Score pour Equipe " + e.getNum() + " (Joueurs: ");
                        // Affichage des joueurs de l'équipe pour aider l'admin
                        List<Joueur> membres = Joueur.joueursPourEquipe(con, e.getId());
                        membres.forEach(j -> System.out.print(j.getSurnom() + " "));
                        System.out.print(") : ");
                        
                        int sc = ConsoleFdB.entreeInt("");
                        e.setScore(sc);
                        e.updateInDB(con);
                    }
                    
                    // Clôture du match [cite: 51]
                    m.setStatut("clos");
                    m.updateInDB(con);
                    System.out.println("✅ Match clos et scores enregistrés.");
                    
                    // Mise à jour immédiate du classement général [cite: 16]
                    Joueur.mettreAJourClassementGeneral(con);
                    
                    // Vérification fin de ronde [cite: 52]
                    if (Ronde.tousMatchsRondeClos(con, m.getIdRonde())) {
                        new Ronde(m.getIdRonde(), 0, 0, "").setEtat(con, "close");
                        System.out.println("🏁 TOUS LES MATCHS SONT TERMINÉS : LA RONDE EST CLÔTURÉE !");
                    }
                }
            }
        }
    }

    // =========================================================================
    // 5. PARAMÈTRES & OUTILS DIVERS
    // =========================================================================
    
    private static void menuParametres(Connection con) throws SQLException {
        Parametres p = Parametres.load(con);
        System.out.println("\n--- CONFIGURATION GLOBALE DU TOURNOI --- [cite: 24, 27]");
        System.out.println("Nombre de terrains utilisables simultanément : " + p.getNbTerrains());
        System.out.println("Nombre de joueurs par équipe                 : " + p.getNbJoueursParEquipe());
        
        System.out.println("1) Modifier");
        System.out.println("0) Retour");
        if (ConsoleFdB.entreeEntier("Choix : ") == 1) {
            int nbT = ConsoleFdB.entreeInt("Nouveau nb terrains : ");
            int nbJ = ConsoleFdB.entreeInt("Nouveau nb joueurs/équipe : ");
            p.setNbTerrains(nbT);
            p.setNbJoueursParEquipe(nbJ);
            p.save(con);
            System.out.println("Paramètres sauvegardés.");
        }
    }
    
    private static void menuTerrains(Connection con) throws SQLException {
        System.out.println("\n--- GESTION TERRAINS ---");
        System.out.println("1) Lister");
        System.out.println("2) Ajouter");
        int c = ConsoleFdB.entreeEntier("Choix : ");
        if (c == 2) {
            String n = ConsoleFdB.entreeString("Nom (ex: Court A) : ");
            String t = ConsoleFdB.entreeString("Type (C=Couvert / O=Ouvert) : ");
            new Terrain(n, t).saveInDB(con);
            System.out.println("Terrain ajouté.");
        } else if (c == 1) {
            Terrain.tousLesTerrains(con).forEach(System.out::println);
        }
    }

    private static void menuOutilsBdd(Connection con) throws SQLException {
        System.out.println("\n--- OUTILS BDD ---");
        System.out.println("1) RAZ BDD (Attention : Efface tout !)");
        System.out.println("2) Import CSV");
        System.out.println("3) Export CSV");
        int c = ConsoleFdB.entreeEntier("Choix : ");
        
        if (c == 1) {
            String confirm = ConsoleFdB.entreeString("Tapez 'OUI' pour confirmer l'effacement complet : ");
            if ("OUI".equals(confirm)) {
                GestionBdD.razBdd(con);
                // Optionnel : On peut relancer le script de test pour remettre des données
                // BdDTestTournoi.createBdDTestTournoi(con); 
                System.out.println("Base de données réinitialisée.");
            }
        } else if (c == 2) {
            String t = ConsoleFdB.entreeString("Nom de la table (ex: joueur) : ");
            String f = ConsoleFdB.entreeString("Chemin du fichier CSV : ");
            GestionBdD.importCSV(con, t, f);
        } else if (c == 3) {
            String t = ConsoleFdB.entreeString("Nom de la table à exporter : ");
            GestionBdD.exportCSV(con, t);
        }
    }

    // =========================================================================
    // 6. POINT D'ENTRÉE PRINCIPAL (MAIN)
    // =========================================================================
    public static void main(String[] args) {
        // Lance l'application
        menuPrincipal();
    }

}