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
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Classe de service contenant la logique métier complexe.
 * Utilisable par la Console ET par l'interface Vaadin.
 */
public class ServiceTournoi {

    /**
     * Algorithme de création de ronde respectant les contraintes du PDF.
     * Utilise les Parametres globaux (nb terrains, nb joueurs/équipe).
     */
    public static void creerRondeAleatoire(Connection con, int idRonde) throws SQLException {
        // 1. Charger les paramètres globaux
        Parametres params = Parametres.load(con);
        int nbTerrainsDispo = params.getNbTerrains();
        int joueursParEquipe = params.getNbJoueursParEquipe();
        int joueursParMatch = joueursParEquipe * 2; // Match 2 équipes

        // 2. Récupérer et mélanger les joueurs
        List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
        Collections.shuffle(joueurs); // Aléatoire

        if (joueurs.size() < joueursParMatch) {
            System.out.println("ServiceInfo : Pas assez de joueurs inscrits pour faire un seul match !");
            return;
        }

        // 3. Récupérer les terrains physiques
        List<Terrain> terrains = Terrain.tousLesTerrains(con);
        if (terrains.isEmpty()) {
            System.out.println("ServiceInfo : Aucun terrain n'existe en base.");
            return;
        }

        // 4. Calculer le nombre de matchs possibles
        int maxMatchsJoueurs = joueurs.size() / joueursParMatch;
        int nbMatchsAFaire = Math.min(nbTerrainsDispo, maxMatchsJoueurs);

        int indexJoueur = 0;
        for (int i = 0; i < nbMatchsAFaire; i++) {
            // Assignation terrain (Cyclique)
            Terrain t = terrains.get(i % terrains.size());
            
            // Création Match
            Matchs m = new Matchs(idRonde, t.getId());
            m.setStatut("en_cours");
            m.saveInDB(con);
            
            // Création Equipe 1 + Compo
            creerEquipeComplete(con, m.getId(), 1, joueurs, indexJoueur, joueursParEquipe);
            indexJoueur += joueursParEquipe;
            
            // Création Equipe 2 + Compo
            creerEquipeComplete(con, m.getId(), 2, joueurs, indexJoueur, joueursParEquipe);
            indexJoueur += joueursParEquipe;
        }
        
        // 5. Mise à jour état ronde
        new Ronde(idRonde, 0, 0, "").setEtat(con, "en_cours");
        System.out.println("ServiceInfo : Ronde " + idRonde + " générée (" + nbMatchsAFaire + " matchs).");
    }

    
    public static void creerMatchManuel(Connection con, int idRonde, int idTerrain, List<Joueur> joueursEq1, List<Joueur> joueursEq2) throws SQLException {
        // 1. Création du Match
        Matchs m = new Matchs(idRonde, idTerrain);
        m.setStatut("en_cours");
        m.saveInDB(con);

        // 2. Création Equipe 1
        Equipe e1 = new Equipe(1, 0, m.getId());
        e1.saveInDB(con);
        for (Joueur j : joueursEq1) {
            new Composition(e1.getId(), j.getId()).save(con);
        }

        // 3. Création Equipe 2
        Equipe e2 = new Equipe(2, 0, m.getId());
        e2.saveInDB(con);
        for (Joueur j : joueursEq2) {
            new Composition(e2.getId(), j.getId()).save(con);
        }

        System.out.println("Match manuel créé (ID " + m.getId() + ")");
        
        // Met à jour l'état de la ronde si ce n'est pas déjà fait
        new Ronde(idRonde, 0, 0, "").setEtat(con, "en_cours");
    }
    
    
    // Helper privé (car utilisé uniquement par la méthode ci-dessus)
    private static void creerEquipeComplete(Connection con, int idMatch, int numEq, List<Joueur> pool, int startIdx, int count) throws SQLException {
        Equipe e = new Equipe(numEq, 0, idMatch); // Score 0 initial
        e.saveInDB(con);
        for (int i = 0; i < count; i++) {
            Joueur j = pool.get(startIdx + i);
            new Composition(e.getId(), j.getId()).save(con);
        }
    }
}
