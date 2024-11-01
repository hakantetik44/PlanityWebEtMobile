Feature: Filtrer les coiffeurs

  Scenario: Sélectionner des coiffeurs selon la disponibilité et le tri
    Given Je lance l'application
    When Je recherche "Coiffeur" et je saisis "Paris"
    And Je clique sur le bouton "Recherche"
    Then Je devrais voir une liste de coiffeurs à Paris
    When Je choisis une option dans la disponibilité
    And Je sélectionne une option dans le tri
    Then Je devrais voir les coiffeurs filtrés selon mes choix