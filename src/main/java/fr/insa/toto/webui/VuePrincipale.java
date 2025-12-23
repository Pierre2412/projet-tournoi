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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H4;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import com.vaadin.flow.data.renderer.LitRenderer; // Important pour le classement 1, 2, 3...
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.ArrayList;
import java.util.List;

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
    removeAll(); 

    H1 title = new H1("Gestion Tournois 2025");
    
    TextField surnomField = new TextField("Votre Surnom");
    
    // Nouveau champ Mot de Passe
    PasswordField passwordField = new PasswordField("Mot de passe");
    passwordField.setPlaceholder("Requis pour les Admins");
    passwordField.setHelperText("Laisser vide si vous n'êtes pas admin");

    Button loginBtn = new Button("Se connecter");
    loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    // Permet de valider avec la touche Entrée
    loginBtn.addClickShortcut(com.vaadin.flow.component.Key.ENTER); 

    loginBtn.addClickListener(e -> {
        String surnom = surnomField.getValue();
        String password = passwordField.getValue();
        
        try {
            // 1. On cherche l'utilisateur
            Optional<Joueur> userOpt = Joueur.findBySurnom(con, surnom);
            
            if (userOpt.isPresent()) {
                Joueur user = userOpt.get();
                
                // 2. VERIFICATION SÉCURITÉ
                if ("A".equals(user.getRole())) {
                    // Si c'est un Admin, on vérifie le mot de passe
                    // On compare le mot de passe saisi avec celui en base (Attention aux nulls)
                    String dbPass = user.getMotDePasse();
                    
                    if (dbPass != null && !dbPass.equals(password)) {
                        Notification.show("Mot de passe Admin incorrect !", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return; // On arrête tout, pas de connexion
                    }
                    
                    if (dbPass == null) {
                         // Cas rare : un admin sans mot de passe en base (vieille version)
                         // On laisse passer ou on bloque, ici on laisse passer avec un warning
                         Notification.show("Attention : Compte Admin sans sécurité !");
                    }
                }
                
                // 3. Connexion réussie
                this.currentUser = user;
                Notification.show("Bienvenue " + currentUser.getSurnom());
                showMainInterface(); 
                
            } else {
                Notification.show("Utilisateur inconnu", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (SQLException ex) {
            Notification.show("Erreur SQL: " + ex.getMessage());
        }
    });

    VerticalLayout loginLayout = new VerticalLayout(title, surnomField, passwordField, loginBtn);
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
        Button btnVueTournoi = new Button("Vue Tournois", e -> showVueGlobaleTournoi());

        navBar.add(btnClassement, btnJoueurs, btnVueTournoi, btnMatchs);

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
    // --- VUE LISTE DES JOUEURS ---
// --- VUE LISTE DES JOUEURS ---
    // --- VUE LISTE DES JOUEURS (COMPLÈTE : Recherche + Création + Détails) ---
    private void showJoueursView() {
        contentArea.removeAll();
        contentArea.add(new H2("Annuaire des Joueurs"));

        try {
            // 1. Récupération des données
            List<Joueur> tousLesJoueurs = Joueur.tousLesJoueurs(con);
            
            // 2. Barre de Recherche (Toujours visible)
            TextField searchField = new TextField();
            searchField.setPlaceholder("Rechercher par nom...");
            searchField.setPrefixComponent(new Span("🔍"));
            searchField.setWidth("300px");
            
            // 3. Configuration de la Grille
            Grid<Joueur> grid = new Grid<>(Joueur.class, false);
            grid.addColumn(Joueur::getId).setHeader("ID").setWidth("60px").setFlexGrow(0);
            grid.addColumn(Joueur::getSurnom).setHeader("Surnom").setSortable(true);
            grid.addColumn(Joueur::getCategorie).setHeader("Cat").setWidth("80px").setFlexGrow(0);
            grid.addColumn(Joueur::getTotalScore).setHeader("Score Total").setSortable(true);
            grid.addColumn(j -> "A".equals(j.getRole()) ? "Admin" : "Joueur").setHeader("Rôle");

            // Filtrage dynamique
            com.vaadin.flow.component.grid.dataview.GridListDataView<Joueur> dataView = grid.setItems(tousLesJoueurs);
            searchField.addValueChangeListener(e -> {
                dataView.setFilter(joueur -> {
                    String recherche = e.getValue().toLowerCase();
                    String nom = joueur.getSurnom().toLowerCase();
                    return nom.contains(recherche);
                });
            });

            // Clic pour détails
            grid.addItemClickListener(event -> afficherHistoriqueJoueur(event.getItem()));
            grid.getStyle().set("cursor", "pointer"); 

            // --- SECTION ADMIN : BOUTONS DE CRÉATION ---
            if ("A".equals(currentUser.getRole())) {
                
                // A. BOUTON MAGIQUE (Génération auto)
                Button btnGenAuto = new Button("🎲 Générer 5 Joueurs", e -> {
                    try {
                        for (int i = 0; i < 5; i++) {
                            int randId = (int)(Math.random() * 10000);
                            String nom = "Joueur " + randId;
                            String cat = Math.random() > 0.5 ? "S" : "J"; // S ou J uniquement
                            int taille = 160 + (int)(Math.random() * 40);
                            new Joueur(nom, cat, taille).saveInDB(con);
                        }
                        Notification.show("5 joueurs créés !");
                        showJoueursView(); // Rafraichir
                    } catch (Exception ex) {
                        Notification.show("Erreur: " + ex.getMessage());
                    }
                });
                btnGenAuto.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                btnGenAuto.getStyle().set("margin-bottom", "10px");

                // B. FORMULAIRE MANUEL
                HorizontalLayout addLayout = new HorizontalLayout();
                addLayout.setAlignItems(Alignment.BASELINE);
                
                TextField tfNom = new TextField("Surnom");
                TextField tfCat = new TextField("Cat (S/J)");
                IntegerField tfTaille = new IntegerField("Taille");
                
                Checkbox cbIsAdmin = new Checkbox("Admin ?");
                PasswordField tfPass = new PasswordField("Mot de passe");
                tfPass.setPlaceholder("Requis");
                tfPass.setVisible(false);
                
                cbIsAdmin.addValueChangeListener(e -> {
                    tfPass.setVisible(e.getValue());
                    if (!e.getValue()) tfPass.clear();
                });
                
                Button btnAdd = new Button("Créer Manuellement", e -> {
                    try {
                        Joueur nouveau = new Joueur(tfNom.getValue(), tfCat.getValue(), tfTaille.getValue());
                        if (cbIsAdmin.getValue()) {
                            nouveau.setRole("A");
                            if (tfPass.isEmpty()) {
                                Notification.show("Mot de passe requis pour Admin !").addThemeVariants(NotificationVariant.LUMO_ERROR);
                                return;
                            }
                            nouveau.setMotDePasse(tfPass.getValue());
                        } else {
                            nouveau.setRole("U");
                        }
                        nouveau.saveInDB(con); 
                        Notification.show("Joueur ajouté !");
                        showJoueursView();
                    } catch (Exception ex) {
                        Notification.show("Erreur: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                
                addLayout.add(tfNom, tfCat, tfTaille, cbIsAdmin, tfPass, btnAdd);
                
                // On ajoute les outils Admin AVANT la grille
                contentArea.add(btnGenAuto, addLayout);
            }
            
            // On ajoute enfin la recherche et la grille pour tout le monde
            contentArea.add(searchField, grid);
            
        } catch (SQLException e) {
            Notification.show("Erreur: " + e.getMessage());
        }
    }
    
    private void afficherHistoriqueJoueur(Joueur j) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Historique : " + j.getSurnom());
        dialog.setWidth("600px");
        
        VerticalLayout layout = new VerticalLayout();
        
        // Résumé stats
        layout.add(new H4("Score cumulé : " + j.getTotalScore() + " points"));
        
        // Liste des matchs
        Grid<Matchs> gridMatchs = new Grid<>(Matchs.class, false);
        gridMatchs.addColumn(m -> "Match #" + m.getId()).setHeader("Match");
        gridMatchs.addColumn(Matchs::getStatut).setHeader("Statut");
        
        // Colonne calculée compliquée : "Combien j'ai marqué dans ce match ?"
        gridMatchs.addColumn(m -> {
             try {
                 // On cherche l'équipe du joueur dans ce match
                 String sql = "SELECT e.SCORE FROM equipe e " +
                              "JOIN composition c ON c.IDEQUIPE = e.ID " +
                              "WHERE e.IDMATCH = ? AND c.IDJOUEUR = ?";
                 try (java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
                     pst.setInt(1, m.getId());
                     pst.setInt(2, j.getId());
                     java.sql.ResultSet rs = pst.executeQuery();
                     if (rs.next()) return rs.getInt(1) + " pts";
                 }
             } catch (Exception ex) {}
             return "-";
        }).setHeader("Mon Score");

        try {
            // Requête pour trouver tous les matchs du joueur
            String sqlIds = "SELECT m.ID, m.ID_RONDE, m.ID_TERRAIN, m.STATUT " +
                            "FROM matchs m " +
                            "JOIN equipe e ON e.IDMATCH = m.ID " +
                            "JOIN composition c ON c.IDEQUIPE = e.ID " +
                            "WHERE c.IDJOUEUR = ? ORDER BY m.ID DESC";
            
            List<Matchs> matchsJoues = new ArrayList<>();
            try (java.sql.PreparedStatement pst = con.prepareStatement(sqlIds)) {
                pst.setInt(1, j.getId());
                java.sql.ResultSet rs = pst.executeQuery();
                while(rs.next()) {
                    matchsJoues.add(new Matchs(rs.getInt("ID"), rs.getInt("ID_RONDE"), 
                                               rs.getInt("ID_TERRAIN"), rs.getString("STATUT")));
                }
            }
            
            if (matchsJoues.isEmpty()) layout.add(new Span("Aucun match joué pour le moment."));
            else {
                gridMatchs.setItems(matchsJoues);
                layout.add(gridMatchs);
            }
            
        } catch (SQLException e) {
            layout.add(new Span("Erreur chargement historique."));
        }

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        layout.add(closeBtn);
        layout.setAlignItems(Alignment.END);
        
        dialog.add(layout);
        dialog.open();
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
    // --- VUE ADMIN : GESTION TOURNOIS ---
    // --- VUE ADMIN : GESTION TOURNOIS ---
    private void showAdminTournoisView() {
        contentArea.removeAll();
        contentArea.add(new H2("Gestion Tournois & Matchs"));
        
        // =========================================================
        // BLOC 1 : CRÉATION EXPRESS (AUTOMATIQUE)
        // =========================================================
        VerticalLayout autoLayout = new VerticalLayout();
        autoLayout.getStyle().set("border", "1px solid #2ecc71").set("padding", "20px").set("border-radius", "8px").set("background-color", "#f0fff4");
        
        H3 titleAuto = new H3("Création Automatique");
        
        TextField tfNomAuto = new TextField("Nom du Tournoi");
        tfNomAuto.setPlaceholder("Ex: Tournoi d'Été");
        
        java.util.Map<String, Integer> sports = new java.util.LinkedHashMap<>();
        sports.put("Tennis Simple", 1); sports.put("Tennis Double", 2); sports.put("Padel", 2);
        sports.put("Badminton Simple", 1); sports.put("Badminton Double", 2);
        sports.put("Basket 3x3", 3); sports.put("Football", 11); sports.put("Futsal", 5);
        
        ComboBox<String> cbSport = new ComboBox<>("Sport (Définit la taille des équipes)");
        cbSport.setItems(sports.keySet());
        cbSport.setValue("Tennis Simple");
        
        Button btnAuto = new Button("Générer Tournoi + Ronde 1 + Matchs", e -> {
            if (tfNomAuto.isEmpty() || cbSport.getValue() == null) {
                Notification.show("Veuillez remplir le nom et le sport.");
                return;
            }
            try {
                // 1. Params
                Parametres p = Parametres.load(con);
                p.setNbJoueursParEquipe(sports.get(cbSport.getValue()));
                p.save(con);
                // 2. Tournoi
                Tournoi t = new Tournoi(tfNomAuto.getValue(), null, null);
                t.saveInDB(con);
                // 3. Ronde 1
                Ronde r1 = new Ronde(1, t.getId());
                r1.saveInDB(con);
                // 4. Matchs
                ServiceTournoi.creerRondeAleatoire(con, r1.getId());
                
                Notification.show("✅ Tournoi '" + t.getNom() + "' généré avec succès !");
                tfNomAuto.clear();
                showAdminTournoisView(); // Rafraichir pour voir le nouveau tournoi dans la liste en bas
                
            } catch (Exception ex) {
                Notification.show("Erreur génération: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        btnAuto.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        
        autoLayout.add(titleAuto, tfNomAuto, cbSport, btnAuto);


        // =========================================================
        // BLOC 2 : GESTION MANUELLE COMPLETE
        // =========================================================
        VerticalLayout manualLayout = new VerticalLayout();
        manualLayout.getStyle().set("border", "1px solid #ddd").set("padding", "20px").set("margin-top", "20px");
        H3 titleManual = new H3("Gestion Manuelle");
        
        // --- SOUS-PARTIE A : CRÉER LE CONTENANT (TOURNOI VIDE) ---
        H4 titleCreateT = new H4("1. Créer un Tournoi vide");
        HorizontalLayout layoutCreateT = new HorizontalLayout();
        layoutCreateT.setAlignItems(Alignment.BASELINE);
        
        TextField tfNomManuel = new TextField("Nom");
        Button btnCreateManuel = new Button("Créer", e -> {
            if (tfNomManuel.isEmpty()) { Notification.show("Nom requis"); return; }
            try {
                new Tournoi(tfNomManuel.getValue(), null, null).saveInDB(con);
                Notification.show("Tournoi créé (vide). Vous pouvez maintenant lui ajouter des rondes.");
                tfNomManuel.clear();
                showAdminTournoisView(); // Rafraichir la liste déroulante ci-dessous
            } catch (Exception ex) { Notification.show("Erreur: " + ex.getMessage()); }
        });
        layoutCreateT.add(tfNomManuel, btnCreateManuel);

        // --- SOUS-PARTIE B : CRÉER LE CONTENU (RONDES & MATCHS) ---
        H4 titleAddRonde = new H4("2. Ajouter une Ronde & ses Matchs");
        HorizontalLayout layoutAddRonde = new HorizontalLayout();
        layoutAddRonde.setAlignItems(Alignment.BASELINE);
        
        ComboBox<Tournoi> cbTournoi = new ComboBox<>("Choisir Tournoi");
        try { cbTournoi.setItems(Tournoi.tousLesTournois(con)); } catch(Exception ex){}
        cbTournoi.setItemLabelGenerator(Tournoi::getNom);
        
        IntegerField numRonde = new IntegerField("N° Ronde");
        
        Button btnCreerRonde = new Button("Créer Ronde & Ouvrir Éditeur", e -> {
            if(cbTournoi.getValue() == null || numRonde.getValue() == null) return;
            try {
                Ronde r = new Ronde(numRonde.getValue(), cbTournoi.getValue().getId());
                r.saveInDB(con);
                Notification.show("Ronde créée.");
                openManualMatchCreationDialog(r.getId());
            } catch (Exception ex) { Notification.show("Erreur: " + ex.getMessage()); }
        });
        
        layoutAddRonde.add(cbTournoi, numRonde, btnCreerRonde);
        
        manualLayout.add(titleManual, titleCreateT, layoutCreateT, titleAddRonde, layoutAddRonde);
        
        contentArea.add(autoLayout, manualLayout);
    }
    
    // --- VUE ADMIN : PARAMETRES ---
    // =========================================================================
    // VUE ADMIN : PARAMETRES GLOBAUX + CRÉATION RAPIDE TERRAINS
    // =========================================================================
    // =========================================================================
    // VUE ADMIN : PARAMETRES GLOBAUX + CRÉATION RAPIDE TERRAINS
    // =========================================================================
    // Génère une représentation visuelle des terrains (Plan Dynamique)
    // Génère une représentation visuelle des terrains (AVEC STYLE BOÎTE + ACTUALISER)
    private Component createDynamicTerrainMap() {
        // 1. Le conteneur principal (La "Boîte")
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.getStyle()
            .set("border", "1px solid #ddd")
            .set("padding", "20px")
            .set("border-radius", "10px")
            .set("margin-top", "20px") // Espace par rapport au bloc du dessus
            .set("background-color", "white");

        // 2. L'en-tête avec Titre + Bouton Actualiser
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H3 titre = new H3("3.️ Plan des Terrains");
        titre.getStyle().set("margin", "0");

        Button btnRefresh = new Button("Actualiser le plan");
        btnRefresh.addThemeVariants(ButtonVariant.LUMO_SMALL);
        // On ajoute une icône si on veut, ou juste du texte
        
        header.add(titre, btnRefresh);
        mainLayout.add(header);

        // 3. Le conteneur des "Cartes" de terrains
        com.vaadin.flow.component.orderedlayout.FlexLayout mapContainer = new com.vaadin.flow.component.orderedlayout.FlexLayout();
        mapContainer.setFlexWrap(com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap.WRAP);
        mapContainer.getStyle().set("gap", "20px").set("margin-top", "10px");

        // 4. La logique de chargement (mise dans une fonction pour être rappelée)
        Runnable chargerPlan = () -> {
            mapContainer.removeAll();
            try {
                // Récupérer tous les terrains
                List<Terrain> terrains = Terrain.tousLesTerrains(con);
                
                // Récupérer les terrains occupés
                List<Integer> terrainsOccupes = new ArrayList<>();
                String sql = "SELECT ID_TERRAIN FROM matchs WHERE STATUT = 'en_cours'";
                try (java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
                    java.sql.ResultSet rs = pst.executeQuery();
                    while(rs.next()) terrainsOccupes.add(rs.getInt(1));
                }

                if (terrains.isEmpty()) {
                    mapContainer.add(new Span("Aucun terrain n'a été créé."));
                } else {
                    for (Terrain t : terrains) {
                        boolean isBusy = terrainsOccupes.contains(t.getId());
                        
                        // La petite carte visuelle
                        VerticalLayout card = new VerticalLayout();
                        card.setWidth("140px");
                        card.setHeight("100px");
                        card.setAlignItems(Alignment.CENTER);
                        card.setJustifyContentMode(JustifyContentMode.CENTER);
                        
                        String color = isBusy ? "#e74c3c" : "#2ecc71"; // Rouge ou Vert
                        card.getStyle()
                            .set("background-color", color)
                            .set("border-radius", "8px")
                            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)")
                            .set("color", "white");

                        Span nom = new Span(t.getNom());
                        nom.getStyle().set("font-weight", "bold").set("font-size", "1.0em");
                        
                        Span type = new Span("Type: " + ("C".equals(t.getType()) ? "Couvert" : "Ouvert"));
                        type.getStyle().set("font-size", "0.8em");
                        
                        Span status = new Span(isBusy ? "OCCUPÉ" : "LIBRE");
                        status.getStyle().set("font-weight", "bold").set("font-size", "0.8em");

                        card.add(nom, type, status);
                        mapContainer.add(card);
                    }
                }
            } catch (SQLException e) {
                mapContainer.add(new Span("Erreur chargement: " + e.getMessage()));
            }
        };

        // 5. Action du bouton Actualiser
        btnRefresh.addClickListener(e -> {
            chargerPlan.run();
            Notification.show("Plan mis à jour !");
        });

        // Chargement initial
        chargerPlan.run();

        mainLayout.add(mapContainer);
        return mainLayout;
    }
    
    private void showAdminParamsView() {
        contentArea.removeAll();
        contentArea.add(new H2("Configuration du Tournoi"));
        
        try {
            // --- PARTIE 1 : PARAMÈTRES GLOBAUX ---
            VerticalLayout paramsLayout = new VerticalLayout();
            paramsLayout.getStyle()
                .set("border", "1px solid #ddd")
                .set("padding", "20px")
                .set("border-radius", "10px")
                .set("background-color", "white");
            
            H3 titleParams = new H3("1. Règles Globales");
            
            Parametres p = Parametres.load(con);
            
            IntegerField nbT = new IntegerField("Nombre de terrains simultanés");
            nbT.setValue(p.getNbTerrains());
            nbT.setHelperText("Combien de matchs peuvent se jouer en même temps ?");
            
            // --- NOUVEAUTÉ : SÉLECTEUR DE SPORT ---
            // On crée une Map pour associer "Nom du sport" -> "Nb Joueurs"
            java.util.Map<String, Integer> sports = new java.util.LinkedHashMap<>();
            sports.put("Tennis Simple", 1);
            sports.put("Tennis Double", 2);
            sports.put("Padel", 2);
            sports.put("Badminton Simple", 1);
            sports.put("Badminton Double", 2);
            sports.put("Basket 3x3", 3);
            sports.put("Basket Standard", 5);
            sports.put("Volley-ball", 6);
            sports.put("Futsal / Foot à 5", 5);
            sports.put("Football", 11);
            sports.put("Rugby à 7", 7);
            sports.put("Rugby à XV", 15);
            sports.put("Pétanque (Triplette)", 3);

            ComboBox<String> cbSport = new ComboBox<>("Sélection Rapide (Sport)");
            cbSport.setItems(sports.keySet());
            cbSport.setPlaceholder("Choisissez un sport...");
            cbSport.setHelperText("Remplit automatiquement le nombre de joueurs ci-dessous.");
            
            // Le champ numérique classique
            IntegerField nbJ = new IntegerField("Joueurs par Équipe");
            nbJ.setValue(p.getNbJoueursParEquipe());
            nbJ.setHelperText("Vous pouvez modifier cette valeur manuellement.");
            
            // LOGIQUE AUTOMATIQUE : Quand on change le sport, on change le nombre
            cbSport.addValueChangeListener(e -> {
                if (e.getValue() != null) {
                    Integer nb = sports.get(e.getValue());
                    nbJ.setValue(nb);
                    Notification.show("Règle appliquée : " + nb + " joueurs pour " + e.getValue());
                }
            });
            // ---------------------------------------
            
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
            
            paramsLayout.add(titleParams, nbT, cbSport, nbJ, saveParams); // Ajout de cbSport ici
            
            
            // --- PARTIE 2 : CRÉATION RAPIDE DE TERRAIN ---
            VerticalLayout terrainLayout = new VerticalLayout();
            terrainLayout.getStyle()
                .set("border", "1px solid #ddd")
                .set("padding", "20px")
                .set("border-radius", "10px")
                .set("margin-top", "20px")
                .set("background-color", "white");
            
            H3 titleTerrain = new H3("2. Ajouter un Terrain");
            
            HorizontalLayout formTerrain = new HorizontalLayout();
            formTerrain.setAlignItems(Alignment.BASELINE); 
            
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
                    String codeType = typeTerrain.getValue().equals("Couvert") ? "C" : "O";
                    new Terrain(nomTerrain.getValue(), codeType).saveInDB(con);
                    
                    Notification.show("Terrain '" + nomTerrain.getValue() + "' créé !", 
                            3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                    nomTerrain.clear();
                    nomTerrain.focus();
                } catch (Exception ex) {
                    Notification.show("Erreur : " + ex.getMessage())
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            
            formTerrain.add(nomTerrain, typeTerrain, btnAddTerrain);
            terrainLayout.add(titleTerrain, formTerrain);

            contentArea.add(paramsLayout, terrainLayout);
            
        } catch (SQLException e) {
            Notification.show("Erreur chargement params: " + e.getMessage());
        }
        contentArea.add(createDynamicTerrainMap());
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
    // Ouvre une fenêtre modale pour saisir les scores d'un match
    private void openScoreDialog(Matchs m) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Arbitrage - Match " + m.getId());
        dialog.setWidth("800px");

        VerticalLayout mainWrapper = new VerticalLayout();
        HorizontalLayout teamsLayout = new HorizontalLayout();
        teamsLayout.setSizeFull();
        teamsLayout.setJustifyContentMode(JustifyContentMode.EVENLY);

        // Listes pour stocker les références et pouvoir mettre à jour l'affichage
        List<Span> scoreLabels = new ArrayList<>();
        List<Equipe> equipesDuMatch = new ArrayList<>();

        try {
            equipesDuMatch = Equipe.equipesPourMatch(con, m.getId());
            if (equipesDuMatch.size() < 2) {
                Notification.show("Erreur : Match incomplet (pas assez d'équipes)");
                return;
            }

            // --- CONSTRUCTION DE L'AFFICHAGE DES ÉQUIPES ---
            for (Equipe equipe : equipesDuMatch) {
                VerticalLayout teamLayout = new VerticalLayout();
                teamLayout.setAlignItems(Alignment.CENTER);
                teamLayout.getStyle().set("border", "1px solid #eee").set("padding", "10px").set("border-radius", "5px");
                
                Span scoreLabel = new Span("Score: " + equipe.getScore());
                scoreLabel.getStyle().set("font-weight", "bold").set("font-size", "1.5em").set("color", "#2c3e50");
                scoreLabels.add(scoreLabel); // On garde une référence pour update
                
                H3 title = new H3("Équipe " + equipe.getNum());
                teamLayout.add(title, scoreLabel);
                
                List<Joueur> joueurs = equipe.getJoueurs(con);
                for (Joueur joueur : joueurs) {
                    // Bouton +1 existant
                    Button btnMarquer = new Button(joueur.getSurnom() + " (+1)", e -> {
                        try {
                            int newS = equipe.getScore() + 1;
                            equipe.setScore(newS); equipe.updateInDB(con);
                            joueur.setTotalScore(joueur.getTotalScore() + 1); joueur.updateInDB(con);
                            scoreLabel.setText("Score: " + newS);
                        } catch (Exception ex) { Notification.show("Erreur: " + ex.getMessage()); }
                    });
                    btnMarquer.addThemeVariants(ButtonVariant.LUMO_SMALL);
                    teamLayout.add(btnMarquer);
                }
                teamsLayout.add(teamLayout);
            }
            
            // --- BOUTON SCORE ALÉATOIRE ---
            // On doit copier la liste pour l'utiliser dans le lambda (final effective)
            List<Equipe> finalEquipes = equipesDuMatch; 
            
            Button btnRandom = new Button("🎲 Générer Score Aléatoire (0-5)", e -> {
                try {
                    for (int i = 0; i < finalEquipes.size(); i++) {
                        Equipe eq = finalEquipes.get(i);
                        
                        // 1. Génération score (0 à 5)
                        int randomScore = (int)(Math.random() * 6);
                        
                        // 2. Calcul du delta pour mettre à jour les joueurs
                        // (Si score passe de 2 à 5, on ajoute 3 pts à chaque joueur)
                        int delta = randomScore - eq.getScore();
                        
                        // 3. Update Equipe
                        eq.setScore(randomScore);
                        eq.updateInDB(con);
                        
                        // 4. Update Joueurs (Important pour le classement)
                        for (Joueur j : eq.getJoueurs(con)) {
                            j.setTotalScore(j.getTotalScore() + delta);
                            j.updateInDB(con);
                        }
                        
                        // 5. Update Affichage
                        if (i < scoreLabels.size()) {
                            scoreLabels.get(i).setText("Score: " + randomScore);
                        }
                    }
                    Notification.show("Scores aléatoires appliqués !");
                } catch (Exception ex) {
                    Notification.show("Erreur: " + ex.getMessage());
                }
            });
            btnRandom.getStyle().set("margin-top", "20px").set("background-color", "#f1c40f").set("color", "black");


            // --- BOUTON CLÔTURE (Ton code existant) ---
            Button btnClore = new Button("Siffler la fin du match (Clore)", e -> {
                try {
                    m.setStatut("clos");
                    m.updateInDB(con);
                    Joueur.mettreAJourClassementGeneral(con);
                    
                    if (Ronde.tousMatchsRondeClos(con, m.getIdRonde())) {
                         new Ronde(m.getIdRonde(), 0, 0, "").setEtat(con, "close");
                         Notification.show("🏆 Ronde terminée !");
                    } else {
                        Notification.show("Match terminé.");
                    }
                    dialog.close();
                    showMatchsView(); 
                } catch (Exception ex) { Notification.show("Erreur: " + ex.getMessage()); }
            });
            btnClore.addThemeVariants(ButtonVariant.LUMO_ERROR);

            // Assemblage
            mainWrapper.add(teamsLayout, btnRandom, btnClore);
            mainWrapper.setAlignItems(Alignment.CENTER);
            
            dialog.add(mainWrapper);
            dialog.open();

        } catch (SQLException e) {
            Notification.show("Erreur chargement: " + e.getMessage());
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
    
    // --- VUE GLOBALE TOURNOIS (Avec bouton Classement) ---
    private void showVueGlobaleTournoi() {
        contentArea.removeAll();
        contentArea.add(new H2("Tableau de bord des Tournois"));

        try {
            List<Tournoi> tournois = Tournoi.tousLesTournois(con);

            if (tournois.isEmpty()) {
                contentArea.add(new Span("Aucun tournoi n'a été créé pour le moment."));
                return;
            }

            for (Tournoi t : tournois) {
                // --- EN-TÊTE DU TOURNOI ---
                HorizontalLayout headerTournoi = new HorizontalLayout();
                headerTournoi.setAlignItems(Alignment.CENTER);
                headerTournoi.setWidthFull();
                
                H3 titre = new H3(t.getNom());
                titre.getStyle().set("margin", "0");
                
                
                // --- NOUVEAU : BOUTON CLASSEMENT ---
                Button btnClassement = new Button("🏆 Classement", e -> {
                    // Empêche l'accordéon de s'ouvrir/fermer quand on clique sur le bouton
                    e.getSource().getElement().executeJs("event.stopPropagation()");
                    openClassementTournoiDialog(t);
                });
                btnClassement.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                btnClassement.getStyle().set("margin-left", "auto"); // Pousse le bouton à droite
                
                headerTournoi.add(titre, btnClassement);

                // --- CONTENU (RONDES) ---
                VerticalLayout contentTournoi = new VerticalLayout();
                contentTournoi.setPadding(false);

                // Récupération des Rondes
                List<Ronde> rondes = new ArrayList<>();
                try (java.sql.PreparedStatement pst = con.prepareStatement(
                        "SELECT * FROM ronde WHERE ID_TOURNOI = ? ORDER BY NUMERO")) {
                    pst.setInt(1, t.getId());
                    java.sql.ResultSet rs = pst.executeQuery();
                    while(rs.next()) {
                        rondes.add(new Ronde(rs.getInt("ID"), rs.getInt("NUMERO"), 
                                             rs.getInt("ID_TOURNOI"), rs.getString("ETAT")));
                    }
                }

                if (rondes.isEmpty()) {
                    contentTournoi.add(new Span("Aucune ronde générée."));
                } else {
                    for (Ronde r : rondes) {
                        String emoji = "ouverte".equals(r.getEtat()) ? "🟢" : 
                                       "en_cours".equals(r.getEtat()) ? "🔵" : "🔴";
                        Span summaryRonde = new Span(emoji + " Ronde N°" + r.getNumero() + " (" + r.getEtat() + ")");
                        summaryRonde.getStyle().set("font-weight", "bold");

                        VerticalLayout contentRonde = new VerticalLayout();
                        contentRonde.setPadding(false);

                        // Matchs de la ronde
                        List<Matchs> matchsDeLaRonde = new ArrayList<>();
                        try (java.sql.PreparedStatement pstM = con.prepareStatement(
                                "SELECT * FROM matchs WHERE ID_RONDE = ?")) {
                            pstM.setInt(1, r.getId());
                            java.sql.ResultSet rsM = pstM.executeQuery();
                            while(rsM.next()) {
                                matchsDeLaRonde.add(new Matchs(rsM.getInt("ID"), rsM.getInt("ID_RONDE"), 
                                                               rsM.getInt("ID_TERRAIN"), rsM.getString("STATUT")));
                            }
                        }

                        if (!matchsDeLaRonde.isEmpty()) {
                            Grid<Matchs> gridM = new Grid<>(Matchs.class, false);
                            gridM.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_COMPACT);
                            gridM.setAllRowsVisible(true);
                            gridM.addColumn(m -> getTerrainNom(m.getIdTerrain())).setHeader("Terrain").setWidth("150px");
                            gridM.addColumn(m -> getScoreText(m)).setHeader("Affiche & Score").setAutoWidth(true);
                            gridM.addColumn(Matchs::getStatut).setHeader("Statut").setWidth("100px");
                            gridM.setItems(matchsDeLaRonde);
                            contentRonde.add(gridM);
                        } else {
                            contentRonde.add(new Span("Pas de matchs."));
                        }

                        Details detailsRonde = new Details(summaryRonde, contentRonde);
                        detailsRonde.setOpened("en_cours".equals(r.getEtat()));
                        detailsRonde.setWidthFull();
                        detailsRonde.getStyle().set("background-color", "#f8f9fa").set("border", "1px solid #eee")
                                               .set("margin-top", "5px").set("padding", "5px");
                        contentTournoi.add(detailsRonde);
                    }
                }

                Details tournoiAccordion = new Details(headerTournoi, contentTournoi);
                tournoiAccordion.setWidthFull();
                tournoiAccordion.getStyle().set("border", "1px solid #ccc").set("border-radius", "8px")
                                           .set("margin-bottom", "15px").set("padding", "10px");

                contentArea.add(tournoiAccordion);
            }

        } catch (SQLException e) {
            Notification.show("Erreur chargement: " + e.getMessage());
        }
    }
    
    // Ouvre une fenêtre avec le classement spécifique d'un tournoi
    private void openClassementTournoiDialog(Tournoi t) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Classement : " + t.getNom());
        dialog.setWidth("600px");
        
        VerticalLayout layout = new VerticalLayout();
        
        try {
            // Calcul du classement pour ce tournoi
            List<Joueur> classement = calculerClassementPourTournoi(t.getId());
            // Tri décroissant
            classement.sort((j1, j2) -> Integer.compare(j2.getTotalScore(), j1.getTotalScore()));
            
            if (classement.isEmpty()) {
                layout.add(new Span("Aucun point marqué dans ce tournoi pour l'instant."));
            } else {
                Grid<Joueur> grid = new Grid<>(Joueur.class, false);
                grid.addColumn(Joueur::getSurnom).setHeader("Joueur").setAutoWidth(true);
                grid.addColumn(Joueur::getCategorie).setHeader("Cat").setWidth("80px");
                grid.addColumn(Joueur::getTotalScore).setHeader("Points (" + t.getNom() + ")");
                
                grid.setItems(classement);
                layout.add(grid);
            }
        } catch (SQLException e) {
            layout.add(new Span("Erreur calcul : " + e.getMessage()));
        }

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        layout.add(closeBtn);
        layout.setAlignItems(Alignment.END);
        
        dialog.add(layout);
        dialog.open();
    }
    
    // Calcule les points uniquement pour un tournoi donné
    private List<Joueur> calculerClassementPourTournoi(int idTournoi) throws SQLException {
        List<Joueur> resultats = new ArrayList<>();
        String sql = "SELECT j.ID, j.SURNOM, j.CATEGORIE, j.TAILLECM, j.ROLE, " +
                     "       COALESCE(SUM(e.SCORE), 0) as SCORE_TOURNOI " +
                     "FROM joueur j " +
                     "JOIN composition c ON j.ID = c.IDJOUEUR " +
                     "JOIN equipe e ON c.IDEQUIPE = e.ID " +
                     "JOIN matchs m ON e.IDMATCH = m.ID " +
                     "JOIN ronde r ON m.ID_RONDE = r.ID " +
                     "WHERE r.ID_TOURNOI = ? " +
                     "GROUP BY j.ID, j.SURNOM, j.CATEGORIE, j.TAILLECM, j.ROLE";

        try (java.sql.PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idTournoi);
            try (java.sql.ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Joueur j = new Joueur(
                        rs.getInt("ID"),
                        rs.getString("SURNOM"),
                        rs.getString("CATEGORIE"),
                        rs.getObject("TAILLECM") != null ? rs.getInt("TAILLECM") : 0,
                        rs.getString("ROLE"),
                        rs.getInt("SCORE_TOURNOI") // Le score du tournoi
                    );
                    resultats.add(j);
                }
            }
        }
        return resultats;
    }

    // Petit helper pour formater le texte du match "Equipe X (Score) vs Equipe Y (Score)"
    private String getScoreText(Matchs m) {
        try {
            List<Equipe> eqs = Equipe.equipesPourMatch(con, m.getId());
            if (eqs.size() >= 2) {
                Equipe e1 = eqs.get(0);
                Equipe e2 = eqs.get(1);
                // Format : "Equipe 1 (12 pts)  ⚡  Equipe 2 (8 pts)"
                return "Éq." + e1.getNum() + " (" + e1.getScore() + ")  vs  " + 
                       "Éq." + e2.getNum() + " (" + e2.getScore() + ")";
            }
            return "En attente d'équipes...";
        } catch (Exception e) {
            return "Erreur lecture";
        }
    }
    
    private void openManualMatchCreationDialog(int idRonde) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Création de Match Manuel");
        dialog.setWidth("1000px");

        VerticalLayout mainLayout = new VerticalLayout();

        // 1. Déclaration des composants (vides pour l'instant)
        ComboBox<Terrain> cbTerrain = new ComboBox<>("Terrain (Libres)");
        cbTerrain.setItemLabelGenerator(Terrain::getNom);
        cbTerrain.setWidth("50%");

        // Les listes de sélection de joueurs
        MultiSelectComboBox<Joueur> mainEq1 = new MultiSelectComboBox<>("Équipe 1 - Titulaires");
        MultiSelectComboBox<Joueur> subEq1 = new MultiSelectComboBox<>("Remplaçants Eq 1");
        MultiSelectComboBox<Joueur> mainEq2 = new MultiSelectComboBox<>("Équipe 2 - Titulaires");
        MultiSelectComboBox<Joueur> subEq2 = new MultiSelectComboBox<>("Remplaçants Eq 2");

        // Configuration affichage
        mainEq1.setItemLabelGenerator(Joueur::getSurnom); subEq1.setItemLabelGenerator(Joueur::getSurnom);
        mainEq2.setItemLabelGenerator(Joueur::getSurnom); subEq2.setItemLabelGenerator(Joueur::getSurnom);
        mainEq1.setWidthFull(); subEq1.setWidthFull(); mainEq2.setWidthFull(); subEq2.setWidthFull();

        // --- LOGIQUE INTELLIGENTE DE RAFRAÎCHISSEMENT ---
        // Cette fonction (Runnable) sera appelée à l'ouverture ET après chaque création de match
        Runnable refreshData = () -> {
            try {
                // A. Mise à jour des Terrains (On ne veut que les libres)
                List<Terrain> terrainsLibres = Terrain.terrainsDisponibles(con);
                cbTerrain.setItems(terrainsLibres);
                cbTerrain.clear(); // On décoche
                if (terrainsLibres.isEmpty()) cbTerrain.setHelperText("⚠️ Aucun terrain disponible !");
                else cbTerrain.setHelperText(terrainsLibres.size() + " terrains libres.");

                // B. Mise à jour des Joueurs (Exclure ceux DÉJÀ dans cette ronde)
                // 1. On trouve les IDs des joueurs déjà occupés dans la ronde actuelle
                List<Integer> idsOccupes = new ArrayList<>();
                String sqlCheck = "SELECT c.IDJOUEUR FROM composition c " +
                                  "JOIN equipe e ON c.IDEQUIPE = e.ID " +
                                  "JOIN matchs m ON e.IDMATCH = m.ID " +
                                  "WHERE m.ID_RONDE = ?"; // Filtre par la ronde en cours
                
                try (java.sql.PreparedStatement pst = con.prepareStatement(sqlCheck)) {
                    pst.setInt(1, idRonde);
                    java.sql.ResultSet rs = pst.executeQuery();
                    while(rs.next()) idsOccupes.add(rs.getInt(1));
                }

                // 2. On filtre la liste complète
                List<Joueur> tous = Joueur.tousLesJoueurs(con);
                List<Joueur> dispos = new ArrayList<>();
                for (Joueur j : tous) {
                    if (!idsOccupes.contains(j.getId())) {
                        dispos.add(j);
                    }
                }

                // 3. On met à jour les listes déroulantes
                mainEq1.setItems(dispos); subEq1.setItems(dispos);
                mainEq2.setItems(dispos); subEq2.setItems(dispos);
                
                // On vide les sélections précédentes
                mainEq1.clear(); subEq1.clear(); mainEq2.clear(); subEq2.clear();

            } catch (SQLException ex) {
                Notification.show("Erreur rechargement données: " + ex.getMessage());
            }
        };

        // Appel initial pour remplir les listes
        refreshData.run();

        // --- VALIDATION CROISÉE (Ton code existant pour éviter doublons Eq1 vs Eq2) ---
        HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<MultiSelectComboBox<Joueur>, Set<Joueur>>> validationListener = event -> {
            if (!event.isFromClient()) return;
            MultiSelectComboBox<Joueur> source = event.getSource();
            Set<Joueur> selection = event.getValue();
            
            // Paramètres globaux pour limite joueurs
            int maxJ = 2; // Valeur par défaut de sécurité
            try { maxJ = Parametres.load(con).getNbJoueursParEquipe(); } catch(Exception e){}

            // Vérif limite taille
            if ((source == mainEq1 || source == mainEq2) && selection.size() > maxJ) {
                source.setValue(event.getOldValue());
                Notification.show("Max " + maxJ + " titulaires !");
                return;
            }
            
            // Vérif doublons entre les listes
            List<Joueur> autres = new ArrayList<>();
            if (source != mainEq1) autres.addAll(mainEq1.getValue());
            if (source != subEq1)  autres.addAll(subEq1.getValue());
            if (source != mainEq2) autres.addAll(mainEq2.getValue());
            if (source != subEq2)  autres.addAll(subEq2.getValue());

            for (Joueur j : selection) {
                if (autres.contains(j)) {
                    source.setValue(event.getOldValue());
                    Notification.show(j.getSurnom() + " est déjà sélectionné ailleurs !");
                    return;
                }
            }
        };

        mainEq1.addValueChangeListener(validationListener); subEq1.addValueChangeListener(validationListener);
        mainEq2.addValueChangeListener(validationListener); subEq2.addValueChangeListener(validationListener);

        // --- LES BOUTONS ---
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        
        // Bouton 1 : Classique
        Button btnSaveClose = new Button("Valider & Fermer", e -> {
            if (saveMatchHelper(idRonde, cbTerrain, mainEq1, subEq1, mainEq2, subEq2)) {
                dialog.close();
            }
        });
        btnSaveClose.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Bouton 2 : NOUVEAU
        Button btnSaveContinue = new Button("Valider & Créer un autre", e -> {
            if (saveMatchHelper(idRonde, cbTerrain, mainEq1, subEq1, mainEq2, subEq2)) {
                Notification.show("✅ Match créé ! Prêt pour le suivant...", 3000, Notification.Position.MIDDLE);
                // C'est ici la magie : on recharge tout sans fermer
                refreshData.run(); 
            }
        });
        btnSaveContinue.addThemeVariants(ButtonVariant.LUMO_CONTRAST); // Couleur différente

        buttonsLayout.add(btnSaveClose, btnSaveContinue);

        // Mise en page
        HorizontalLayout row1 = new HorizontalLayout(mainEq1, subEq1); row1.setWidthFull();
        HorizontalLayout row2 = new HorizontalLayout(mainEq2, subEq2); row2.setWidthFull();

        mainLayout.add(cbTerrain, new H3("Équipe 1"), row1, new H3("Équipe 2"), row2, buttonsLayout);
        dialog.add(mainLayout);
        dialog.open();
    }

    // Petite méthode aide pour éviter de copier-coller la logique de sauvegarde
    private boolean saveMatchHelper(int idRonde, ComboBox<Terrain> cb, 
                                    MultiSelectComboBox<Joueur> m1, MultiSelectComboBox<Joueur> s1,
                                    MultiSelectComboBox<Joueur> m2, MultiSelectComboBox<Joueur> s2) {
        if (cb.getValue() == null) { Notification.show("Choisissez un terrain !"); return false; }
        if (m1.getValue().isEmpty() || m2.getValue().isEmpty()) { Notification.show("Équipes vides !"); return false; }

        try {
            List<Joueur> eq1 = new ArrayList<>(m1.getValue()); eq1.addAll(s1.getValue());
            List<Joueur> eq2 = new ArrayList<>(m2.getValue()); eq2.addAll(s2.getValue());
            
            // Sauvegarde BDD
            ServiceTournoi.creerMatchManuel(con, idRonde, cb.getValue().getId(), eq1, eq2);
            return true;
        } catch (Exception ex) {
            Notification.show("Erreur: " + ex.getMessage());
            return false;
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