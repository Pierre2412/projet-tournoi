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

public class Matchs extends ClasseMiroir {

    private int idRonde;
    private int idTerrain;
    private String statut; // 'en_cours' ou 'clos'

    public Matchs(int idRonde, int idTerrain) {
        super();
        this.idRonde = idRonde;
        this.idTerrain = idTerrain;
        this.statut = "en_cours";
    }

    public Matchs(int id, int idRonde, int idTerrain, String statut) {
        super(id);
        this.idRonde = idRonde;
        this.idTerrain = idTerrain;
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Match " + getId() + " [Ronde=" + idRonde + ", Terrain=" + idTerrain + ", Statut=" + statut + "]";
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO matchs (ID_RONDE, ID_TERRAIN, STATUT) VALUES (?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setInt(1, idRonde);
        insert.setInt(2, idTerrain);
        insert.setString(3, this.statut);
        insert.executeUpdate();
        return insert;
    }

    public void updateInDB(Connection con) throws SQLException {
        if (this.getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
        try (PreparedStatement update = con.prepareStatement(
                "UPDATE matchs SET ID_RONDE = ?, ID_TERRAIN = ?, STATUT = ? WHERE ID = ?")) {
            update.setInt(1, this.idRonde);
            update.setInt(2, this.idTerrain);
            update.setString(3, this.statut);
            update.setInt(4, this.getId());
            update.executeUpdate();
        }
    }

    public static List<Matchs> matchsOuverts(Connection con) throws SQLException {
        List<Matchs> res = new ArrayList<>();
        // On récupère les matchs explicitement "en_cours"
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, ID_RONDE, ID_TERRAIN, STATUT FROM matchs WHERE STATUT = 'en_cours'")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Matchs(rs.getInt("ID"), rs.getInt("ID_RONDE"), rs.getInt("ID_TERRAIN"), rs.getString("STATUT")));
                }
            }
        }
        return res;
    }

    // Getters/Setters
    public int getIdRonde() { return idRonde; }
    public int getIdTerrain() { return idTerrain; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
