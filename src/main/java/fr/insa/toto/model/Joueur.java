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
import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Classe miroir pour la table joueur.
 * Inspirée de Utilisateur du projet fourni.
 */
public class Joueur extends ClasseMiroir {

    private String surnom;      // NOT NULL UNIQUE
    private String categorie;   // CHAR(1), nullable
    private Integer tailleCm;   // INT, nullable

    // Constructeur pour nouvel objet (ID = -1)
    public Joueur(String surnom, String categorie, Integer tailleCm) {
        super();
        this.surnom = surnom;
        this.categorie = categorie;
        this.tailleCm = tailleCm;
    }

    // Constructeur pour load depuis BDD (avec ID)
    public Joueur(int id, String surnom, String categorie, Integer tailleCm) {
        super(id);
        this.surnom = surnom;
        this.categorie = categorie;
        this.tailleCm = tailleCm;
    }

    @Override
    public String toString() {
        return "Joueur{id=" + this.getId() + ", surnom='" + surnom + "', categorie='" + categorie + "', tailleCm=" + tailleCm + '}';
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO joueur (SURNOM, CATEGORIE, TAILLECM) VALUES (?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setString(1, this.getSurnom());
        insert.setString(2, this.getCategorie());  // Null géré par JDBC
        if (this.getTailleCm() != null) {
            insert.setInt(3, this.getTailleCm());
        } else {
            insert.setNull(3, java.sql.Types.INTEGER);
        }
        insert.executeUpdate();
        return insert;
    }

    // Méthode pour UPDATE (gère si ID != -1)
    public void updateInDB(Connection con) throws SQLException {
        if (this.getId() == -1) {
            throw new ClasseMiroir.EntiteNonSauvegardee();
        }
        try (PreparedStatement update = con.prepareStatement(
                "UPDATE joueur SET SURNOM = ?, CATEGORIE = ?, TAILLECM = ? WHERE ID = ?")) {
            update.setString(1, this.getSurnom());
            update.setString(2, this.getCategorie());
            if (this.getTailleCm() != null) {
                update.setInt(3, this.getTailleCm());
            } else {
                update.setNull(3, java.sql.Types.INTEGER);
            }
            update.setInt(4, this.getId());
            int rows = update.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Aucune ligne mise à jour (ID " + this.getId() + " introuvable ?)");
            }
            System.out.println("Joueur ID " + this.getId() + " mis à jour (" + rows + " ligne(s)).");
        }
    }

    // Méthode statique exemple : Tous les joueurs
    public static List<Joueur> tousLesJoueurs(Connection con) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, SURNOM, CATEGORIE, TAILLECM FROM joueur")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Joueur(
                            rs.getInt("ID"),
                            rs.getString("SURNOM"),
                            rs.getString("CATEGORIE"),
                            rs.getObject("TAILLECM") != null ? rs.getInt("TAILLECM") : null
                    ));
                }
            }
        }
        return res;
    }

    // Méthode statique exemple : Find by surnom
    public static Optional<Joueur> findBySurnom(Connection con, String surnom) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, CATEGORIE, TAILLECM FROM joueur WHERE SURNOM = ?")) {
            pst.setString(1, surnom);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Joueur(
                            rs.getInt("ID"),
                            surnom,
                            rs.getString("CATEGORIE"),
                            rs.getObject("TAILLECM") != null ? rs.getInt("TAILLECM") : null
                    ));
                }
                return Optional.empty();
            }
        }
    }

    // Delete exemple (gère dépendances si besoin, ex. composition)
    public void deleteInDB(Connection con) throws SQLException {
        if (this.getId() == -1) {
            throw new ClasseMiroir.EntiteNonSauvegardee();
        }
        try {
            con.setAutoCommit(false);
            // Supprime dépendances (ex. composition)
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM composition WHERE IDJOUEUR = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            // Supprime le joueur
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM joueur WHERE ID = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            this.entiteSupprimee();
            con.commit();
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }
    
    // Recherche joueurs par taille minimale (ex. q5 >160)
public static List<Joueur> joueursPlusTaille(Connection con, int tailleMin) throws SQLException {
    List<Joueur> res = new ArrayList<>();
    try (PreparedStatement pst = con.prepareStatement(
            "SELECT ID, SURNOM, CATEGORIE, TAILLECM FROM joueur WHERE TAILLECM > ? ORDER BY TAILLECM DESC")) {
        pst.setInt(1, tailleMin);
        try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(new Joueur(rs.getInt("ID"), rs.getString("SURNOM"), rs.getString("CATEGORIE"),
                        rs.getObject("TAILLECM") != null ? rs.getInt("TAILLECM") : null));
            }
        }
    }
    return res;
}

// Moyenne tailles par catégorie (q11)
public static List<MoyenneCategorie> moyennesParCategorie(Connection con) throws SQLException {
    List<MoyenneCategorie> res = new ArrayList<>();
    try (PreparedStatement pst = con.prepareStatement(
            "SELECT CATEGORIE, AVG(TAILLECM) AS MOYTAILLE FROM joueur GROUP BY CATEGORIE")) {
        try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(new MoyenneCategorie(rs.getString("CATEGORIE"), rs.getDouble("MOYTAILLE")));
            }
        }
    }
    return res;
}

// Classe interne pour résultat (ajoute ça dans Joueur)
public static class MoyenneCategorie {
    public String categorie;
    public Double moyenne;
    public MoyenneCategorie(String cat, Double moy) { this.categorie = cat; this.moyenne = moy; }
    @Override public String toString() { return categorie + ": " + moyenne; }
}

// Validation avant insert (ex. surnom unique, taille >0)
public boolean isValid() {
    return surnom != null && !surnom.trim().isEmpty() && (tailleCm == null || tailleCm > 0);
}

// Bulk delete par catégorie (ex. supprimer tous juniors)
public static void deleteParCategorie(Connection con, String cat) throws SQLException {
    con.setAutoCommit(false);
    try {
        // Supprime compo d'abord
        try (PreparedStatement pstCompo = con.prepareStatement("DELETE FROM composition WHERE IDJOUEUR IN (SELECT ID FROM joueur WHERE CATEGORIE = ?)")) {
            pstCompo.setString(1, cat);
            pstCompo.executeUpdate();
        }
        // Supprime joueurs
        try (PreparedStatement pst = con.prepareStatement("DELETE FROM joueur WHERE CATEGORIE = ?")) {
            pst.setString(1, cat);
            int deleted = pst.executeUpdate();
            System.out.println(deleted + " joueurs supprimés (cat " + cat + ").");
        }
        con.commit();
    } catch (SQLException ex) {
        con.rollback();
        throw ex;
    } finally {
        con.setAutoCommit(true);
    }
}

    // Entrée console exemple (optionnel)
    public static Joueur entreeConsole() {
        String nom = ConsoleFdB.entreeString("Surnom : ");
        String cat = ConsoleFdB.entreeString("Catégorie (J/S ou vide pour null) : ");
        cat = cat.isEmpty() ? null : cat;
        Integer taille = ConsoleFdB.entreeInt("Taille cm (0 pour null) : ");
        taille = (taille == 0) ? null : taille;
        return new Joueur(nom, cat, taille);
    }

    // Getters/Setters
    public String getSurnom() { return surnom; }
    public void setSurnom(String surnom) { this.surnom = surnom; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public Integer getTailleCm() { return tailleCm; }
    public void setTailleCm(Integer tailleCm) { this.tailleCm = tailleCm; }
}