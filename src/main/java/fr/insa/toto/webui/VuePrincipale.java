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
package fr.insa.toto.webui;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.AbstractField;
import java.util.Set;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.*;
// Ajoutez ces imports en haut de VuePrincipale.java
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils; // Utile si dispo, sinon on fera en Java pur

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Point d'entrée de l'application Web (Vaadin).
 * Remplace le MainConsoleTournoi.
 */
@Route("") // Mappe cette vue à la racine du site (http://localhost:8080/)
@StyleSheet("css/style.css")
public class VuePrincipale extends VerticalLayout {

    // Session et Utilisateur
    private Connection con;
    private Joueur currentUser;

    // Composants d'interface principaux
    private VerticalLayout contentArea; // Zone où on affiche les différentes pages (Joueurs, Matchs...)
    private H1 headerTitle;

    public VuePrincipale() {
        try {
            this.con = ConnectionSimpleSGBD.defaultCon();
        } catch (SQLException e) {
            Notification.show("Erreur connexion BDD: " + e.getMessage());
            return;
        }

        // Configuration de base de la page
        setSizeFull();
        setAlignItems(Alignment.CENTER);

        // Affiche l'écran de connexion au démarrage
        showLoginScreen();
    }

    // =========================================================================
    // 1. ÉCRAN DE CONNEXION (LOGIN)
    // =========================================================================
    private void showLoginScreen() {
        removeAll(); // Nettoie l'écran

        H1 title = new H1("Gestion Tournois 2025");
        TextField surnomField = new TextField("Votre Surnom");
        Button loginBtn = new Button("Se connecter");
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        loginBtn.addClickListener(e -> {
            String surnom = surnomField.getValue();
            try {
                Optional<Joueur> userOpt = Joueur.findBySurnom(con, surnom);
                if (userOpt.isPresent()) {
                    this.currentUser = userOpt.get();
                    Notification.show("Bienvenue " + currentUser.getSurnom());
                    showMainInterface(); // Passage à l'interface principale
                } else {
                    Notification.show("Utilisateur inconnu", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (SQLException ex) {
                Notification.show("Erreur SQL: " + ex.getMessage());
            }
        });

        // Layout centré pour le login
        VerticalLayout loginLayout = new VerticalLayout(title, surnomField, loginBtn);
        loginLayout.setAlignItems(Alignment.CENTER);
        loginLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        add(loginLayout);
    }

    // =========================================================================
    // 2. INTERFACE PRINCIPALE (MENU + CONTENU)
    // =========================================================================
    private void showMainInterface() {
        removeAll();

        // --- Header ---
        headerTitle = new H1("Tableau de Bord - " + currentUser.getSurnom());
        headerTitle.getStyle().set("font-size", "1.5em");
        
        Button logoutBtn = new Button("Déconnexion", e -> {
            currentUser = null;
            showLoginScreen();
        });
        logoutBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        HorizontalLayout header = new HorizontalLayout(headerTitle, logoutBtn);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        // --- Menu de Navigation ---
        HorizontalLayout navBar = new HorizontalLayout();
        
        // Boutons accessibles à tous
        Button btnClassement = new Button("Classement", e -> showClassementView());
        Button btnJoueurs = new Button("Joueurs", e -> showJoueursView());
        Button btnMatchs = new Button("Matchs & Résultats", e -> showMatchsView());

        navBar.add(btnClassement, btnJoueurs, btnMatchs);

        // Boutons ADMIN uniquement
// Dans showMainInterface() ...
        
        if ("A".equals(currentUser.getRole())) {
            Button btnTournois = new Button("Admin: Tournois", e -> showAdminTournoisView());
            Button btnParams = new Button("Admin: Paramètres", e -> showAdminParamsView());
            
            // --- NOUVEAU BOUTON ---
            Button btnData = new Button("Admin: Données (CSV)", e -> showAdminDataView());
            btnData.addThemeVariants(ButtonVariant.LUMO_ERROR); // Rouge
            // ----------------------

            btnTournois.addThemeVariants(ButtonVariant.LUMO_ERROR);
            btnParams.addThemeVariants(ButtonVariant.LUMO_ERROR);
            
            // N'oubliez pas d'ajouter btnData dans le navBar
            navBar.add(btnTournois, btnParams, btnData);
        }

        // --- Zone de Contenu ---
        contentArea = new VerticalLayout();
        contentArea.setSizeFull();
        
        // Assemblage
        add(header, navBar, contentArea);
        
        // Vue par défaut
        showClassementView();
    }

    // =========================================================================
    // 3. VUES (Sub-views)
    // =========================================================================

    // --- VUE CLASSEMENT ---
    private void showClassementView() {
        contentArea.removeAll();
        contentArea.add(new H2("Classement Général"));

        try {
            // Recalcul des scores avant affichage
            Joueur.mettreAJourClassementGeneral(con);
            
            List<Joueur> joueurs = Joueur.tousLesJoueurs(con);
            // Tri Java par score décroissant
            joueurs.sort((j1, j2) -> Integer.compare(j2.getTotalScore(), j1.getTotalScore()));

            Grid<Joueur> grid = new Grid<>(Joueur.class, false);
            grid.addColumn(Joueur::getSurnom).setHeader("Surnom");
            grid.addColumn(Joueur::getCategorie).setHeader("Catégorie");
            grid.addColumn(Joueur::getTotalScore).setHeader("Points Totaux");
            
            grid.setItems(joueurs);
            contentArea.add(grid);
            
        } catch (SQLException e) {
            Notification.show("Erreur: " + e.getMessage());
        }
    }

    // --- VUE LISTE DES JOUEURS ---
    private void showJoueursView() {
        contentArea.removeAll();
        contentArea.add(new H2("Liste des Joueurs"));

        try {
            List<Joueur> liste = Joueur.tousLesJoueurs(con);
            Grid<Joueur> grid = new Grid<>(Joueur.class, false);
            grid.addColumn(Joueur::getId).setHeader("ID").setWidth("50px").setFlexGrow(0);
            grid.addColumn(Joueur::getSurnom).setHeader("Surnom");
            grid.addColumn(Joueur::getTailleCm).setHeader("Taille (cm)");
            grid.addColumn(Joueur::getCategorie).setHeader("Catégorie");
            
            // Ajout simple (Admin seulement)
            if ("A".equals(currentUser.getRole())) {
                HorizontalLayout addLayout = new HorizontalLayout();
                TextField tfNom = new TextField("Surnom");
                TextField tfCat = new TextField("Cat");
                IntegerField tfTaille = new IntegerField("Taille");
                Button btnAdd = new Button("Ajouter", e -> {
                    try {
                        new Joueur(tfNom.getValue(), tfCat.getValue(), tfTaille.getValue()).saveInDB(con);
                        Notification.show("Joueur ajouté !");
                        showJoueursView(); // Rafraichir
                    } catch (Exception ex) {
                        Notification.show("Erreur ajout: " + ex.getMessage());
                    }
                });
                addLayout.add(tfNom, tfCat, tfTaille, btnAdd);
                addLayout.setAlignItems(Alignment.BASELINE);
                contentArea.add(addLayout);
            }

            grid.setItems(liste);
            contentArea.add(grid);
        } catch (SQLException e) {
            Notification.show("Erreur: " + e.getMessage());
        }
    }

    // --- VUE MATCHS (Saisie scores) ---
    private void showMatchsView() {
        contentArea.removeAll();
        contentArea.add(new H2("Matchs en cours"));

        try {
            List<Matchs> matchsOuverts = Matchs.matchsOuverts(con);
            
            if (matchsOuverts.isEmpty()) {
                contentArea.add(new Span("Aucun match en cours actuellement."));
                return;
            }

            Grid<Matchs> grid = new Grid<>(Matchs.class, false);
            grid.addColumn(Matchs::getId).setHeader("ID Match");
            grid.addColumn(m -> "Ronde " + getRondeNum(m.getIdRonde())).setHeader("Ronde");
            grid.addColumn(m -> getTerrainNom(m.getIdTerrain())).setHeader("Terrain");
            grid.addColumn(Matchs::getStatut).setHeader("Statut");

            // Bouton d'action pour noter
            if ("A".equals(currentUser.getRole())) {
                grid.addComponentColumn(match -> {
                    Button btnNote = new Button("Saisir Scores");
                    btnNote.addClickListener(e -> openScoreDialog(match));
                    return btnNote;
                }).setHeader("Action");
            }

            grid.setItems(matchsOuverts);
            contentArea.add(grid);

        } catch (SQLException e) {
            Notification.show("Erreur: " + e.getMessage());
        }
    }
    
    // --- VUE ADMIN : CRÉATION TOURNOI/RONDE ---
    private void showAdminTournoisView() {
        contentArea.removeAll();
        contentArea.add(new H2("Gestion Tournois & Matchs"));
        
        // --- PARTIE 1 : CRÉATION TOURNOI ---
        HorizontalLayout layoutTournoi = new HorizontalLayout();
        TextField nomTournoi = new TextField("Nom du nouveau Tournoi");
        Button btnCreateT = new Button("Créer Tournoi", e -> {
            try {
                new Tournoi(nomTournoi.getValue(), null, null).saveInDB(con);
                Notification.show("Tournoi créé.");
            } catch (Exception ex) { Notification.show("Erreur: " + ex.getMessage()); }
        });
        layoutTournoi.add(nomTournoi, btnCreateT);
        layoutTournoi.setAlignItems(Alignment.BASELINE);
        
        // --- PARTIE 2 : GESTION DES RONDES ET MATCHS ---
        VerticalLayout layoutRonde = new VerticalLayout();
        layoutRonde.getStyle().set("border", "1px solid #ddd").set("padding", "10px");
        
        H3 titleRonde = new H3("Ajouter une Ronde & ses Matchs");
        
        ComboBox<Tournoi> cbTournoi = new ComboBox<>("1. Choisir Tournoi");
        try { cbTournoi.setItems(Tournoi.tousLesTournois(con)); } catch(Exception ex){}
        cbTournoi.setItemLabelGenerator(Tournoi::getNom);
        
        IntegerField numRonde = new IntegerField("2. Numéro Ronde");
        
        // Bouton pour créer la ronde vide
        Button btnCreerRonde = new Button("Créer la Ronde", e -> {
            if(cbTournoi.getValue() == null || numRonde.getValue() == null) {
                Notification.show("Sélectionnez un tournoi et un numéro.");
                return;
            }
            try {
                Ronde r = new Ronde(numRonde.getValue(), cbTournoi.getValue().getId());
                r.saveInDB(con);
                Notification.show("Ronde " + r.getNumero() + " initialisée. Ajoutez maintenant les matchs !");
                // On ouvre directement le dialogue de création de match pour cette ronde
                openManualMatchCreationDialog(r.getId());
            } catch (Exception ex) { Notification.show("Erreur: " + ex.getMessage()); }
        });
        
        // Bouton pour ajouter des matchs à une ronde existante (si on revient plus tard)
        Button btnAjoutMatch = new Button("Ajouter Matchs à une ronde existante", e -> {
             // Pour simplifier ici, on demande à l'utilisateur de saisir l'ID de la ronde, 
             // ou on pourrait faire un selecteur de ronde. 
             // Pour l'instant, on suppose qu'il vient de créer la ronde ci-dessus.
             Notification.show("Utilisez le bouton 'Créer la Ronde' pour démarrer une session.");
        });

        layoutRonde.add(titleRonde, cbTournoi, numRonde, btnCreerRonde);
        
        contentArea.add(layoutTournoi, layoutRonde);
    }
    
    // --- VUE ADMIN : PARAMETRES ---
    // =========================================================================
    // VUE ADMIN : PARAMETRES GLOBAUX + CRÉATION RAPIDE TERRAINS
    // =========================================================================
    private void showAdminParamsView() {
        contentArea.removeAll();
        contentArea.add(new H2("Configuration du Tournoi"));
        
        try {
            // --- PARTIE 1 : PARAMÈTRES GLOBAUX ---
            VerticalLayout paramsLayout = new VerticalLayout();
            paramsLayout.getStyle().set("border", "1px solid #ddd").set("padding", "20px").set("border-radius", "10px");
            
            H3 titleParams = new H3("1. Règles Globales");
            
            Parametres p = Parametres.load(con);
            
            IntegerField nbT = new IntegerField("Nombre de terrains simultanés");
            nbT.setValue(p.getNbTerrains());
            nbT.setHelperText("Combien de matchs peuvent se jouer en même temps ?");
            
            IntegerField nbJ = new IntegerField("Joueurs par Équipe");
            nbJ.setValue(p.getNbJoueursParEquipe());
            nbJ.setHelperText("Ex: 1 pour Simple, 2 pour Double");
            
            Button saveParams = new Button("Sauvegarder les Règles", e -> {
               if (nbT.getValue() == null || nbJ.getValue() == null) return;
               p.setNbTerrains(nbT.getValue());
               p.setNbJoueursParEquipe(nbJ.getValue());
               try {
                   p.save(con);
                   Notification.show("Règles mises à jour avec succès.", 3000, Notification.Position.MIDDLE)
                           .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
               } catch (Exception ex) { 
                   Notification.show("Erreur: " + ex.getMessage()); 
               }
            });
            saveParams.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            paramsLayout.add(titleParams, nbT, nbJ, saveParams);
            
            
            // --- PARTIE 2 : CRÉATION RAPIDE DE TERRAIN ---
            VerticalLayout terrainLayout = new VerticalLayout();
            terrainLayout.getStyle().set("border", "1px solid #ddd").set("padding", "20px").set("border-radius", "10px");
            terrainLayout.getStyle().set("margin-top", "20px"); // Espace entre les blocs
            
            H3 titleTerrain = new H3("2. Ajouter un Terrain Physique");
            
            HorizontalLayout formTerrain = new HorizontalLayout();
            formTerrain.setAlignItems(Alignment.BASELINE); // Aligner le bouton avec les champs
            
            TextField nomTerrain = new TextField("Nom du Terrain");
            nomTerrain.setPlaceholder("Ex: Court Central");
            
            ComboBox<String> typeTerrain = new ComboBox<>("Type");
            typeTerrain.setItems("Couvert", "Ouvert");
            typeTerrain.setValue("Ouvert");
            
            Button btnAddTerrain = new Button("Créer Terrain", e -> {
                if (nomTerrain.isEmpty() || typeTerrain.getValue() == null) {
                    Notification.show("Veuillez remplir le nom et le type.");
                    return;
                }
                try {
                    // Conversion "Couvert" -> "C", "Ouvert" -> "O"
                    String codeType = typeTerrain.getValue().equals("Couvert") ? "C" : "O";
                    
                    new Terrain(nomTerrain.getValue(), codeType).saveInDB(con);
                    
                    Notification.show("Terrain '" + nomTerrain.getValue() + "' créé !", 
                            3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                    // Reset du champ pour en ajouter un autre rapidement
                    nomTerrain.clear();
                    nomTerrain.focus();
                    
                } catch (Exception ex) {
                    Notification.show("Erreur (Nom déjà pris ?) : " + ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            
            formTerrain.add(nomTerrain, typeTerrain, btnAddTerrain);
            terrainLayout.add(titleTerrain, formTerrain);

            // Ajout des deux blocs à la page
            contentArea.add(paramsLayout, terrainLayout);
            
        } catch (SQLException e) {
            Notification.show("Erreur chargement params: " + e.getMessage());
        }
    }
    
    // =========================================================================
    // VUE ADMIN : GESTION DONNÉES (IMPORT/EXPORT)
    // =========================================================================
    private void showAdminDataView() {
        contentArea.removeAll();
        contentArea.add(new H2("Import / Export CSV"));

        // --- SECTION 1 : EXPORT (Sauvegarde sur le serveur) ---
        VerticalLayout exportLayout = new VerticalLayout();
        exportLayout.add(new H2("Exporter vers CSV"));
        
        ComboBox<String> cbTableExport = new ComboBox<>("Table à exporter");
        cbTableExport.setItems("joueur", "matchs", "equipe", "composition", "tournoi", "ronde", "terrain");
        cbTableExport.setValue("joueur"); // Valeur par défaut
        
        Button btnExport = new Button("Exporter le fichier", e -> {
            if (cbTableExport.getValue() == null) return;
            try {
                // Appelle votre fonction existante
                GestionBdD.exportCSV(con, cbTableExport.getValue());
                
                // Récupère le chemin absolu pour informer l'utilisateur
                File f = new File(cbTableExport.getValue() + ".csv");
                Notification.show("Succès ! Fichier créé ici : " + f.getAbsolutePath(), 
                        5000, Notification.Position.MIDDLE);
            } catch (Exception ex) {
                Notification.show("Erreur Export: " + ex.getMessage());
            }
        });
        
        exportLayout.add(cbTableExport, btnExport);
        exportLayout.getStyle().set("border", "1px solid #ccc").set("padding", "20px");

        // --- SECTION 2 : IMPORT (Upload depuis le PC) ---
        VerticalLayout importLayout = new VerticalLayout();
        importLayout.add(new H2("Importer depuis CSV"));
        
        ComboBox<String> cbTableImport = new ComboBox<>("Table cible");
        cbTableImport.setItems("joueur", "matchs", "equipe", "composition");
        cbTableImport.setValue("joueur");

        // Composant Vaadin pour l'upload
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv", ".txt");
        
        upload.addSucceededListener(event -> {
            String tableName = cbTableImport.getValue();
            if (tableName == null) {
                Notification.show("Veuillez choisir une table cible !");
                return;
            }

            // Fichier temporaire
            File tempFile = null;

            try {
                // 1. Récupération du flux
                InputStream inputStream = buffer.getInputStream();
                
                // 2. Création fichier temp sécurisé
                tempFile = File.createTempFile("import_" + tableName + "_", ".csv");
                
                // 3. Écriture robuste (Standard Java)
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] bufferBytes = new byte[4096]; // Tampon de 4ko
                    int bytesRead;
                    while ((bytesRead = inputStream.read(bufferBytes)) != -1) {
                        out.write(bufferBytes, 0, bytesRead);
                    }
                }
                
                System.out.println("Fichier reçu enregistré ici : " + tempFile.getAbsolutePath());

                // 4. Appel de la logique BDD
                // Attention : On vérifie que le fichier n'est pas vide
                if (tempFile.length() == 0) {
                    throw new Exception("Le fichier reçu est vide !");
                }

                GestionBdD.importCSV(con, tableName, tempFile.getAbsolutePath());
                
                Notification.show("✅ Import réussi dans la table " + tableName + " !", 
                        3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("❌ ERREUR : " + ex.getMessage(), 
                        5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                // Nettoyage
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        });

        importLayout.add(cbTableImport, upload);
        importLayout.getStyle().set("border", "1px solid #ccc").set("padding", "20px");

        // Ajout des deux sections
        contentArea.add(exportLayout, importLayout);
    }

    // =========================================================================
    // 4. DIALOGUES & UTILITAIRES
    // =========================================================================

    // Ouvre une fenêtre modale pour saisir les scores d'un match
    private void openScoreDialog(Matchs m) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Arbitrage - Match " + m.getId());
        dialog.setWidth("800px");

        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setJustifyContentMode(JustifyContentMode.EVENLY);

        try {
            List<Equipe> equipes = Equipe.equipesPourMatch(con, m.getId());
            if (equipes.size() < 2) {
                Notification.show("Erreur : Données équipes invalides");
                return;
            }

            // On boucle sur les 2 équipes pour créer deux colonnes visuelles
            for (Equipe equipe : equipes) {
                VerticalLayout teamLayout = new VerticalLayout();
                teamLayout.setAlignItems(Alignment.CENTER);
                
                // 1. En-tête de l'équipe (Numéro + Score actuel)
                // On utilise un Span pour pouvoir mettre à jour le texte du score dynamiquement
                Span scoreLabel = new Span("Score: " + equipe.getScore());
                scoreLabel.getStyle().set("font-weight", "bold").set("font-size", "1.2em");
                
                H3 title = new H3("Équipe " + equipe.getNum());
                teamLayout.add(title, scoreLabel);
                
                // 2. Liste des joueurs de cette équipe
                List<Joueur> joueurs = equipe.getJoueurs(con); // Utilise la méthode corrigée d'Equipe
                
                for (Joueur joueur : joueurs) {
                    // Bouton pour chaque joueur
                    Button btnMarquer = new Button(joueur.getSurnom() + " (+1)", e -> {
                        try {
                            // A. Mise à jour Equipe (+1)
                            int newScoreEq = equipe.getScore() + 1;
                            equipe.setScore(newScoreEq);
                            equipe.updateInDB(con);
                            
                            // B. Mise à jour Joueur (+1 au total score global)
                            int newScoreJoueur = joueur.getTotalScore() + 1;
                            joueur.setTotalScore(newScoreJoueur);
                            joueur.updateInDB(con);
                            
                            // C. Mise à jour Visuelle
                            scoreLabel.setText("Score: " + newScoreEq);
                            Notification.show("But pour " + joueur.getSurnom() + " !");
                            
                        } catch (SQLException ex) {
                            Notification.show("Erreur BDD: " + ex.getMessage());
                        }
                    });
                    
                    // Style du bouton
                    btnMarquer.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    btnMarquer.setWidthFull();
                    teamLayout.add(btnMarquer);
                }
                
                // Ajout de la colonne au layout principal
                mainLayout.add(teamLayout);
            }

            dialog.add(mainLayout);

            // --- PIED DE PAGE (BOUTON FINIR) ---
            Button btnClore = new Button("Siffler la fin du match (Clore)", e -> {
                try {
                    // On passe le match à "clos"
                    m.setStatut("clos");
                    m.updateInDB(con);
                    
                    // On recalcule le classement général par sécurité
                    Joueur.mettreAJourClassementGeneral(con);
                    
                    // Vérification fin de ronde
                    if (Ronde.tousMatchsRondeClos(con, m.getIdRonde())) {
                         new Ronde(m.getIdRonde(), 0, 0, "").setEtat(con, "close");
                         Notification.show("Match terminé et Ronde CLÔTURÉE !", 5000, Notification.Position.MIDDLE);
                    } else {
                        Notification.show("Match terminé.");
                    }
                    
                    dialog.close();
                    showMatchsView(); // Rafraichir la liste des matchs
                } catch (Exception ex) {
                    Notification.show("Erreur Clôture: " + ex.getMessage());
                }
            });
            btnClore.addThemeVariants(ButtonVariant.LUMO_ERROR); // Rouge pour marquer l'importance
            btnClore.getStyle().set("margin-top", "20px");
            
            // Centre le bouton fermer
            HorizontalLayout footer = new HorizontalLayout(btnClore);
            footer.setJustifyContentMode(JustifyContentMode.CENTER);
            footer.setWidthFull();
            
            dialog.add(footer);
            dialog.open();

        } catch (SQLException e) {
            Notification.show("Erreur chargement match: " + e.getMessage());
        }
    }

    // Petits helpers pour afficher des noms au lieu des IDs dans les grids
    private String getRondeNum(int idRonde) {
        // Pour faire simple, on fait une query rapide. 
        // En prod, on utiliserait un Map ou une jointure.
        try (java.sql.PreparedStatement pst = con.prepareStatement("SELECT NUMERO FROM ronde WHERE ID=?")) {
            pst.setInt(1, idRonde);
            java.sql.ResultSet rs = pst.executeQuery();
            if(rs.next()) return String.valueOf(rs.getInt(1));
        } catch(Exception e) {}
        return "?";
    }
    
    private String getTerrainNom(int idTerrain) {
        try (java.sql.PreparedStatement pst = con.prepareStatement("SELECT NOM FROM terrain WHERE ID=?")) {
            pst.setInt(1, idTerrain);
            java.sql.ResultSet rs = pst.executeQuery();
            if(rs.next()) return rs.getString(1);
        } catch(Exception e) {}
        return "?";
    }
    
    private void openManualMatchCreationDialog(int idRonde) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Création de Match Manuel");
        dialog.setWidth("1000px");

        VerticalLayout mainLayout = new VerticalLayout();

        try {
            // 1. Paramètres et Terrain
            Parametres params = Parametres.load(con);
            int maxJoueurs = params.getNbJoueursParEquipe();

            List<Terrain> terrainsLibres = Terrain.terrainsDisponibles(con);
            ComboBox<Terrain> cbTerrain = new ComboBox<>("Terrain (Uniquement les libres)");
            cbTerrain.setItems(terrainsLibres);
            cbTerrain.setItemLabelGenerator(Terrain::getNom);
            if (terrainsLibres.isEmpty()) Notification.show("Attention : Plus de terrain libre !");

            List<Joueur> allPlayers = Joueur.tousLesJoueurs(con);

            // 2. Création des listes (On met TOUS les joueurs partout au début)
            MultiSelectComboBox<Joueur> mainEq1 = new MultiSelectComboBox<>("Équipe 1 - Titulaires (Max " + maxJoueurs + ")");
            mainEq1.setItemLabelGenerator(Joueur::getSurnom);
            mainEq1.setWidthFull();
            mainEq1.setItems(allPlayers);

            MultiSelectComboBox<Joueur> subEq1 = new MultiSelectComboBox<>("Équipe 1 - Remplaçants");
            subEq1.setItemLabelGenerator(Joueur::getSurnom);
            subEq1.setWidthFull();
            subEq1.setItems(allPlayers);

            MultiSelectComboBox<Joueur> mainEq2 = new MultiSelectComboBox<>("Équipe 2 - Titulaires (Max " + maxJoueurs + ")");
            mainEq2.setItemLabelGenerator(Joueur::getSurnom);
            mainEq2.setWidthFull();
            mainEq2.setItems(allPlayers);

            MultiSelectComboBox<Joueur> subEq2 = new MultiSelectComboBox<>("Équipe 2 - Remplaçants");
            subEq2.setItemLabelGenerator(Joueur::getSurnom);
            subEq2.setWidthFull();
            subEq2.setItems(allPlayers);

            // 3. LOGIQUE DE VALIDATION ROBUSTE (Anti-Bug)
            // Au lieu de modifier les items, on vérifie juste si le joueur est dispo
            HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<MultiSelectComboBox<Joueur>, Set<Joueur>>> validationListener = event -> {
                if (!event.isFromClient()) return; // Ignore les changements faits par le code

                MultiSelectComboBox<Joueur> source = event.getSource();
                Set<Joueur> newSelection = event.getValue();

                // A. Vérification Limite (Max Titulaires)
                if ((source == mainEq1 || source == mainEq2) && newSelection.size() > maxJoueurs) {
                    source.setValue(event.getOldValue()); // Annule l'action
                    Notification.show("Limite atteinte ! Max " + maxJoueurs + " titulaires.")
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                // B. Vérification Doublons (Est-ce que le joueur est déjà ailleurs ?)
                // On récupère les joueurs sélectionnés dans les 3 AUTRES listes
                List<Joueur> others = new ArrayList<>();
                if (source != mainEq1) others.addAll(mainEq1.getValue());
                if (source != subEq1)  others.addAll(subEq1.getValue());
                if (source != mainEq2) others.addAll(mainEq2.getValue());
                if (source != subEq2)  others.addAll(subEq2.getValue());

                // On cherche s'il y a une intersection entre la "Nouvelle Sélection" et "Les Autres"
                for (Joueur j : newSelection) {
                    if (others.contains(j)) {
                        // ERREUR : Le joueur est déjà pris !
                        source.setValue(event.getOldValue()); // On annule tout de suite
                        Notification.show(j.getSurnom() + " est déjà placé dans une autre liste !", 
                                3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                }
            };

            // 4. Attachement des listeners
            mainEq1.addValueChangeListener(validationListener);
            subEq1.addValueChangeListener(validationListener);
            mainEq2.addValueChangeListener(validationListener);
            subEq2.addValueChangeListener(validationListener);

            // 5. Mise en page
            HorizontalLayout row1 = new HorizontalLayout(mainEq1, subEq1); row1.setWidthFull();
            HorizontalLayout row2 = new HorizontalLayout(mainEq2, subEq2); row2.setWidthFull();

            Button btnSave = new Button("Valider le Match", e -> {
                if (cbTerrain.getValue() == null) { Notification.show("Sélectionnez un terrain."); return; }
                if (mainEq1.getValue().isEmpty() || mainEq2.getValue().isEmpty()) { Notification.show("Les équipes titulaires sont vides !"); return; }

                try {
                    List<Joueur> totalEq1 = new ArrayList<>(mainEq1.getValue());
                    totalEq1.addAll(subEq1.getValue());
                    List<Joueur> totalEq2 = new ArrayList<>(mainEq2.getValue());
                    totalEq2.addAll(subEq2.getValue());

                    ServiceTournoi.creerMatchManuel(con, idRonde, cbTerrain.getValue().getId(), totalEq1, totalEq2);
                    Notification.show("Match créé avec succès !");
                    dialog.close();
                } catch (Exception ex) { Notification.show("Erreur: " + ex.getMessage()); }
            });
            btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            mainLayout.add(cbTerrain, new H3("Équipe 1"), row1, new H3("Équipe 2"), row2, btnSave);
            dialog.add(mainLayout);
            dialog.open();

        } catch (SQLException ex) {
            Notification.show("Erreur BDD: " + ex.getMessage());
        }
    }

    // Helper pour retirer les joueurs déjà pris
    @SafeVarargs
    private List<Joueur> filterList(List<Joueur> source, List<Joueur>... exclusions) {
        List<Joueur> result = new ArrayList<>(source);
        for (List<Joueur> exc : exclusions) {
            // Grâce au equals/hashCode ajouté dans Joueur.java, ceci fonctionne maintenant parfaitement
            result.removeAll(exc);
        }
        return result;
    }

   
}