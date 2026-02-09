```mermaid
flowchart TD
    A["Login / Registration"] --> B{"New user?"}
    B -- Yes --> C["Register: Insert data"]
    B -- No --> D["Login"]
    C --> D
    D --> E["Choose game mode"]
    E --> F{"Solo or Multiplayer?"}
    F -- Solo --> G["Start solo game"]
    F -- Multiplayer --> H["Connect with other players / wait in lobby"]
    G --> I["Choose bet"]
    H --> I
    I --> J["Deal cards to players"]
    J --> K["Player decisions: Hit / Stand / Double / Split"]
    K --> L["Deal cards to dealer"]
    L --> M["Compare results / calculate winnings"]
    M --> N["Show result / update player account"]
    N --> O{"Play again?"}
    O -- Yes --> E
    O -- No --> P["Logout / END"]

