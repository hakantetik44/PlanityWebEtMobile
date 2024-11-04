# **PlanityWebEtMobile**

## **Planity Web et Mobile Testing**

## **Table des Matières**
- [Aperçu du Projet](#aperçu-du-projet)
- [Fonctionnalités](#fonctionnalités)
- [Technologies Utilisées](#technologies-utilisées)
- [Prise en Main](#prise-en-main)
    - [Prérequis](#prérequis)
    - [Installation](#installation)
    - [Configuration](#configuration)
- [Exécution des Tests](#exécution-des-tests)
- [Structure des Tests](#structure-des-tests)
- [Rapports](#rapports)
- [Contributions](#contributions)
- [Licence](#licence)

## **Aperçu du Projet**

Planity est une application web et mobile conçue pour faciliter la recherche et la réservation de coiffeurs. Ce dépôt contient des tests automatisés qui valident les fonctionnalités de l'application Planity en utilisant Cucumber pour le développement orienté comportement (BDD), Selenium pour les tests web, et Appium pour les tests mobiles.

## **Fonctionnalités**

Les fonctionnalités suivantes sont mises en œuvre dans l'application Planity :

- 🔍 **Recherche de Coiffeurs** : Les utilisateurs peuvent rechercher des coiffeurs dans des emplacements spécifiques.
- 📅 **Gestion des Réservations** : Les utilisateurs peuvent gérer leurs réservations avec des coiffeurs.
- 🔐 **Inscription et Authentification des Utilisateurs** : Les utilisateurs peuvent s'inscrire et se connecter à l'application.
- 👤 **Gestion du Profil Utilisateur** : Les utilisateurs peuvent consulter et mettre à jour leur profil.

# **PlanityWebEtMobile**

## **Planity Web et Mobile Testing**

## **Table des Matières**
- [Aperçu du Projet](#aperçu-du-projet)
- [Fonctionnalités](#fonctionnalités)
- [Technologies Utilisées](#technologies-utilisées)
- [Prise en Main](#prise-en-main)
    - [Prérequis](#prérequis)
    - [Installation](#installation)
    - [Configuration](#configuration)
- [Exécution des Tests](#exécution-des-tests)
- [Structure des Tests](#structure-des-tests)
- [Rapports](#rapports)
- [Contributions](#contributions)
- [Licence](#licence)

## **Aperçu du Projet**

Planity est une application web et mobile conçue pour faciliter la recherche et la réservation de coiffeurs. Ce dépôt contient des tests automatisés qui valident les fonctionnalités de l'application Planity en utilisant Cucumber pour le développement orienté comportement (BDD), Selenium pour les tests web, et Appium pour les tests mobiles.

## **Fonctionnalités**

Les fonctionnalités suivantes sont mises en œuvre dans l'application Planity :

- 🔍 **Recherche de Coiffeurs** : Les utilisateurs peuvent rechercher des coiffeurs dans des emplacements spécifiques.
- 📅 **Gestion des Réservations** : Les utilisateurs peuvent gérer leurs réservations avec des coiffeurs.
- 🔐 **Inscription et Authentification des Utilisateurs** : Les utilisateurs peuvent s'inscrire et se connecter à l'application.
- 👤 **Gestion du Profil Utilisateur** : Les utilisateurs peuvent consulter et mettre à jour leur profil.

### **Exemple de Fonctionnalité : Recherche de Coiffeur**

```gherkin
Feature: Recherche de coiffeur

  Scenario: Rechercher un coiffeur via menu et recherche directe
    Given Je lance l'application
    When Je clique sur le lien "Coiffeur" dans le menu
    Then Je devrais voir une liste de coiffeurs à Paris

## **Planity Web et Mobile Testing**


## **Aperçu du Projet**

Planity est une application web et mobile conçue pour faciliter la recherche et la réservation de coiffeurs. Ce dépôt contient des tests automatisés qui valident les fonctionnalités de l'application Planity en utilisant Cucumber pour le développement orienté comportement (BDD), Selenium pour les tests web, et Appium pour les tests mobiles.

## **Fonctionnalités**

Les fonctionnalités suivantes sont mises en œuvre dans l'application Planity :

- 🔍 **Recherche de Coiffeurs** : Les utilisateurs peuvent rechercher des coiffeurs dans des emplacements spécifiques.
- 📅 **Gestion des Réservations** : Les utilisateurs peuvent gérer leurs réservations avec des coiffeurs.
- 🔐 **Inscription et Authentification des Utilisateurs** : Les utilisateurs peuvent s'inscrire et se connecter à l'application.
- 👤 **Gestion du Profil Utilisateur** : Les utilisateurs peuvent consulter et mettre à jour leur profil.

# **PlanityWebEtMobile**

## **Planity Web et Mobile Testing**
Contributions
Les contributions sont les bienvenues ! Si vous avez des suggestions d'améliorations ou de fonctionnalités, veuillez créer un problème ou soumettre une demande de tirage.Technologies Utilisées
☕ Java 17 : Langage de programmation utilisé pour le développement des tests.
⚙️ Maven : Outil d'automatisation de construction pour gérer les dépendances du projet et le construire.
📜 Cucumber : Cadre de test BDD pour définir des cas de test en langage naturel.
🌐 Selenium : Outil pour automatiser les applications web à des fins de test.
📱 Appium : Outil pour automatiser les applications mobiles.
🧪 JUnit : Cadre de test utilisé pour exécuter des tests unitaires.
📊 Allure : Outil de rapport pour générer des rapports de test.
📜 Log4j & SLF4J : Cadres de journalisation utilisés pour enregistrer les événements de l'application.
Prise en Main
Prérequis
JDK 17 ou supérieur
Apache Maven
Connexion Internet pour le téléchargement des dépendances
Installation
Clonez le dépôt :


git clone https://github.com/hakantetik44/RadioFranceWebAndMobil.git
cd RadioFranceWebAndMobil
Installez les dépendances Maven :

mvn clean install
Configuration
Assurez-vous de configurer les paramètres de l'application dans le fichier src/test/resources/environment.properties ou tout autre fichier de configuration spécifié dans pom.xml. Ajustez les URL et les paramètres d'environnement selon vos besoins.

Exécution des Tests
Pour exécuter les tests, utilisez la commande Maven suivante :


mvn test
Cette commande compilera les tests et les exécutera selon les spécifications définies dans le répertoire src/test/java.

Structure des Tests
Les tests sont organisés de la manière suivante :

📜 Définitions de Pas : Situées dans src/test/java/stepdefinitions, ces classes contiennent les définitions des pas qui correspondent à la syntaxe Gherkin utilisée dans les fichiers de fonctionnalité.
📁 Fichiers de Fonctionnalité : Situés dans src/test/resources/features, ces fichiers définissent les scénarios et les résultats attendus en utilisant le langage Gherkin.
🖥️ Modèle d'Objet de Page : Situé dans src/test/java/pages, ces classes représentent les pages web et mobiles et contiennent des méthodes pour interagir avec les éléments de l'interface utilisateur.
Rapports
Les résultats des tests seront générés dans le répertoire target/allure-results après l'exécution des tests. Pour afficher le rapport, exécutez :
mvn allure:serve
Cela démarrera un serveur web pour visualiser les rapports de test.

 