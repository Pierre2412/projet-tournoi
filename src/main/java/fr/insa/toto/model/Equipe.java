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

public class Equipe extends ClasseMiroir {

    private int num;
    private int score;
    private int idMatch;

    public Equipe(int num, int score, int idMatch) {
        super();
        this.num = num;
        this.score = score;
        this.idMatch = idMatch;
    }

    public Equipe(int id, int num, int score, int idMatch) {
        super(id);
        this.num = num;
        this.score = score;
        this.idMatch = idMatch;
    }

    @Override
    public String toString() {
        return "Equipe " + num + " (Score: " + score + ")";
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

    public void updateInDB(Connection con) throws SQLException {
        if (this.getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
        try (PreparedStatement update = con.prepareStatement(
                "UPDATE equipe SET NUM = ?, SCORE = ?, IDMATCH = ? WHERE ID = ?")) {
            update.setInt(1, this.getNum());
            update.setInt(2, this.getScore());
            update.setInt(3, this.getIdMatch());
            update.setInt(4, this.getId());
            update.executeUpdate();
        }
    }
    
    public void ajouterJoueur(Connection con, int idJoueur) throws SQLException {
        if (this.getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
        new Composition(this.getId(), idJoueur).save(con);
    }

    public static List<Equipe> tousLesEquipes(Connection con) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, NUM, SCORE, IDMATCH FROM equipe")) {
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) res.add(new Equipe(rs.getInt("ID"), rs.getInt("NUM"), rs.getInt("SCORE"), rs.getInt("IDMATCH")));
            }
        }
        return res;
    }

    public static List<Equipe> equipesPourMatch(Connection con, int idMatch) throws SQLException {
        List<Equipe> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, NUM, SCORE, IDMATCH FROM equipe WHERE IDMATCH = ?")) {
            pst.setInt(1, idMatch);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Equipe(rs.getInt("ID"), rs.getInt("NUM"), rs.getInt("SCORE"), rs.getInt("IDMATCH")));
                }
            }
        }
        return res;
    }

    // Méthode corrigée pour récupérer les joueurs avec le nouveau constructeur (Total Score)
    public List<Joueur> getJoueurs(Connection con) throws SQLException {
        if (this.getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
        List<Joueur> res = new ArrayList<>();
        String sql = "SELECT j.ID, j.SURNOM, j.CATEGORIE, j.TAILLECM, j.ROLE, j.TOTAL_SCORE " +
                     "FROM joueur j JOIN composition c ON j.ID = c.IDJOUEUR WHERE c.IDEQUIPE = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, this.getId());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Joueur(  
                            rs.getInt("ID"),
                            rs.getString("SURNOM"),
                            rs.getString("CATEGORIE"),
                            rs.getObject("TAILLECM") != null ? rs.getInt("TAILLECM") : null,
                            rs.getString("ROLE"),
                            rs.getObject("TOTAL_SCORE") != null ? rs.getInt("TOTAL_SCORE") : 0 // Gestion du null
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