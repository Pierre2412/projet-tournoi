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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Joueur extends ClasseMiroir {

    private String surnom;
    private String categorie;
    private Integer tailleCm;
    private String role;
    private Integer totalScore;

    public Joueur(String surnom, String categorie, Integer tailleCm) {
        super();
        this.surnom = surnom;
        this.categorie = categorie;
        this.tailleCm = tailleCm;
        this.role = "U";
        this.totalScore = 0;
    }

    public Joueur(int id, String surnom, String categorie, Integer tailleCm, String role, Integer totalScore) {
        super(id);
        this.surnom = surnom;
        this.categorie = categorie;
        this.tailleCm = tailleCm;
        this.role = role;
        this.totalScore = totalScore;
    }

    @Override
    public String toString() {
        return "Joueur " + getId() + ": " + surnom + " (" + (categorie!=null?categorie:"-") + ") - Score Total: " + totalScore;
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO joueur (SURNOM, CATEGORIE, TAILLECM, ROLE, TOTAL_SCORE) VALUES (?, ?, ?, ?, 0)",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setString(1, surnom);
        insert.setString(2, categorie);
        if (tailleCm != null) insert.setInt(3, tailleCm); else insert.setNull(3, Types.INTEGER);
        insert.setString(4, role);
        insert.executeUpdate();
        return insert;
    }

    public void updateInDB(Connection con) throws SQLException {
        if (this.getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
        try (PreparedStatement update = con.prepareStatement(
                "UPDATE joueur SET SURNOM = ?, CATEGORIE = ?, TAILLECM = ?, ROLE = ?, TOTAL_SCORE = ? WHERE ID = ?")) {
            update.setString(1, surnom);
            update.setString(2, categorie);
            if (tailleCm != null) update.setInt(3, tailleCm); else update.setNull(3, Types.INTEGER);
            update.setString(4, role);
            update.setInt(5, totalScore != null ? totalScore : 0);
            update.setInt(6, getId());
            update.executeUpdate();
        }
    }

    // NOUVEAU : Recalcule le score total de TOUS les joueurs (Somme des scores des équipes)
    public static void mettreAJourClassementGeneral(Connection con) throws SQLException {
        String sql = "UPDATE joueur j SET TOTAL_SCORE = ( " +
                     "   SELECT COALESCE(SUM(e.SCORE), 0) " +
                     "   FROM composition c " +
                     "   JOIN equipe e ON c.IDEQUIPE = e.ID " +
                     "   WHERE c.IDJOUEUR = j.ID " +
                     ")";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            int updates = pst.executeUpdate();
            System.out.println("Classement recalculé pour " + updates + " joueurs.");
        }
    }

    public static List<Joueur> tousLesJoueurs(Connection con) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT * FROM joueur ORDER BY SURNOM")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(mapRow(rs));
                }
            }
        }
        return res;
    }
    
    public static Optional<Joueur> findBySurnom(Connection con, String surnom) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("SELECT * FROM joueur WHERE SURNOM = ?")) {
            pst.setString(1, surnom);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }
    
    // Joueurs pour une équipe donnée
    public static List<Joueur> joueursPourEquipe(Connection con, int idEquipe) throws SQLException {
        List<Joueur> res = new ArrayList<>();
        String sql = "SELECT j.* FROM joueur j JOIN composition c ON j.ID = c.IDJOUEUR WHERE c.IDEQUIPE = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idEquipe);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) res.add(mapRow(rs));
            }
        }
        return res;
    }

    private static Joueur mapRow(ResultSet rs) throws SQLException {
        return new Joueur(
                rs.getInt("ID"),
                rs.getString("SURNOM"),
                rs.getString("CATEGORIE"),
                rs.getObject("TAILLECM") != null ? rs.getInt("TAILLECM") : null,
                rs.getString("ROLE"),
                rs.getInt("TOTAL_SCORE")
        );
    }

    // Console Helper
    public static Joueur entreeConsole() {
        String nom = ConsoleFdB.entreeString("Surnom : ");
        String cat = ConsoleFdB.entreeString("Catégorie (J/S...) : ");
        int taille = ConsoleFdB.entreeInt("Taille (0 si inconnue) : ");
        return new Joueur(nom, cat.isEmpty() ? null : cat, taille == 0 ? null : taille);
    }
    
    // Ajoutez ceci dans Joueur.java
    
    public void deleteInDB(Connection con) throws SQLException {
        if (this.getId() == -1) {
            throw new ClasseMiroir.EntiteNonSauvegardee();
        }
        try {
            con.setAutoCommit(false);
            // 1. Supprimer les dépendances dans la table composition
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM composition WHERE IDJOUEUR = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            // 2. Supprimer le joueur
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM joueur WHERE ID = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            this.entiteSupprimee();
            con.commit();
            System.out.println("Joueur supprimé.");
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }
    
    // Dans Joueur.java

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Joueur joueur = (Joueur) o;
        // Deux joueurs sont identiques s'ils ont le même ID
        return this.getId() == joueur.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.getId());
    }
    
    // Getters/Setters
    public String getSurnom() { return surnom; }
    public String getCategorie() { return categorie; }
    public Integer getTailleCm() { return tailleCm; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    public void setTailleCm(Integer tailleCm) { this.tailleCm = tailleCm; }
}