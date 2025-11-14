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
package fr.insa.toto.model;

import fr.insa.beuvron.utils.ConsoleFdB;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.beuvron.utils.database.ResultSetUtils; // Note: Utils au pluriel, comme dans le fourni
import fr.insa.beuvron.utils.exceptions.ExceptionsUtils;
import fr.insa.beuvron.utils.list.ListUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe pour gérer la base de données en mode console pour le projet tournoi.
 * Inspirée de MainConsole du projet fourni.
 * Utilise ResultSetUtils pour affichage, ListUtils pour sélection d'éléments,
 * et les classes miroirs (Joueur, Equipe, etc.) pour opérations CRUD.
 * Modifications ajoutées : Options pour recherches avancées (ex. par taille), delete par catégorie,
 * et intégration des nouvelles méthodes (joueursPlusTaille, deleteParCategorie, etc.).
 */
public class MainConsoleTournoi {

    public static void menuJoueurs(Connection con) {
        int rep = -1;
        while (rep != 0) {
            int i = 1;
            System.out.println("Menu joueurs");
            System.out.println("============================");
            System.out.println((i++) + ") Liste des joueurs (q1)");
            System.out.println((i++) + ") Ajouter un joueur");
            System.out.println((i++) + ") Supprimer un joueur");
            System.out.println((i++) + ") Modifier taille d'un joueur");
            System.out.println((i++) + ") Joueurs > 1.6m (q5)");
            System.out.println((i++) + ") Moyenne tailles par catégorie (q11)");
            System.out.println((i++) + ") Recherche joueurs > taille (général)");
            System.out.println((i++) + ") Delete par catégorie");
            System.out.println("0) Retour");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
            try {
                int j = 1;
                if (rep == j++) {
                    listerJoueurs(con);
                } else if (rep == j++) {
                    ajouterJoueur(con);
                } else if (rep == j++) {
                    supprimerJoueur(con);
                } else if (rep == j++) {
                    modifierTailleJoueur(con);
                } else if (rep == j++) {
                    joueursPlus160(con);
                } else if (rep == j++) {
                    moyenneTaillesParCategorie(con);
                } else if (rep == j++) {
                    rechercheJoueursPlusTaille(con);
                } else if (rep == j++) {
                    deleteParCategorie(con);
                }
            } catch (Exception ex) {
                System.out.println(ExceptionsUtils.messageEtPremiersAppelsDansPackage(ex, "fr.insa", 3));
            }
        }
    }

    public static void menuEquipes(Connection con) {
        int rep = -1;
        while (rep != 0) {
            int i = 1;
            System.out.println("Menu équipes");
            System.out.println("============================");
            System.out.println((i++) + ") Liste des équipes (q3)");
            System.out.println((i++) + ") Ajouter une équipe");
            System.out.println((i++) + ") Équipes avec au moins un junior (q20)");
            System.out.println((i++) + ") Équipes avec J et S (q22)");
            System.out.println((i++) + ") Ajouter joueur à équipe");
            System.out.println((i++) + ") Nb catégories distinctes pour une équipe (q21)");
            System.out.println("0) Retour");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
            try {
                int j = 1;
                if (rep == j++) {
                    listerEquipes(con);
                } else if (rep == j++) {
                    ajouterEquipe(con);
                } else if (rep == j++) {
                    equipesAvecJunior(con);
                } else if (rep == j++) {
                    equipesAvecJuniorEtSenior(con);
                } else if (rep == j++) {
                    ajouterJoueurAEquipe(con);
                } else if (rep == j++) {
                    nbCategoriesPourEquipe(con);
                }
            } catch (Exception ex) {
                System.out.println(ExceptionsUtils.messageEtPremiersAppelsDansPackage(ex, "fr.insa", 3));
            }
        }
    }

    public static void menuBdD(Connection con) {
        int rep = -1;
        while (rep != 0) {
            int i = 1;
            System.out.println("Menu gestion base de données");
            System.out.println("============================");
            System.out.println((i++) + ") RAZ BdD = delete + create + init données exemple");
            System.out.println((i++) + ") Ordre SQL update quelconque");
            System.out.println((i++) + ") Requête SQL query quelconque");
            System.out.println((i++) + ") Export CSV d'une table");
            System.out.println("0) Retour");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
            try {
                int j = 1;
                if (rep == j++) {
                    GestionBdD.razBdd(con);
                    BdDTestTournoi.createBdDTestTournoi(con);
                    System.out.println("BDD remise à zéro et peuplée avec données exemple (q1-q4).");
                } else if (rep == j++) {
                    String ordre = ConsoleFdB.entreeString("Ordre SQL update : ");
                    try (PreparedStatement pst = con.prepareStatement(ordre)) {
                        int rows = pst.executeUpdate();
                        System.out.println(rows + " lignes affectées.");
                    }
                } else if (rep == j++) {
                    String ordre = ConsoleFdB.entreeString("Requête SQL : ");
                    try (PreparedStatement pst = con.prepareStatement(ordre);
                         ResultSet rs = pst.executeQuery()) {
                        System.out.println(ResultSetUtils.formatResultSetAsTxt(rs));
                    }
                } else if (rep == j++) {
                    exportCSV(con);
                }
            } catch (Exception ex) {
                System.out.println(ExceptionsUtils.messageEtPremiersAppelsDansPackage(ex, "fr.insa", 3));
            }
        }
    }

    // Méthodes utilitaires pour les menus (exemples inspirés de menuUtilisateur)
    private static void listerJoueurs(Connection con) throws SQLException {
        List<Joueur> tous = Joueur.tousLesJoueurs(con);
        System.out.println(tous.size() + " joueurs trouvés :");
        System.out.println(ListUtils.enumerateList(tous, " ", 1, " : ", "\n", j -> j.getId() + " - " + j.getSurnom() + " (" + j.getCategorie() + ", " + j.getTailleCm() + "cm)"));
    }

    private static void ajouterJoueur(Connection con) throws SQLException {
        System.out.println("Nouvel joueur : ");
        Joueur j = Joueur.entreeConsole(); // Utilise la méthode statique si implémentée
        if (!j.isValid()) {
            System.out.println("Erreur : Surnom invalide ou taille <=0.");
            return;
        }
        j.saveInDB(con);
        System.out.println("Joueur ajouté avec ID : " + j.getId());
    }

    private static void supprimerJoueur(Connection con) throws SQLException {
        List<Joueur> tous = Joueur.tousLesJoueurs(con);
        List<Joueur> selected = ListUtils.selectMultiple("Sélectionnez les joueurs à supprimer : ", tous,
                j -> j.getId() + " : " + j.getSurnom());
        for (Joueur j : selected) {
            j.deleteInDB(con);
        }
        System.out.println(selected.size() + " joueur(s) supprimé(s).");
    }

    private static void modifierTailleJoueur(Connection con) throws SQLException {
        List<Joueur> tous = Joueur.tousLesJoueurs(con);
        Optional<Joueur> choisi = ListUtils.selectOneOrCancel("Choisissez un joueur à modifier : ", tous,
                j -> j.getId() + " : " + j.getSurnom());
        if (choisi.isPresent()) {
            Joueur j = choisi.get();
            Integer nouvelleTaille = ConsoleFdB.entreeInt("Nouvelle taille (0 pour null) : ");
            j.setTailleCm(nouvelleTaille == 0 ? null : nouvelleTaille);
            j.updateInDB(con); // Utilise updateInDB au lieu de saveInDB pour existant
            System.out.println("Taille mise à jour.");
        }
    }

    private static void joueursPlus160(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, SURNOM, CATEGORIE, TAILLECM FROM joueur WHERE TAILLECM > 160");
             ResultSet rs = pst.executeQuery()) {
            System.out.println("Joueurs > 1.6m (q5) :");
            System.out.println(ResultSetUtils.formatResultSetAsTxt(rs));
        }
    }

    private static void moyenneTaillesParCategorie(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("SELECT CATEGORIE, AVG(TAILLECM) AS MOYTAILLE FROM joueur GROUP BY CATEGORIE");
             ResultSet rs = pst.executeQuery()) {
            System.out.println("Moyenne tailles par catégorie (q11) :");
            System.out.println(ResultSetUtils.formatResultSetAsTxt(rs));
        }
    }

    // Nouvelle méthode : Recherche générale > taille
    private static void rechercheJoueursPlusTaille(Connection con) throws SQLException {
        int tailleMin = ConsoleFdB.entreeInt("Taille min (cm) : ");
        List<Joueur> result = Joueur.joueursPlusTaille(con, tailleMin);
        System.out.println(result.size() + " joueurs > " + tailleMin + "cm :");
        System.out.println(ListUtils.enumerateList(result, " ", 1, " : ", "\n", j -> j.toString()));
    }

    // Nouvelle méthode : Delete par catégorie
    private static void deleteParCategorie(Connection con) throws SQLException {
        String cat = ConsoleFdB.entreeString("Catégorie à supprimer (J/S/null) : ");
        Joueur.deleteParCategorie(con, cat.isEmpty() ? null : cat);
    }

    private static void listerEquipes(Connection con) throws SQLException {
        List<Equipe> toutes = Equipe.tousLesEquipes(con);
        System.out.println(toutes.size() + " équipes trouvées :");
        System.out.println(ListUtils.enumerateList(toutes, " ", 1, " : ", "\n", e -> e.getId() + " - Num " + e.getNum() + " (Score " + e.getScore() + ", Match " + e.getIdMatch() + ")"));
    }

    private static void ajouterEquipe(Connection con) throws SQLException {
        int num = ConsoleFdB.entreeInt("Num équipe : ");
        int score = ConsoleFdB.entreeInt("Score : ");
        int idMatch = ConsoleFdB.entreeInt("ID Match : ");
        Equipe e = new Equipe(num, score, idMatch);
        e.saveInDB(con);
        System.out.println("Équipe ajoutée avec ID : " + e.getId());
    }

    private static void equipesAvecJunior(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT DISTINCT e.ID FROM equipe e " +
                "JOIN composition c ON e.ID = c.IDEQUIPE " +
                "JOIN joueur j ON c.IDJOUEUR = j.ID " +
                "WHERE j.CATEGORIE = 'J'");
             ResultSet rs = pst.executeQuery()) {
            System.out.println("Équipes avec au moins un junior (q20) :");
            System.out.println(ResultSetUtils.formatResultSetAsTxt(rs));
        }
    }

    private static void equipesAvecJuniorEtSenior(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT e.ID FROM equipe e " +
                "WHERE e.ID IN (SELECT c1.IDEQUIPE FROM composition c1 JOIN joueur j1 ON c1.IDJOUEUR = j1.ID WHERE j1.CATEGORIE = 'J') " +
                "AND e.ID IN (SELECT c2.IDEQUIPE FROM composition c2 JOIN joueur j2 ON c2.IDJOUEUR = j2.ID WHERE j2.CATEGORIE = 'S')");
             ResultSet rs = pst.executeQuery()) {
            System.out.println("Équipes avec au moins un J et un S (q22) :");
            System.out.println(ResultSetUtils.formatResultSetAsTxt(rs));
        }
    }

    // Nouvelle méthode : Ajouter joueur à équipe
    private static void ajouterJoueurAEquipe(Connection con) throws SQLException {
        List<Equipe> equipes = Equipe.tousLesEquipes(con);
        Optional<Equipe> equipeChoisie = ListUtils.selectOneOrCancel("Choisissez une équipe : ", equipes,
                e -> e.getId() + " : " + e.toString());
        if (equipeChoisie.isPresent()) {
            Equipe e = equipeChoisie.get();
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
            Optional<Joueur> joueurChoisi = ListUtils.selectOneOrCancel("Choisissez un joueur à ajouter : ", joueurs,
                    j -> j.getId() + " : " + j.getSurnom());
            if (joueurChoisi.isPresent()) {
                e.ajouterJoueur(con, joueurChoisi.get().getId());
            }
        }
    }

    // Nouvelle méthode : Nb catégories pour une équipe
    private static void nbCategoriesPourEquipe(Connection con) throws SQLException {
        List<Equipe> equipes = Equipe.tousLesEquipes(con);
        Optional<Equipe> equipeChoisie = ListUtils.selectOneOrCancel("Choisissez une équipe : ", equipes,
                e -> e.getId() + " : " + e.toString());
        if (equipeChoisie.isPresent()) {
            int nb = equipeChoisie.get().nbCategoriesDistinctes(con);
            System.out.println("Équipe " + equipeChoisie.get().getId() + " : " + nb + " catégories distinctes non null.");
        }
    }

    // Nouvelle méthode : Export CSV
    private static void exportCSV(Connection con) throws SQLException {
        String tableName = ConsoleFdB.entreeString("Nom de la table à exporter (joueur/matchs/equipe/composition) : ");
        GestionBdD.exportCSV(con, tableName);
    }

    public static void menuPrincipal() {
        int rep = -1;
        Connection con = null;
        try {
            con = ConnectionSimpleSGBD.defaultCon();
            System.out.println("Connexion OK");
        } catch (SQLException ex) {
            System.out.println("Problème de connexion : " + ex.getLocalizedMessage());
            throw new Error(ex);
        }
        while (rep != 0) {
            int i = 1;
            System.out.println("Menu principal Tournoi");
            System.out.println("======================");
            System.out.println((i++) + ") Menu gestion BdD");
            System.out.println((i++) + ") Menu joueurs");
            System.out.println((i++) + ") Menu équipes");
            System.out.println((i++) + ") Requêtes complexes (TD4)");
            System.out.println("0) Fin");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
            try {
                int j = 1;
                if (rep == j++) {
                    menuBdD(con);
                } else if (rep == j++) {
                    menuJoueurs(con);
                } else if (rep == j++) {
                    menuEquipes(con);
                } else if (rep == j++) {
                    menuRequetesComplexes(con); // Option pour TD4
                }
            } catch (Exception ex) {
                System.out.println(ExceptionsUtils.messageEtPremiersAppelsDansPackage(ex, "fr.insa", 3));
            }
        }
        try {
            if (con != null) con.close();
        } catch (SQLException ex) {
            // Ignore
        }
    }

    // Menu pour TD4 (exemples de queries complexes)
    private static void menuRequetesComplexes(Connection con) {
        int rep = -1;
        while (rep != 0) {
            int i = 1;
            System.out.println("Menu requêtes complexes");
            System.out.println("============================");
            System.out.println((i++) + ") IDs joueurs équipe 1 (q14)");
            System.out.println((i++) + ") Nombre matchs Toto (q16)");
            System.out.println((i++) + ") Équipes avec nb catégories distinctes (q21)");
            System.out.println("0) Retour");
            rep = ConsoleFdB.entreeEntier("Votre choix : ");
            try {
                int j = 1;
                if (rep == j++) {
                    try (PreparedStatement pst = con.prepareStatement("SELECT IDJOUEUR FROM composition WHERE IDEQUIPE = 1");
                         ResultSet rs = pst.executeQuery()) {
                        System.out.println("IDs joueurs équipe 1 (q14) :");
                        System.out.println(ResultSetUtils.formatResultSetAsTxt(rs));
                    }
                } else if (rep == j++) {
                    try (PreparedStatement pst = con.prepareStatement(
                            "SELECT COUNT(DISTINCT e.IDMATCH) AS NBRMATCHTOTO " +
                            "FROM joueur j JOIN composition c ON j.ID = c.IDJOUEUR " +
                            "JOIN equipe e ON c.IDEQUIPE = e.ID " +
                            "WHERE j.SURNOM = 'Toto'");
                         ResultSet rs = pst.executeQuery()) {
                        System.out.println("Nombre matchs Toto (q16) :");
                        System.out.println(ResultSetUtils.formatResultSetAsTxt(rs));
                    }
                } else if (rep == j++) {
                    try (PreparedStatement pst = con.prepareStatement(
                            "SELECT e.ID, COUNT(DISTINCT j.CATEGORIE) AS NBRCAT " +
                            "FROM equipe e JOIN composition c ON e.ID = c.IDEQUIPE " +
                            "LEFT JOIN joueur j ON c.IDJOUEUR = j.ID AND j.CATEGORIE IS NOT NULL " +
                            "GROUP BY e.ID");
                         ResultSet rs = pst.executeQuery()) {
                        System.out.println("Équipes avec nb catégories distinctes (q21) :");
                        System.out.println(ResultSetUtils.formatResultSetAsTxt(rs));
                    }
                }
            } catch (Exception ex) {
                System.out.println(ExceptionsUtils.messageEtPremiersAppelsDansPackage(ex, "fr.insa", 3));
            }
        }
    }

    public static void main(String[] args) {
        menuPrincipal();
    }
}