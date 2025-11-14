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
import java.sql.SQLException;
import java.util.List;

/**
 * Test pour peupler la BDD tournoi (inspiré de BdDTest).
 * Exécute après creeSchema.
 */
public class BdDTestTournoi {

    public static void createBdDTestTournoi(Connection con) throws SQLException {
        // q1: Joueurs
        List<Joueur> joueurs = List.of(
                new Joueur("Toto", "S", 180),
                new Joueur("Titi", "J", 160),
                new Joueur("Tutu", null, null),
                new Joueur("Toti", null, 170),
                new Joueur("Tuti", "J", 190)
        );
        for (Joueur j : joueurs) {
            j.saveInDB(con);
        }

        // q2: Matchs
        List<Matchs> matchs = List.of(
                new Matchs(1),
                new Matchs(1)
        );
        for (Matchs m : matchs) {
            m.saveInDB(con);
        }

        // q3: Equipes (utilise IDs matchs : 1 et 2)
        List<Equipe> equipes = List.of(
                new Equipe(1, 10, 1),  // ID match 1
                new Equipe(2, 15, 1),
                new Equipe(1, 12, 2),  // ID match 2
                new Equipe(2, 5, 2)
        );
        for (Equipe e : equipes) {
            e.saveInDB(con);
        }

        // q4: Composition (utilise IDs : joueurs 1-5, equipes 1-4)
        int[][] compo = {
                {1, 1}, {1, 2},  // Equipe 1 : Joueurs 1,2
                {2, 3}, {2, 4},  // Equipe 2 : 3,4
                {3, 5}, {3, 3},  // Equipe 3 : 5,3
                {4, 4}, {4, 2}   // Equipe 4 : 4,2
        };
        for (int[] c : compo) {
            new Composition(c[0], c[1]).save(con);
        }
    }

    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            GestionBdD.razBdd(con);  // Crée schéma avant
            createBdDTestTournoi(con);
            System.out.println("Données test tournoi créées.");
            // Test : Affiche joueurs
            for (Joueur j : Joueur.tousLesJoueurs(con)) {
                System.out.println(j);
            }
        } catch (SQLException ex) {
            throw new Error(ex);
        }
    }
}
