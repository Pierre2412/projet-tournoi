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

public class Ronde extends ClasseMiroir {

    private int numero;
    private int idTournoi;
    private String etat;

    public Ronde(int numero, int idTournoi) {
        super();
        this.numero = numero;
        this.idTournoi = idTournoi;
        this.etat = "ouverte";
    }

    public Ronde(int id, int numero, int idTournoi, String etat) {
        super(id);
        this.numero = numero;
        this.idTournoi = idTournoi;
        this.etat = etat;
    }

    @Override
    public String toString() {
        return "Ronde " + numero + " (Tournoi " + idTournoi + ") - Etat: " + etat;
    }

    @Override
    public Statement saveSansId(Connection con) throws SQLException {
        PreparedStatement insert = con.prepareStatement(
                "INSERT INTO ronde (NUMERO, ID_TOURNOI, ETAT) VALUES (?, ?, 'ouverte')",
                PreparedStatement.RETURN_GENERATED_KEYS);
        insert.setInt(1, numero);
        insert.setInt(2, idTournoi);
        insert.executeUpdate();
        return insert;
    }
    
    public void setEtat(Connection con, String newEtat) throws SQLException {
        if (getId() == -1) throw new ClasseMiroir.EntiteNonSauvegardee();
        try (PreparedStatement pst = con.prepareStatement("UPDATE ronde SET ETAT = ? WHERE ID = ?")) {
            pst.setString(1, newEtat);
            pst.setInt(2, getId());
            pst.executeUpdate();
            this.etat = newEtat;
        }
    }

    public static List<Ronde> rondesOuvertes(Connection con) throws SQLException {
        List<Ronde> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, NUMERO, ID_TOURNOI, ETAT FROM ronde WHERE ETAT IN ('ouverte', 'en_cours')")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Ronde(rs.getInt("ID"), rs.getInt("NUMERO"), rs.getInt("ID_TOURNOI"), rs.getString("ETAT")));
                }
            }
        }
        return res;
    }
    
    public static List<Ronde> toutesLesRondes(Connection con) throws SQLException {
        List<Ronde> res = new ArrayList<>();
        try (PreparedStatement pst = con.prepareStatement("SELECT ID, NUMERO, ID_TOURNOI, ETAT FROM ronde")) {
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    res.add(new Ronde(rs.getInt("ID"), rs.getInt("NUMERO"), rs.getInt("ID_TOURNOI"), rs.getString("ETAT")));
                }
            }
        }
        return res;
    }

    // CORRECTION MAJEURE : Vérifie si tous les matchs ont le statut 'clos'
    public static boolean tousMatchsRondeClos(Connection con, int idRonde) throws SQLException {
        // Compte les matchs qui ne sont PAS clos
        String sql = "SELECT COUNT(*) FROM matchs WHERE ID_RONDE = ? AND STATUT != 'clos'";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idRonde);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int nonClos = rs.getInt(1);
                    return nonClos == 0; // Si 0 match non clos, alors tout est clos.
                }
            }
        }
        return false;
    }

    // Getters
    public int getNumero() { return numero; }
    public int getIdTournoi() { return idTournoi; }
    public String getEtat() { return etat; }
}