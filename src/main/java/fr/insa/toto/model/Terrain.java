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

public class Terrain extends ClasseMiroir {

    private String nom;
    private String type;  // 'C' couvert, 'O' ouvert

    public Terrain(String nom, String type) {
        super();
        this.nom = nom;
        this.type = type;
    }

    public Terrain(int id, String nom, String type) {
        super(id);
        this.nom = nom;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Terrain " + getId() + ": " + nom + " (" + type + ")";
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO terrain (NOM, TYPE) VALUES (?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setString(1, nom);
        insert.setString(2, type);
        insert.executeUpdate();
        return insert;
    }

    public static List<Terrain> tousLesTerrains(Connection con) throws SQLException {
        List<Terrain> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, NOM, TYPE FROM terrain")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Terrain(rs.getInt("ID"), rs.getString("NOM"), rs.getString("TYPE")));
                }
            }
        }
        return res;
    }
    
    public static List<Terrain> terrainsDisponibles(Connection con) throws SQLException {
        List<Terrain> res = new ArrayList<>();
        // On sélectionne les terrains dont l'ID n'est PAS dans la liste des matchs en cours
        String sql = "SELECT * FROM terrain " +
                     "WHERE ID NOT IN (" +
                     "    SELECT ID_TERRAIN FROM matchs WHERE STATUT = 'en_cours'" +
                     ")";
        
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Terrain(rs.getInt("ID"), rs.getString("NOM"), rs.getString("TYPE")));
                }
            }
        }
        return res;
    }

    // Getters/Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
