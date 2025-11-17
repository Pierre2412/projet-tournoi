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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

// Imports de votre package model
import fr.insa.beuvron.utils.database.ConnectionSimpleSGBD;
import fr.insa.toto.model.Equipe;
import fr.insa.toto.model.Joueur;
import fr.insa.toto.model.Matchs;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Route(value = "")
@PageTitle("Gestion Tournoi")
public class VuePrincipale extends VerticalLayout {

    // ----- COMPOSANTS PRINCIPAUX -----
    private Tabs tabs;
    private Tab tabJoueurs;
    private Tab tabEquipes;
    private Tab tabBdD;
    
    private VerticalLayout contentJoueurs;
    private VerticalLayout contentEquipes;
    private VerticalLayout contentBdD;
    
    // ----- COMPOSANTS DE L'ONGLET JOUEURS -----
    private Grid<Joueur> gridJoueurs;
    private Button btnAjouterJoueur;
    private Button btnModifierJoueur;
    private Button btnSupprimerJoueur;

    // ----- COMPOSANTS DE L'ONGLET EQUIPES -----
    private Grid<Equipe> gridEquipes;
    private Grid<Joueur> gridComposition;
    private Button btnAjouterEquipe;
    private Button btnAjouterJoueurAEquipe;
    private H2 h2CompositionTitre;

    public VuePrincipale() {
        // En-tête
        add(new H1("Projet Tournoi - Gestion"));

        // 1. Créer les onglets
        tabJoueurs = new Tab("Joueurs");
        tabEquipes = new Tab("Équipes");
        tabBdD = new Tab("Base de Données");
        tabs = new Tabs(tabJoueurs, tabEquipes, tabBdD);

        // 2. Créer les conteneurs pour le contenu de chaque onglet
        contentJoueurs = new VerticalLayout();
        contentEquipes = new VerticalLayout();
        contentBdD = new VerticalLayout(new H2("TODO: Menu Gestion BdD")); // Contenu à faire

        // Mappage des onglets à leur contenu
        Map<Tab, VerticalLayout> tabsToPages = new HashMap<>();
        tabsToPages.put(tabJoueurs, contentJoueurs);
        tabsToPages.put(tabEquipes, contentEquipes);
        tabsToPages.put(tabBdD, contentBdD);

        // 3. Gérer l'affichage des onglets
        // Cacher tous les contenus sauf celui des joueurs au début
        contentEquipes.setVisible(false);
        contentBdD.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            // Cacher tout
            tabsToPages.values().forEach(content -> content.setVisible(false));
            // Afficher le bon
            VerticalLayout contentToShow = tabsToPages.get(tabs.getSelectedTab());
            if (contentToShow != null) {
                contentToShow.setVisible(true);
            }
        });

        // 4. Construire les interfaces des onglets
        buildUiJoueurs();
        buildUiEquipes();

        // 5. Ajouter les composants à la vue principale
        add(tabs, contentJoueurs, contentEquipes, contentBdD);
        
        // 6. Charger les données initiales
        refreshGridJoueurs();
        refreshGridEquipes();
    }

    // ==================================================================
    //                  MÉTHODES DE L'ONGLET "JOUEURS"
    // ==================================================================

    /**
     * Construit l'interface de l'onglet "Joueurs"
     * (Équivalent de votre "menuJoueurs")
     */
    private void buildUiJoueurs() {
        // 1. Les Boutons
        btnAjouterJoueur = new Button("Ajouter Joueur", VaadinIcon.PLUS.create());
        btnModifierJoueur = new Button("Modifier", VaadinIcon.PENCIL.create());
        btnSupprimerJoueur = new Button("Supprimer", VaadinIcon.TRASH.create());

        // Désactiver les boutons de modification/suppression au début
        btnModifierJoueur.setEnabled(false);
        btnSupprimerJoueur.setEnabled(false);

        HorizontalLayout boutonsLayout = new HorizontalLayout(btnAjouterJoueur, btnModifierJoueur, btnSupprimerJoueur);
        
        // 2. La Grille (votre "listerJoueurs")
        gridJoueurs = new Grid<>(Joueur.class);
        gridJoueurs.setColumns("id", "surnom", "categorie", "tailleCm");
        gridJoueurs.getColumnByKey("tailleCm").setHeader("Taille (cm)"); // Renommer colonne

        // 3. Logique de sélection
        gridJoueurs.addSelectionListener(select -> {
            boolean isSelected = !gridJoueurs.getSelectedItems().isEmpty();
            btnModifierJoueur.setEnabled(isSelected);
            btnSupprimerJoueur.setEnabled(isSelected);
        });

        // 4. Logique des boutons
        btnAjouterJoueur.addClickListener(e -> openJoueurDialog(null));
        btnModifierJoueur.addClickListener(e -> openJoueurDialog(gridJoueurs.asSingleSelect().getValue()));
        btnSupprimerJoueur.addClickListener(e -> supprimerJoueur());

        // 5. Ajouter les composants à l'onglet "Joueurs"
        contentJoueurs.add(boutonsLayout, gridJoueurs);
    }

    /**
     * Ouvre le formulaire (Dialog) pour ajouter ou modifier un joueur.
     * Si 'joueur' est null, c'est une création (votre "ajouterJoueur").
     * Si 'joueur' n'est pas null, c'est une modification (votre "modifierTailleJoueur").
     */
    private void openJoueurDialog(Joueur joueur) {
        Dialog dialog = new Dialog();
        FormLayout form = new FormLayout();
        
        // 1. Les champs
        TextField fieldSurnom = new TextField("Surnom");
        TextField fieldCategorie = new TextField("Catégorie (J, S, ...)");
        IntegerField fieldTaille = new IntegerField("Taille (cm)");

        if (joueur != null) {
            // Mode Modification
            dialog.setHeaderTitle("Modifier Joueur: " + joueur.getSurnom());
            fieldSurnom.setValue(joueur.getSurnom());
            fieldCategorie.setValue(joueur.getCategorie() != null ? joueur.getCategorie() : "");
            // Gère le cas où la taille est null
            fieldTaille.setValue(joueur.getTailleCm() != null ? joueur.getTailleCm() : 0);
        } else {
            // Mode Ajout
            dialog.setHeaderTitle("Nouveau Joueur");
            fieldTaille.setValue(0); // Valeur par défaut
        }

        form.add(fieldSurnom, fieldCategorie, fieldTaille);
        
        // 2. Les boutons du formulaire
        Button btnSave = new Button("Enregistrer");
        Button btnCancel = new Button("Annuler", e -> dialog.close());
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnSave.addClickShortcut(Key.ENTER);

        dialog.add(form, new HorizontalLayout(btnSave, btnCancel));
        
        // 3. Logique d'enregistrement (appel à votre backend)
        btnSave.addClickListener(e -> {
            String surnom = fieldSurnom.getValue();
            String categorie = fieldCategorie.getValue();
            Integer taille = fieldTaille.getValue();

            // Validation
            if (surnom == null || surnom.trim().isEmpty()) {
                Notification.show("Le surnom est obligatoire !");
                return;
            }
            // Gérer les nulls comme dans votre console
            if (categorie != null && categorie.trim().isEmpty()) categorie = null;
            if (taille != null && taille <= 0) taille = null;

            try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
                if (joueur == null) {
                    // Création (votre "ajouterJoueur")
                    Joueur nouveau = new Joueur(surnom, categorie, taille);
                    nouveau.saveInDB(con); // Appel à Joueur.saveInDB
                    Notification.show("Joueur '" + surnom + "' créé !");
                } else {
                    // Modification (votre "modifierTailleJoueur")
                    joueur.setSurnom(surnom); 
                    joueur.setCategorie(categorie);
                    joueur.setTailleCm(taille);
                    joueur.updateInDB(con); // Appel à Joueur.updateInDB
                    Notification.show("Joueur '" + surnom + "' mis à jour !");
                }
                
                // Rafraîchir la grille et fermer
                refreshGridJoueurs();
                dialog.close();

            } catch (SQLException ex) {
                Notification.show("Erreur BDD: " + ex.getMessage());
            }
        });
        
        dialog.open();
    }

    /**
     * Logique de suppression (votre "supprimerJoueur")
     */
    private void supprimerJoueur() {
        Joueur joueur = gridJoueurs.asSingleSelect().getValue();
        if (joueur == null) return;

        // On peut ajouter une confirmation
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Confirmer la suppression");
        confirmDialog.add("Voulez-vous vraiment supprimer " + joueur.getSurnom() + " ?");
        
        Button btnConfirm = new Button("Supprimer", VaadinIcon.TRASH.create(), e -> {
            try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
                joueur.deleteInDB(con); // Appel à Joueur.deleteInDB
                Notification.show("Joueur supprimé.");
                refreshGridJoueurs();
                confirmDialog.close();
            } catch (SQLException ex) {
                Notification.show("Erreur BDD: " + ex.getMessage());
            }
        });
        btnConfirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button btnCancel = new Button("Annuler", e -> confirmDialog.close());
        
        confirmDialog.add(new HorizontalLayout(btnConfirm, btnCancel));
        confirmDialog.open();
    }

    /**
     * Met à jour la grille des joueurs (votre "listerJoueurs")
     */
    private void refreshGridJoueurs() {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            gridJoueurs.setItems(Joueur.tousLesJoueurs(con)); // Appel à Joueur.tousLesJoueurs
        } catch (SQLException ex) {
            Notification.show("Erreur BDD: " + ex.getMessage());
        }
    }

    // ==================================================================
    //                  MÉTHODES DE L'ONGLET "EQUIPES"
    // ==================================================================

    /**
     * Construit l'interface de l'onglet "Équipes"
     * (Équivalent de votre "menuEquipes")
     */
    private void buildUiEquipes() {
        // 1. Les Boutons
        btnAjouterEquipe = new Button("Ajouter Équipe", VaadinIcon.PLUS.create());
        btnAjouterJoueurAEquipe = new Button("Ajouter Joueur...", VaadinIcon.USER.create());
        btnAjouterJoueurAEquipe.setEnabled(false); // Désactivé au début

        HorizontalLayout boutonsLayout = new HorizontalLayout(btnAjouterEquipe, btnAjouterJoueurAEquipe);

        // 2. La Grille des Équipes (votre "listerEquipes")
        gridEquipes = new Grid<>(Equipe.class);
        gridEquipes.setColumns("id", "num", "score", "idMatch");
        gridEquipes.getColumnByKey("idMatch").setHeader("Match ID");

        // 3. La Grille de Composition (vide au début)
        h2CompositionTitre = new H2("Composition de l'équipe (sélectionnez une équipe)");
        gridComposition = new Grid<>(Joueur.class);
        gridComposition.setColumns("id", "surnom", "categorie");
        
        // 4. Logique de sélection
        gridEquipes.addSelectionListener(select -> {
            Equipe selected = gridEquipes.asSingleSelect().getValue();
            if (selected != null) {
                h2CompositionTitre.setText("Composition de l'équipe " + selected.getId());
                btnAjouterJoueurAEquipe.setEnabled(true);
                refreshGridComposition(selected); // Met à jour la 2ème grille
            } else {
                h2CompositionTitre.setText("Composition de l'équipe (sélectionnez une équipe)");
                btnAjouterJoueurAEquipe.setEnabled(false);
                gridComposition.setItems(Collections.emptyList()); // Vide la 2ème grille
            }
        });

        // 5. Logique des boutons
        btnAjouterEquipe.addClickListener(e -> openEquipeDialog());
        btnAjouterJoueurAEquipe.addClickListener(e -> {
            Equipe selected = gridEquipes.asSingleSelect().getValue();
            if (selected != null) {
                openAjouterJoueurDialog(selected); // Ouvre le dialogue d'ajout de joueur
            }
        });

        // 6. Ajouter les composants à l'onglet "Équipes"
        contentEquipes.removeAll(); // Vider le "TODO"
        contentEquipes.add(boutonsLayout, gridEquipes, h2CompositionTitre, gridComposition);
    }

    /**
     * Ouvre le formulaire (Dialog) pour ajouter une équipe.
     * (Équivalent de votre "ajouterEquipe")
     */
    private void openEquipeDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nouvelle Équipe");
        FormLayout form = new FormLayout();
        
        // 1. Les champs
        IntegerField fieldNum = new IntegerField("Numéro Équipe");
        IntegerField fieldScore = new IntegerField("Score Initial");
        ComboBox<Matchs> comboMatch = new ComboBox<>("Match");

        // Remplir le ComboBox en appelant le backend
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            // (Nécessite d'ajouter "tousLesMatchs" à Matchs.java)
            comboMatch.setItems(Matchs.tousLesMatchs(con)); 
        } catch (SQLException ex) {
            Notification.show("Erreur: Impossible de charger les matchs.");
        }
        
        fieldScore.setValue(0); // Score par défaut
        form.add(fieldNum, fieldScore, comboMatch);
        
        // 2. Boutons du formulaire
        Button btnSave = new Button("Enregistrer");
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button btnCancel = new Button("Annuler", e -> dialog.close());

        dialog.add(form, new HorizontalLayout(btnSave, btnCancel));

        // 3. Logique d'enregistrement (appel à votre backend)
        btnSave.addClickListener(e -> {
            Integer num = fieldNum.getValue();
            Integer score = fieldScore.getValue();
            Matchs match = comboMatch.getValue();
            
            if (num == null || score == null || match == null) {
                Notification.show("Tous les champs sont obligatoires.");
                return;
            }

            try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
                Equipe nouvelle = new Equipe(num, score, match.getId());
                nouvelle.saveInDB(con); // Appel à Equipe.saveInDB
                Notification.show("Équipe " + nouvelle.getId() + " créée !");
                refreshGridEquipes();
                dialog.close();
            } catch (SQLException ex) {
                Notification.show("Erreur BDD: " + ex.getMessage());
            }
        });
        
        dialog.open();
    }

    /**
     * Ouvre le dialogue pour ajouter un joueur à une équipe sélectionnée.
     * (Équivalent de votre "ajouterJoueurAEquipe")
     */
    private void openAjouterJoueurDialog(Equipe equipe) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Ajouter Joueur à l'Équipe " + equipe.getId());
        FormLayout form = new FormLayout();

        ComboBox<Joueur> comboJoueur = new ComboBox<>("Joueur à ajouter");
        
        // Remplir le ComboBox avec TOUS les joueurs
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            comboJoueur.setItems(Joueur.tousLesJoueurs(con)); // Appel à Joueur.tousLesJoueurs
            comboJoueur.setItemLabelGenerator(Joueur::getSurnom); // Montre le surnom
        } catch (SQLException ex) {
            Notification.show("Erreur: Impossible de charger les joueurs.");
        }
        
        form.add(comboJoueur);
        
        Button btnAdd = new Button("Ajouter");
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button btnCancel = new Button("Annuler", e -> dialog.close());

        dialog.add(form, new HorizontalLayout(btnAdd, btnCancel));

        // Logique d'ajout
        btnAdd.addClickListener(e -> {
            Joueur joueur = comboJoueur.getValue();
            if (joueur == null) {
                Notification.show("Veuillez sélectionner un joueur.");
                return;
            }

            try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
                // Appel à la méthode de Equipe.java
                equipe.ajouterJoueur(con, joueur.getId()); // Appel à Equipe.ajouterJoueur
                Notification.show(joueur.getSurnom() + " ajouté à l'équipe " + equipe.getId());
                refreshGridComposition(equipe); // Rafraîchir la grille de compo
                dialog.close();
            } catch (SQLException ex) {
                // Gérer les doublons (la clé primaire de 'composition' est (IDEQUIPE, IDJOUEUR))
                if (ex.getMessage().contains("Duplicate entry")) {
                    Notification.show(joueur.getSurnom() + " est déjà dans cette équipe !");
                } else {
                    Notification.show("Erreur BDD: " + ex.getMessage());
                }
            }
        });
        
        dialog.open();
    }

    /**
     * Met à jour la grille des équipes (votre "listerEquipes")
     */
    private void refreshGridEquipes() {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            gridEquipes.setItems(Equipe.tousLesEquipes(con)); // Appel à Equipe.tousLesEquipes
        } catch (SQLException ex) {
            Notification.show("Erreur BDD: " + ex.getMessage());
        }
    }

    /**
     * Met à jour la grille de composition pour l'équipe donnée
     */
    private void refreshGridComposition(Equipe equipe) {
        try (Connection con = ConnectionSimpleSGBD.defaultCon()) {
            // (Nécessite d'ajouter "getJoueurs" à Equipe.java)
            gridComposition.setItems(equipe.getJoueurs(con)); 
        } catch (SQLException ex) {
            Notification.show("Erreur BDD: " + ex.getMessage());
        }
    }
}