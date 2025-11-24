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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

public class GestionBdD {

    public static void creeSchema(Connection con) throws SQLException {
        try {
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {
                // 1. Table Paramètres (Critique pour le PDF : nb terrains global)
                st.executeUpdate("CREATE TABLE IF NOT EXISTS parametres ( "
                        + " nb_terrains INT DEFAULT 2,"
                        + " nb_joueurs_par_equipe INT DEFAULT 2"
                        + ") "
                );

                // 2. Tables de base
                // ATTENTION : Ajout de TOTAL_SCORE ici pour corriger votre erreur
                st.executeUpdate("CREATE TABLE IF NOT EXISTS joueur ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " SURNOM VARCHAR(50) NOT NULL UNIQUE,"
                        + " CATEGORIE CHAR(1),"
                        + " TAILLECM INT,"
                        + " ROLE CHAR(1) DEFAULT 'U',"
                        + " TOTAL_SCORE INT DEFAULT 0," 
                        + " MOT_DE_PASSE VARCHAR(100)"
                        + ") "
                );
                
                st.executeUpdate("CREATE TABLE IF NOT EXISTS terrain ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " NOM VARCHAR(50) NOT NULL UNIQUE,"
                        + " TYPE CHAR(1)"
                        + ") "
                );
                
                st.executeUpdate("CREATE TABLE IF NOT EXISTS tournoi ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " NOM VARCHAR(100) NOT NULL,"
                        + " DATE_DEBUT DATE,"
                        + " DATE_FIN DATE"
                        + ") "
                );
                
                st.executeUpdate("CREATE TABLE IF NOT EXISTS ronde ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " NUMERO INT NOT NULL,"
                        + " ID_TOURNOI INT NOT NULL,"
                        + " ETAT VARCHAR(10) DEFAULT 'ouverte'"
                        + ") "
                );

                // 3. Table Matchs avec STATUT (Correction bug 0-0)
                st.executeUpdate("CREATE TABLE IF NOT EXISTS matchs ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " ID_RONDE INT NOT NULL,"
                        + " ID_TERRAIN INT NOT NULL,"
                        + " STATUT VARCHAR(10) DEFAULT 'en_cours'" // 'en_cours' ou 'clos'
                        + ") "
                );

                st.executeUpdate("CREATE TABLE IF NOT EXISTS equipe ( "
                        + ConnectionSimpleSGBD.sqlForGeneratedKeys(con, "ID") + ","
                        + " NUM INT NOT NULL,"
                        + " SCORE INT NOT NULL DEFAULT 0,"
                        + " IDMATCH INT NOT NULL"
                        + ") "
                );
                
                st.executeUpdate("CREATE TABLE IF NOT EXISTS composition ( "
                        + " IDEQUIPE INT NOT NULL,"
                        + " IDJOUEUR INT NOT NULL,"
                        + " PRIMARY KEY (IDEQUIPE, IDJOUEUR)"
                        + ") "
                );

                con.commit();

                // Contraintes FK (Foreign Keys)
                // On ignore les erreurs si la contrainte existe déjà
                try { st.executeUpdate("ALTER TABLE equipe ADD CONSTRAINT fk_equipe_idmatch FOREIGN KEY (IDMATCH) REFERENCES matchs(ID)"); } catch (SQLException ex) {}
                try { st.executeUpdate("ALTER TABLE composition ADD CONSTRAINT fk_composition_idequipe FOREIGN KEY (IDEQUIPE) REFERENCES equipe(ID)"); } catch (SQLException ex) {}
                try { st.executeUpdate("ALTER TABLE composition ADD CONSTRAINT fk_composition_idjoueur FOREIGN KEY (IDJOUEUR) REFERENCES joueur(ID)"); } catch (SQLException ex) {}
                try { st.executeUpdate("ALTER TABLE ronde ADD CONSTRAINT fk_ronde_idtournoi FOREIGN KEY (ID_TOURNOI) REFERENCES tournoi(ID)"); } catch (SQLException ex) {}
                try { st.executeUpdate("ALTER TABLE matchs ADD CONSTRAINT fk_matchs_idronde FOREIGN KEY (ID_RONDE) REFERENCES ronde(ID)"); } catch (SQLException ex) {}
                try { st.executeUpdate("ALTER TABLE matchs ADD CONSTRAINT fk_matchs_idterrain FOREIGN KEY (ID_TERRAIN) REFERENCES terrain(ID)"); } catch (SQLException ex) {}
                
                con.commit();
                System.out.println("Schéma créé (Version complète avec Statut et Paramètres).");
            }
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    // RAZ BDD : Supprime tout et recrée + init params
    public static void razBdd(Connection con) throws SQLException {
        deleteSchema(con);
        creeSchema(con);
        // Initialisation des paramètres par défaut
        try (PreparedStatement pst = con.prepareStatement("INSERT INTO parametres (nb_terrains, nb_joueurs_par_equipe) VALUES (2, 2)")) {
            pst.executeUpdate();
        }
    }

    public static void deleteSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
             // Suppression des contraintes FK pour éviter les erreurs
            String[] tables = {"composition", "equipe", "matchs", "ronde", "tournoi", "joueur", "terrain", "parametres"};
            for (String t : tables) {
                try { st.executeUpdate("DROP TABLE IF EXISTS " + t + " CASCADE"); } catch (Exception e) { st.executeUpdate("DROP TABLE IF EXISTS " + t); }
            }
            System.out.println("Schéma supprimé.");
        }
    }
    
    // Méthodes CSV (inchangées mais nécessaires pour compilation)
    public static void exportCSV(Connection con, String tableName) throws SQLException {
         try (PreparedStatement pst = con.prepareStatement("SELECT * FROM " + tableName);
             ResultSet rs = pst.executeQuery()) {
            ResultSetUtils.ResultSetAsLists data = ResultSetUtils.toLists(rs);
            String fileName = tableName + ".csv";
            try (PrintWriter writer = new PrintWriter(new File(fileName))) {
                writer.println(String.join(";", data.getColumnNames()));
                for (List<Object> row : data.getValues()) {
                    String line = row.stream().map(obj -> "\"" + (obj == null ? "NULL" : obj.toString()) + "\"").collect(Collectors.joining(";"));
                    writer.println(line);
                }
            } catch (IOException ex) { throw new SQLException("Erreur fichier CSV", ex); }
            System.out.println("Exporté : " + fileName);
        }
    }

    public static void importCSV(Connection con, String tableName, String csvFile) throws SQLException {
        con.setAutoCommit(false);
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean headerSkipped = false;
            
            while ((line = reader.readLine()) != null) {
                // 1. Ignorer lignes vides et Entête
                if (line.trim().isEmpty()) continue;
                if (!headerSkipped) { headerSkipped = true; continue; }

                // 2. Nettoyage des données
                String[] values = line.split(";", -1);
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].replace("\"", "").trim();
                    if ("NULL".equals(values[i]) || values[i].isEmpty()) values[i] = null;
                }

                // 3. Logique par table
                // Note : On saute souvent l'index 0 car c'est l'ID (généré auto ou ignoré à l'update)
                
                if ("joueur".equals(tableName) && values.length >= 6) {
                    // CSV attendu : ID;SURNOM;CATEGORIE;TAILLECM;ROLE;TOTAL_SCORE
                    String sql = "INSERT INTO joueur (SURNOM, CATEGORIE, TAILLECM, ROLE, TOTAL_SCORE) VALUES (?,?,?,?,?) " +
                                 "ON DUPLICATE KEY UPDATE CATEGORIE=VALUES(CATEGORIE), TAILLECM=VALUES(TAILLECM), ROLE=VALUES(ROLE), TOTAL_SCORE=VALUES(TOTAL_SCORE)";
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setString(1, values[1]); // SURNOM
                        pst.setString(2, values[2]); // CATEGORIE
                        pst.setObject(3, values[3] != null ? Integer.parseInt(values[3]) : null); // TAILLE
                        pst.setString(4, values[4] != null ? values[4] : "U"); // ROLE
                        pst.setInt(5, values[5] != null ? Integer.parseInt(values[5]) : 0); // SCORE
                        pst.executeUpdate();
                    }
                } 
                
                else if ("matchs".equals(tableName) && values.length >= 4) {
                    // CSV attendu : ID;ID_RONDE;ID_TERRAIN;STATUT
                    // Ici on insert en forçant les IDs car c'est une table de liaison
                    String sql = "INSERT INTO matchs (ID_RONDE, ID_TERRAIN, STATUT) VALUES (?,?,?) " +
                                 "ON DUPLICATE KEY UPDATE ID_RONDE=VALUES(ID_RONDE), ID_TERRAIN=VALUES(ID_TERRAIN), STATUT=VALUES(STATUT)";
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setInt(1, Integer.parseInt(values[1]));
                        pst.setInt(2, Integer.parseInt(values[2]));
                        pst.setString(3, values[3]);
                        pst.executeUpdate();
                    }
                }
                
                else if ("equipe".equals(tableName) && values.length >= 4) {
                    // CSV attendu : ID;NUM;SCORE;IDMATCH
                    String sql = "INSERT INTO equipe (NUM, SCORE, IDMATCH) VALUES (?,?,?) " +
                                 "ON DUPLICATE KEY UPDATE SCORE=VALUES(SCORE), IDMATCH=VALUES(IDMATCH)";
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setInt(1, Integer.parseInt(values[1]));
                        pst.setInt(2, Integer.parseInt(values[2]));
                        pst.setInt(3, Integer.parseInt(values[3]));
                        pst.executeUpdate();
                    }
                }
                
                else if ("composition".equals(tableName) && values.length >= 2) {
                    // CSV attendu : IDEQUIPE;IDJOUEUR (Pas d'ID auto ici, c'est une clé composée)
                    String sql = "INSERT IGNORE INTO composition (IDEQUIPE, IDJOUEUR) VALUES (?,?)";
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setInt(1, Integer.parseInt(values[0]));
                        pst.setInt(2, Integer.parseInt(values[1]));
                        pst.executeUpdate();
                    }
                }
                
                else if ("terrain".equals(tableName) && values.length >= 3) {
                    // CSV attendu : ID;NOM;TYPE
                     String sql = "INSERT INTO terrain (NOM, TYPE) VALUES (?,?) ON DUPLICATE KEY UPDATE TYPE=VALUES(TYPE)";
                    try (PreparedStatement pst = con.prepareStatement(sql)) {
                        pst.setString(1, values[1]);
                        pst.setString(2, values[2]);
                        pst.executeUpdate();
                    }
                }
            }
            con.commit();
        } catch (Exception ex) {
            con.rollback();
            // On affiche l'erreur précise pour aider au débogage
            System.err.println("Erreur Import CSV : " + ex.getMessage());
            throw new SQLException("Erreur lors de l'import du fichier : " + ex.getMessage(), ex);
        } finally {
            con.setAutoCommit(true);
        }
    }
}