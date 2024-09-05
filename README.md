# Cordova Plugin For Play Games Services

> 📌 **Deprecation Notice**
>
> This repository is deprecated and no more work will be done on this by [Alberto Varela](https://github.com/artberri). The usage of this plugin is discouraged and any future issues will not be fixed by its current author.
>
> Feel free to fork this repository and improve your fork.

Cordova Plugin For Google Play Games Services (Fork of [ptgamr/cordova-google-play-game](https://github.com/ptgamr/cordova-plugin-play-games-services))

Modified to include the new Google Play Services (GoogleApiAvailability) and new methods for Leaderboards and Achievements.

**Before you start:**

Understand about **Leaderboard** and **Achievement**. Setting up your game in Google Play Developer Console https://developers.google.com/games/services/android/quickstart

## Install

Cordova >= 5.0.0

```bash
cordova plugin add cordova-plugin-play-games-services --variable APP_ID=you_app_id_here
```

Cordova < 5.0.0

```bash
cordova plugin add https://github.com/artberri/cordova-plugin-play-games-services.git --variable APP_ID=you_app_id_here
```

## Usage

### Authentication

#### Sign in

You should do this as soon as your `deviceready` event has been fired. The plugin handles the various auth scenarios for you.

```js
cordova.plugins.playGamesServices.auth(
  function () {
    // On logged in
  },
  function () {
    // On not logged in
  }
);
```

#### Sign out

You should provide the option for users to sign out

```js
cordova.plugins.playGamesServices.signOut(function () {
  // On logged out
});
```

#### Auth status

To check if the user is already logged in (eg. to determine weather to show the Log In or Log Out button), use the following

```js
cordova.plugins.playGamesServices.isSignedIn(
  function (result) {
    // ‘result’ is the following object
    // {
    //         "isSignedIn": boolean
    // }
    console.log("Is user signed in: " + result.isSignedIn);
  },
  function () {
    // On error: Auth check could not be done
  }
);
```

#### Player Information

Fetch the currently authenticated player's data.

```js
cordova.plugins.playGamesServices.showPlayer(function (playerData) {
  // playerData is the following object
  // {
  //      displayName: string;
  //      playerId: string;
  //      title: string;
  //      iconImageUrl: string;
  //      hiResIconImageUrl: string;
  // }
  console.log("Authenticated as " + playerData.displayName);
});
```

### Leaderboards

#### Submit Score

Ensure you have had a successful callback from `cordova.plugins.playGamesServices.auth()` first before attempting to submit a score. You should also have set up your leaderboard(s) in Google Play Game Console and use the leaderboard identifier assigned there as the `leaderboardId`.

```js
var data = {
  score: 10,
  leaderboardId: "board1",
};
cordova.plugins.playGamesServices.submitScore(
  data,
  function () {
    // On success
  },
  function () {
    // On error
  }
);
```

#### Sumit Score Now

Ensure you have had a successful callback from `cordova.plugins.playGamesServices.auth()` first before attempting to submit a score. You should also have set up your leaderboard(s) in Google Play Game Console and use the leaderboard identifier assigned there as the `leaderboardId`.

This method submit the score immediately and returns info.

```js
var data = {
  score: 10,
  leaderboardId: "board1",
};
cordova.plugins.playGamesServices.submitScoreNow(
  data,
  function (result) {
    // ‘result’ is the following object
    // {
    //      leaderboardId: string;
    //      playerId: string;
    //      formattedScore: string;
    //      newBest: boolean;
    //      rawScore: number;
    //      scoreTag: string;
    // }
    console.log("Is this your best score: " + result.newBest);
  },
  function () {
    // On error
  }
);
```

#### Get player's score

This method gets the score of a leaderboard.

```js
var data = {
  leaderboardId: "board1",
};
cordova.plugins.playGamesServices.getPlayerScore(
  data,
  function (result) {
    // ‘result’ is the following object
    // {
    //      playerScore: number;
    // }
    console.log("Is this your score: " + result.playerScore);
  },
  function () {
    // On error
  }
);
```

#### Show all leaderboards

Launches the native Play Games leaderboard view controller to show all the leaderboards.

```js
cordova.plugins.playGamesServices.showAllLeaderboards(
  function () {
    // On success
  },
  function () {
    // On error
  }
);
```

#### Show specific leaderboard

Launches directly into the specified leaderboard:

```js
var data = {
  leaderboardId: "board1",
};
cordova.plugins.playGamesServices.showLeaderboard(
  leaderboardId,
  function () {
    // On success
  },
  function () {
    // On error
  }
);
```

### Achievements

#### Unlock achievement

Unlocks the specified achievement:

```js
var data = {
  achievementId: "achievementId1",
};

cordova.plugins.playGamesServices.unlockAchievement(
  data,
  function () {
    // On success
  },
  function () {
    // On error
  }
);
```

#### Unlock achievement Now

Unlocks the specified achievement inmediately and waits for response:

```js
var data = {
  achievementId: "achievementId1",
};

cordova.plugins.playGamesServices.unlockAchievementNow(
  data,
  function () {
    // On success
  },
  function () {
    // On error
  }
);
```

#### Increment achievement

Increments the specified incremental achievement by the provided numSteps:

```js
var data = {
  achievementId: "achievementId1",
  numSteps: 1,
};

cordova.plugins.playGamesServices.incrementAchievement(
  data,
  function () {
    // On success
  },
  function () {
    // On error
  }
);
```

#### Increment achievement Now

Increments the specified incremental achievement by the provided numSteps and waits for response

```js
var data = {
  achievementId: "achievementId1",
  numSteps: 1,
};

cordova.plugins.playGamesServices.incrementAchievementNow(
  data,
  function () {
    // On success
  },
  function () {
    // On error
  }
);
```

#### Show achievements

Launches the native Play Games achievements view controller to show the user’s achievements.

```js
cordova.plugins.playGamesServices.showAchievements(
  function () {
    // On success
  },
  function () {
    // On error
  }
);
```

### Other

Callbacks are optional for all methods.

## Platforms

Currently, only Android is supported

## License

[MIT License](http://ilee.mit-license.org)
