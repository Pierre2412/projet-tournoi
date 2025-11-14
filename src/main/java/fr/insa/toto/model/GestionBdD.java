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
import fr.insa.beuvron.utils.database.ResultSetUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
import java.util.List;
import java.io.*;

/**
 * Classe pour la gestion du schéma de la base de données du projet tournoi.
 * Inspirée de GestionSchema du projet fourni.
 */
public class GestionBdD {

    /**
     * Crée le schéma complet de la BdD (tables joueur, matchs, equipe, composition).
     * @param con la connexion à la base
     * @throws SQLException en cas d'erreur SQL
     */
    public static void creeSchema(Connection con) throws SQLException {
        try {
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {
                // Création des tables avec IF NOT EXISTS pour éviter l'erreur
                st.executeUpdate("CREATE TABLE IF NOT EXISTS joueur ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " SURNOM VARCHAR(50) NOT NULL UNIQUE,"
                        + " CATEGORIE CHAR(1),"
                        + " TAILLECM INT"
                        + ") "
                );
                st.executeUpdate("CREATE TABLE IF NOT EXISTS matchs ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " RONDE INT NOT NULL"
                        + ") "
                );
                st.executeUpdate("CREATE TABLE IF NOT EXISTS equipe ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " NUM INT NOT NULL,"
                        + " SCORE INT NOT NULL,"
                        + " IDMATCH INT NOT NULL"
                        + ") "
                );
                st.executeUpdate("CREATE TABLE IF NOT EXISTS composition ( "
                        + " IDEQUIPE INT NOT NULL,"
                        + " IDJOUEUR INT NOT NULL,"
                        + " PRIMARY KEY (IDEQUIPE, IDJOUEUR)" // Clé composite pour éviter doublons
                        + ") "
                );
                con.commit();
                // Ajout des contraintes FK (avec try-catch pour ignorer si déjà présentes)
                try {
                    st.executeUpdate("ALTER TABLE equipe "
                            + " ADD CONSTRAINT fk_equipe_idmatch "
                            + " FOREIGN KEY (IDMATCH) REFERENCES matchs(ID)"
                    );
                } catch (SQLException ex) {
                    if (!ex.getMessage().contains("Duplicate key name")) { // Ignore si contrainte existe déjà
                        throw ex;
                    }
                }
                try {
                    st.executeUpdate("ALTER TABLE composition "
                            + " ADD CONSTRAINT fk_composition_idequipe "
                            + " FOREIGN KEY (IDEQUIPE) REFERENCES equipe(ID)"
                    );
                } catch (SQLException ex) {
                    if (!ex.getMessage().contains("Duplicate key name")) {
                        throw ex;
                    }
                }
                try {
                    st.executeUpdate("ALTER TABLE composition "
                            + " ADD CONSTRAINT fk_composition_idjoueur "
                            + " FOREIGN KEY (IDJOUEUR) REFERENCES joueur(ID)"
                    );
                } catch (SQLException ex) {
                    if (!ex.getMessage().contains("Duplicate key name")) {
                        throw ex;
                    }
                }
                con.commit();
                System.out.println("Schéma créé (ou déjà existant).");
            }
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    /**
     * Supprime tout le schéma (contraintes puis tables).
     * @param con la connexion à la base
     * @throws SQLException en cas d'erreur SQL
     */
    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            // Suppression des contraintes FK (en try-catch pour ignorer si absentes)
            try {
                st.executeUpdate("ALTER TABLE composition "
                        + " DROP FOREIGN KEY fk_composition_idequipe");
            } catch (SQLException ex) {
                // Ignorer si la contrainte n'existe pas
            }
            try {
                st.executeUpdate("ALTER TABLE composition "
                        + " DROP FOREIGN KEY fk_composition_idjoueur");
            } catch (SQLException ex) {
                // Ignorer si la contrainte n'existe pas
            }
            try {
                st.executeUpdate("ALTER TABLE equipe "
                        + " DROP FOREIGN KEY fk_equipe_idmatch");
            } catch (SQLException ex) {
                // Ignorer si la contrainte n'existe pas
            }
            // Suppression des tables dans l'ordre inverse des dépendances (avec IF EXISTS pour éviter erreurs)
            st.executeUpdate("DROP TABLE IF EXISTS composition");
            st.executeUpdate("DROP TABLE IF EXISTS equipe");
            st.executeUpdate("DROP TABLE IF EXISTS matchs");
            st.executeUpdate("DROP TABLE IF EXISTS joueur");
            System.out.println("Schéma supprimé.");
        }
    }

    /**
     * RAZ de la BDD : supprime et recrée le schéma.
     * @param con la connexion à la base
     * @throws SQLException en cas d'erreur SQL
     */
    public static void razBdd(Connection con) throws SQLException {
        deleteSchema(con);
        creeSchema(con);
    }

    /**
     * Exporte une table entière en fichier CSV (header + data).
     * @param con connexion BDD
     * @param tableName nom de la table (ex. "joueur")
     * @throws SQLException erreur SQL ou fichier
     */
    public static void exportCSV(Connection con, String tableName) throws SQLException {
        try (PreparedStatement pst = con.prepareStatement("SELECT * FROM " + tableName);
             ResultSet rs = pst.executeQuery()) {
            // Convertit ResultSet en listes (header + data)
            ResultSetUtils.ResultSetAsLists data = ResultSetUtils.toLists(rs);
            
            // Crée fichier CSV
            String fileName = tableName + ".csv";
            try (PrintWriter writer = new PrintWriter(new File(fileName))) {
                // Header : Noms colonnes
                writer.println(String.join(",", data.getColumnNames()));
                
                // Data : Chaque ligne
                for (List<Object> row : data.getValues()) {
                    String line = row.stream()
                        .map(obj -> obj == null ? "NULL" : obj.toString())
                        .collect(Collectors.joining(","));
                    writer.println(line);
                }
            } catch (IOException ex) {
                throw new SQLException("Erreur fichier CSV : " + ex.getMessage(), ex);
            }
            System.out.println("Exporté : " + fileName + " (" + data.getValues().size() + " lignes).");
        }
    }

    /**
     * Méthode main pour tester (crée et supprime le schéma).
     * @param args arguments ignorés
     */
    public static void main(String[] args) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            razBdd(con);
            System.out.println("Schéma tournoi créé avec succès.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new Error("Erreur lors de la création du schéma : " + ex.getMessage());
        }
    }
}