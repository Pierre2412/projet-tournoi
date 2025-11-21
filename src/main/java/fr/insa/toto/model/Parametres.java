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

/**
 * Gestion des paramètres globaux du tournoi (Singleton base de données).
 * Répond aux exigences : nb terrains et nb joueurs par équipe globaux.
 */
public class Parametres {
    
    private int nbTerrains;
    private int nbJoueursParEquipe;

    public Parametres(int nbTerrains, int nbJoueursParEquipe) {
        this.nbTerrains = nbTerrains;
        this.nbJoueursParEquipe = nbJoueursParEquipe;
    }

    // Charge l'unique ligne de configuration depuis la BDD
    public static Parametres load(Connection con) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("SELECT nb_terrains, nb_joueurs_par_equipe FROM parametres LIMIT 1")) {
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new Parametres(rs.getInt("nb_terrains"), rs.getInt("nb_joueurs_par_equipe"));
            }
            // Valeurs par défaut si table vide
            return new Parametres(2, 2);
        }
    }

    // Met à jour la configuration
    public void save(Connection con) throws SQLException {
        // On supprime tout puis on réinsère pour garantir une seule ligne
        try (PreparedStatement del = con.prepareStatement("DELETE FROM parametres");
             PreparedStatement ins = con.prepareStatement("INSERT INTO parametres (nb_terrains, nb_joueurs_par_equipe) VALUES (?, ?)")) {
            del.executeUpdate();
            ins.setInt(1, this.nbTerrains);
            ins.setInt(2, this.nbJoueursParEquipe);
            ins.executeUpdate();
        }
    }

    public int getNbTerrains() { return nbTerrains; }
    public void setNbTerrains(int nbTerrains) { this.nbTerrains = nbTerrains; }
    public int getNbJoueursParEquipe() { return nbJoueursParEquipe; }
    public void setNbJoueursParEquipe(int nbJoueursParEquipe) { this.nbJoueursParEquipe = nbJoueursParEquipe; }
}
