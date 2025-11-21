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
import java.sql.Date;

public class Tournoi extends ClasseMiroir {

    private String nom;
    private Date dateDebut;
    private Date dateFin;

    public Tournoi(String nom, Date dateDebut, Date dateFin) {
        super();
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    public Tournoi(int id, String nom, Date dateDebut, Date dateFin) {
        super(id);
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    @Override
    public String toString() {
        return "Tournoi " + getId() + " : " + nom;
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO tournoi (NOM, DATE_DEBUT, DATE_FIN) VALUES (?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setString(1, nom);
        insert.setDate(2, dateDebut);
        insert.setDate(3, dateFin);
        insert.executeUpdate();
        return insert;
    }

    public static List<Tournoi> tousLesTournois(Connection con) throws SQLException {
        List<Tournoi> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, NOM, DATE_DEBUT, DATE_FIN FROM tournoi")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Tournoi(rs.getInt("ID"), rs.getString("NOM"), rs.getDate("DATE_DEBUT"), rs.getDate("DATE_FIN")));
                }
            }
        }
        return res;
    }

    // Getters/Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }
    public Date getDateFin() { return dateFin; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }
}