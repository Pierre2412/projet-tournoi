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

import fr.insa.beuvron.utils.database.ClasseMiroir;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import fr.insa.toto.model.Joueur;

/**
 * Classe miroir pour la table equipe.
 * Inspirée de Loisir, avec FK idMatch.
 */
public class Equipe extends ClasseMiroir {

    private int num;        // NOT NULL
    private int score;      // NOT NULL
    private int idMatch;    // NOT NULL, FK

    // Constructeur pour nouvel objet (ID = -1)
    public Equipe(int num, int score, int idMatch) {
        super();
        this.num = num;
        this.score = score;
        this.idMatch = idMatch;
    }

    // Constructeur pour load depuis BDD (avec ID)
    public Equipe(int id, int num, int score, int idMatch) {
        super(id);
        this.num = num;
        this.score = score;
        this.idMatch = idMatch;
    }

    @Override
    public String toString() {
        return "Equipe{id=" + this.getId() + ", num=" + num + ", score=" + score + ", idMatch=" + idMatch + '}';
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO equipe (NUM, SCORE, IDMATCH) VALUES (?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setInt(1, this.getNum());
        insert.setInt(2, this.getScore());
        insert.setInt(3, this.getIdMatch());
        insert.executeUpdate();
        return insert;
    }

    // Méthode statique exemple : Toutes les équipes
    public static List<Equipe> tousLesEquipes(Connection con) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, NUM, SCORE, IDMATCH FROM equipe")) {
            try (ResultSet allE = pst.executeQuery()) {
                return fromResultSetToList(allE);
            }
        }
    }
    
    // Équipes avec au moins un junior (q20)
public static List<Integer> equipesAvecJunior(Connection con) throws SQLException {
    List<Integer> res = new ArrayList<>();
    try (PreparedStatement pst = con.prepareStatement(
            "SELECT DISTINCT e.ID FROM equipe e JOIN composition c ON e.ID = c.IDEQUIPE " +
            "JOIN joueur j ON c.IDJOUEUR = j.ID WHERE j.CATEGORIE = 'J'")) {
        try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                res.add(rs.getInt("ID"));
            }
        }
    }
    return res;
}

// Ajouter joueur à équipe (insert composition)
public void ajouterJoueur(Connection con, int idJoueur) throws SQLException {
    if (this.getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
    try (PreparedStatement pst = con.prepareStatement("INSERT INTO composition (IDEQUIPE, IDJOUEUR) VALUES (?, ?)")) {
        pst.setInt(1, this.getId());
        pst.setInt(2, idJoueur);
        pst.executeUpdate();
        System.out.println("Joueur " + idJoueur + " ajouté à équipe " + this.getId());
    }
}

// Stats équipe : Nb catégories distinctes non null (q21)
public int nbCategoriesDistinctes(Connection con) throws SQLException {
    if (this.getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
    try (PreparedStatement pst = con.prepareStatement(
            "SELECT COUNT(DISTINCT j.CATEGORIE) FROM composition c JOIN joueur j ON c.IDJOUEUR = j.ID " +
            "WHERE c.IDEQUIPE = ? AND j.CATEGORIE IS NOT NULL")) {
        pst.setInt(1, this.getId());
        try (ResultSet rs = pst.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }
}

    /**
     * Suppose que le resultset contient bien une table d'équipes.
     */
    private static List<Equipe> fromResultSetToList(ResultSet equipes) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        while (equipes.next()) {
            res.add(new Equipe(
                    equipes.getInt("ID"),
                    equipes.getInt("NUM"),
                    equipes.getInt("SCORE"),
                    equipes.getInt("IDMATCH")
            ));
        }
        return res;
    }

    // Delete exemple (gère dépendances si besoin, ex. composition)
    public void deleteInDB(Connection con) throws SQLException {
        if (this.getId() == -1) {
            throw new ClasseMiroir.EntiteNonSauvegardee();
        }
        try {
            con.setAutoCommit(false);
            // Supprime dépendances (ex. composition)
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM composition WHERE IDEQUIPE = ?")) {
                pst.setInt(1, this.getId());
                pst.executeUpdate();
            }
            // Supprime l'équipe
            try (PreparedStatement pst = con.prepareStatement("DELETE FROM equipe WHERE ID = ?")) {
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
    
    public List<Joueur> getJoueurs(Connection con) throws SQLException {
        if (this.getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
        List<Joueur> res = new ArrayList<>();
        // Jointure entre composition et joueur
        String sql = "SELECT j.ID, j.SURNOM, j.CATEGORIE, j.TAILLECM " +
                     "FROM joueur j " +
                     "JOIN composition c ON j.ID = c.IDJOUEUR " +
                     "WHERE c.IDEQUIPE = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, this.getId());
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

    // Getters/Setters
    public int getNum() { return num; }
    public void setNum(int num) { this.num = num; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getIdMatch() { return idMatch; }
    public void setIdMatch(int idMatch) { this.idMatch = idMatch; }
}