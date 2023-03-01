# MLB-GameFeed-Bot

This bot allows you to view the live play-by-plays for any MLB.com game. This attempts to mimic [gameday](https://mlb.com/gameday)

To get started, add the bot to your server. Next, wait for a game to be active, finally, type `/startgame` to see active Major League games.

Supported Message Types:
- Score Changes
- Game Advisories (Injury Delay, Pitching changes, etc)
- Inning Changes

Commands:
- `/startgame [game]` - Starts a game. Select a game from the list, but any gamePk is acceptable. You can grab this from sites like https://mlb.chew.pw to show minor league games. If no games show up, there are no active Major League games.
- `/stopgame` - Stops the currently running game.
- `/score` - Shows you the current score privately.
- `/setinfo` - A command to show specific info as a Voice Channel name.

Running `/startgame` does not memorize the game, you will have to start it every time a game starts!
