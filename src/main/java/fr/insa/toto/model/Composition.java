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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Composition {

    private int idEquipe;
    private int idJoueur;

    public Composition(int idEquipe, int idJoueur) {
        this.idEquipe = idEquipe;
        this.idJoueur = idJoueur;
    }

    // Permet de sauvegarder directement à la création
    public Composition(int idEquipe, int idJoueur, Connection con) throws SQLException {
        this(idEquipe, idJoueur);
        this.save(con);
    }

    public void save(Connection con) throws SQLException {
        // Utilise INSERT IGNORE pour éviter les doublons si on ré-exécute par erreur
        try (PreparedStatement pst = con.prepareStatement(
                "INSERT INTO composition (IDEQUIPE, IDJOUEUR) VALUES (?, ?)")) {
            pst.setInt(1, this.idEquipe);
            pst.setInt(2, this.idJoueur);
            try {
                pst.executeUpdate();
            } catch (SQLException ex) {
                // Si c'est une erreur de duplication (déjà existant), on ignore
                if (!ex.getMessage().contains("Duplicate")) throw ex;
            }
        }
    }

    // Getters
    public int getIdEquipe() { return idEquipe; }
    public int getIdJoueur() { return idJoueur; }
}