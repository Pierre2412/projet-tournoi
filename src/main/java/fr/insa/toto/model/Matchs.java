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
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe miroir pour la table matchs.
 * Inspirée de Loisir du projet fourni.
 */
public class Matchs extends ClasseMiroir {

    private int ronde;  // NOT NULL

    public Matchs(int ronde) {
        super();
        this.ronde = ronde;
    }

    public Matchs(int id, int ronde) {
        super(id);
        this.ronde = ronde;
    }
    
    public static List<Matchs> tousLesMatchs(Connection con) throws SQLException {
        List<Matchs> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, RONDE FROM matchs")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Matchs(
                            rs.getInt("ID"),
                            rs.getInt("RONDE")
                    ));
                }
            }
        }
        return res;
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO matchs (RONDE) VALUES (?)",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setInt(1, this.getRonde());
        insert.executeUpdate();
        return insert;
    }
    
    @Override
    public String toString() {
        return "Match " + getId() + " (Ronde " + ronde + ")";
    }

    // Getters/Setters
    public int getRonde() { return ronde; }
    public void setRonde(int ronde) { this.ronde = ronde; }
}