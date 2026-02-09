# Requirements specyfication (sprint 1)

## _Selected Card Game_

The selected game for this project is Blackjack, a classic card game that has been a popular feature in casinos worldwide for decades. Known for its simple yet strategic gameplay, Blackjack involves players competing against the dealer to have a hand value as close to 21 as possible, without exceeding it. The game’s core mechanics—drawing cards, hitting or standing, and calculating hand values—make it engaging for both beginners and experienced players. For this project, we will focus on building the game’s structure and logic, creating a seamless experience where players can place bets, draw cards, and aim to beat the dealer while keeping track of the game state and player balances.

# 1. Introduction

The Card Game is an online multiplayer application that allows players to join matches, manage decks, and track their results.
 Administrators can manage tournaments, rules, and players.

## 1.1. Purpose of the document

The purpose of this document is to define the functional and non-functional requirements of the Black Jack software project.
 It is intended for developers, testers, and instructors acting as stakeholders in the project

## 1.2. Scope

- Provide a digital platform to play the selected card game according to its rules
- Record all moves and game states for each match
- Maintain player rankings and statistics

## 1.3. Definitions, Acronyms and Abbreviations

	Term 		Definition
	- AI		Artificial Intelligence			
	- SRS		Software Requirements Specification
	- GUI		Graphical User Interface
	- User		A player registred in the platrform 
	- Admin		A system administrator managing tournaments and rules

## 1.4. References

- Sommerville, Software Engineering, 9th Edition, Addiso-Wesley, 2011
- IEEE, SWEBOK: Guide to the Software Engineering Body of Knowledge, 2004
- Pressman, Software Engineering: A Practitioner's Approach, McGraw-Hill, 2001
- UPV document: L3_Lab_Project - Card Game (2025)

## 1.5. Overview

The rest of this document desccribes the product overview, user characteristics, system requirements, and diagrams that model system interactions.

# 2. General Description

## 2.1. Product Persoecctive

The system is a standalone software product accessible via a graphical interface. It will include two main users roles:

- Admin: manages rules, tournaments and players.
- Player: joins matches, plays games, and manage bank.
- AI player: system-controlled, may simulate opponents.

## 2.2. Product Functions

- User registration and authentication
- Game creation and joining
- Deck and rule managment 
- Real-time validation od players moves 
- Recording and resuming saved games
- Ranking and statistics visualization
- Play against AI (optional)

## 2.3. User Characteristics 

- Admin: intermedite computer knowledge, manages game setup
- Player: basic computer knowledge, interacts mainly trough GUI

## 2.4. General Constraints

- The system will rely on GitHub for version control
- The server must store games persistently
- Averge networ latency < 2 seconds per move
- The system must support multiple concurrent matches

## 2.5. Assumptions and Dependencies

- All players have internet access
- The system depends on the database and GitHub repository
- Game logic assumes valid predefined rules provided by the admin


# 3. Specific Requirements

## 3.1. Functional requirements

   	ID		Requirement														Priority
   
	FR1		The system shall allow players to register and log in securely 	High
	FR2		The admin shall be able to create, edit and delete tournaments 	High
	FR3		The system shall store every move of each match in a database 	High
	FR4		The system shall allow paused games to be resumed by the same 	Medium
			players								
	FR5		The system shall validate every move according to the official 	High
			rules
	FR6		The system shall calculate and display player rankings 			Medium
	FR7		The system shall may allow users to play against the AI 		Optional
	FR8		The system shall restrict administrative functions 				High
			to authorized users
	
## 3.2. Non-Functional Requirements

   	ID		Requirement 												 	Type

	NFR1	System availability ≥ 95% during normal hours 					Reliability
	NFR2	Average response time < 2 seconds 								Performance
	NFR3	Players should be able to learn basic use after ≤ 4 hours 		Usability
			of training
	NFR4	The interface must be responsive on desktop and mobile devices 	Portability
	NFR5	The system must comply with privacy regulation LOPD 15/1999 	Legal
	NFR6	Each component should be modular to support future 				Maintainability 
			AI integration

## 3.3. Performance Requirements

The system must handle at least 10 simultaneous matches exceeding a 2-seconf delay

## 3.4. Design Constraints

- Development in [language chosen, e.g. Java/Python + SQL database]
- Must integrate with GitHub repository and SCRUM workflow

## 3.5. Software System Attributes

- Security: user authentication and role-based access
- Maintainability: modular and documented code
- Usability: intuitive interface with visual feedback
- Reliability: consistent game state even in case of disconnection

# 4. Appendices 

- Use Case Diagram show actors (player, admin, AI) and main use cases (Play Game, Menage Rules, 
view Ranking)
- Use Case Templates with scenario, preconditions, postconditions and extensions
- Glossary of game-elated terms
