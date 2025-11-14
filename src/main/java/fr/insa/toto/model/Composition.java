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

import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour la table composition (clé composite, pas d'ID auto).
 * Inspirée de la gestion des tables de liaison dans BdDTest (pratique/apprecie).
 */
public class Composition {

    private int idEquipe;   // FK
    private int idJoueur;   // FK

    public Composition(int idEquipe, int idJoueur) {
        this.idEquipe = idEquipe;
        this.idJoueur = idJoueur;
    }

    public Composition(int idEquipe, int idJoueur, Connection con) throws SQLException {
        this(idEquipe, idJoueur);
        this.save(con);  // Save immédiat si besoin
    }

    // Save : Insert direct (pas d'ID auto)
    public void save(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement(
                "INSERT INTO composition (IDEQUIPE, IDJOUEUR) VALUES (?, ?)")) {
            pst.setInt(1, this.getIdEquipe());
            pst.setInt(2, this.getIdJoueur());
            pst.executeUpdate();
        }
    }

    // Méthode statique : Load pour une équipe (retourne liste des ID joueurs)
    public static List<Integer> loadIdJoueursPourEquipe(Connection con, int idEquipe) throws SQLException {
        List<Integer> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT IDJOUEUR FROM composition WHERE IDEQUIPE = ?")) {
            pst.setInt(1, idEquipe);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(rs.getInt("IDJOUEUR"));
                }
            }
        }
        return res;
    }

    // Delete pour une équipe (supprime toutes compo)
    public static void deleteForEquipe(Connection con, int idEquipe) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("DELETE FROM composition WHERE IDEQUIPE = ?")) {
            pst.setInt(1, idEquipe);
            pst.executeUpdate();
        }
    }

    // Getters
    public int getIdEquipe() { return idEquipe; }
    public int getIdJoueur() { return idJoueur; }
}