# AutoChess
Computer chess agent written in Kotlin.
##### Features
- Alpha-beta pruning 
- Iterative Deepening Search
- Transposition Table / Evaluation Cache
- Material Difference Heuristic
- Mask-based board representation
##### Caveats
- Does not support castling yet.
##### Usage
Run `Entrypoint.kt`. The agent will play a game against itself.
For each move a search of at least depth 3 is conducted. Depth will be iteratively
extended until 15 seconds have elapsed since the turn began.