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

/**
 * Script de peuplement de la BDD pour les tests.
 * VERSION NETTOYÉE : Ne crée que les joueurs.
 * Exécutez ce fichier en "Run As Java Application" pour réinitialiser la base.
 */
public class BdDTestTournoi {

    public static void createBdDTestTournoi(Connection con) throws SQLException {
        System.out.println("--- Création des données de test (Joueurs uniquement) ---");

        // 1. Création des Joueurs (Indispensable pour avoir les IDs 1 à 5)
        // ID 1 : Toto (Admin) - Indispensable pour se connecter en Admin
        Joueur j1 = new Joueur("Toto", "S", 180);
        j1.setRole("A"); // Admin
        j1.saveInDB(con);

        // ID 2 à 5 : Autres joueurs (Utilisateurs standard)
        Joueur j2 = new Joueur("Titi", "J", 160); j2.saveInDB(con);
        Joueur j3 = new Joueur("Tutu", "S", 175); j3.saveInDB(con);
        Joueur j4 = new Joueur("Tata", "J", 165); j4.saveInDB(con);
        Joueur j5 = new Joueur("Tete", "S", 190); j5.saveInDB(con);
        
        System.out.println("✅ 5 Joueurs créés (Toto, Titi, Tutu, Tata, Tete).");
        System.out.println("ℹ️ Aucune autre donnée (Tournoi, Terrain, Match) n'a été générée.");
    }

    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            // 1. On efface et recrée le schéma (Tables vides, IDs remis à 1)
            GestionBdD.razBdd(con);
            
            // 2. On remplit avec les données (Joueurs uniquement)
            createBdDTestTournoi(con);
            
            System.out.println("--- BASE DE DONNÉES INITIALISÉE AVEC SUCCÈS ---");
            
            // Test affichage pour confirmation
            System.out.println("Liste des joueurs en base :");
            for (Joueur j : Joueur.tousLesJoueurs(con)) {
                System.out.println(" - " + j);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new Error(ex);
        }
    }
}