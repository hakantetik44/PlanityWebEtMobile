Feature: Recherche de coiffeur

  Scenario: Rechercher un coiffeur via menu et recherche directe
    Given Je lance l'application
    When Je clique sur le lien "Coiffeur" dans le menu
    And Je saisis "Paris" dans la recherche
    And Je clique sur le bouton "Rechercher"
    Then Je devrais voir une liste de coiffeurs à Paris