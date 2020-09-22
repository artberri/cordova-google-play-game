/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.berriart.cordova.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.berriart.cordova.plugins.GameHelper.GameHelperListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.leaderboard.*;
import com.google.android.gms.games.achievement.*;

public class PlayGamesServices extends CordovaPlugin implements GameHelperListener {

    private static final String LOGTAG = "berriart-CordovaPlayGamesServices";

    private static final String ACTION_AUTH = "auth";
    private static final String ACTION_SIGN_OUT = "signOut";
    private static final String ACTION_IS_SIGNEDIN = "isSignedIn";

    private static final String ACTION_SUBMIT_SCORE = "submitScore";
    private static final String ACTION_SUBMIT_SCORE_NOW = "submitScoreNow";
    private static final String ACTION_GET_PLAYER_SCORE = "getPlayerScore";
    private static final String ACTION_SHOW_ALL_LEADERBOARDS = "showAllLeaderboards";
    private static final String ACTION_SHOW_LEADERBOARD = "showLeaderboard";

    private static final String ACTION_UNLOCK_ACHIEVEMENT = "unlockAchievement";
    private static final String ACTION_UNLOCK_ACHIEVEMENT_NOW = "unlockAchievementNow";

    private static final String ACTION_REVEAL_ACHIEVEMENT = "revealAchievement";    
    private static final String ACTION_INCREMENT_EVENT = "incrementEvent";

    private static final String ACTION_INCREMENT_ACHIEVEMENT = "incrementAchievement";
    private static final String ACTION_INCREMENT_ACHIEVEMENT_NOW = "incrementAchievementNow";
    private static final String ACTION_SHOW_ACHIEVEMENTS = "showAchievements";
    private static final String ACTION_SHOW_PLAYER = "showPlayer";

    private static final int ACTIVITY_CODE_SHOW_LEADERBOARD = 0;
    private static final int ACTIVITY_CODE_SHOW_ACHIEVEMENTS = 1;

    private GameHelper gameHelper;

    private CallbackContext authCallbackContext;
    private int googlePlayServicesReturnCode;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Activity cordovaActivity = cordova.getActivity();

        googlePlayServicesReturnCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(cordovaActivity);

        if (googlePlayServicesReturnCode == ConnectionResult.SUCCESS) {
            gameHelper = new GameHelper(cordovaActivity, GameHelper.CLIENT_GAMES);
            gameHelper.setup(this);
        } else {
            Log.w(LOGTAG, String.format("GooglePlayServices not available. Error: '" +
                    GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode) +
                    "'. Error Code: " + googlePlayServicesReturnCode));
        }

        cordova.setActivityResultCallback(this);
    }

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {

        JSONObject options = inputs.optJSONObject(0);

        if (gameHelper == null) {
            Log.w(LOGTAG, String.format("Tried calling: '" + action + "', but error with GooglePlayServices"));
            Log.w(LOGTAG, String.format("GooglePlayServices not available. Error: '" +
                    GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode) +
                    "'. Error Code: " + googlePlayServicesReturnCode));

            JSONObject googlePlayError = new JSONObject();
            googlePlayError.put("errorCode", googlePlayServicesReturnCode);
            googlePlayError.put("errorString", GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesReturnCode));

            JSONObject result = new JSONObject();
            result.put("googlePlayError", googlePlayError);
            callbackContext.error(result);

            return true;
        }

        Log.i(LOGTAG, String.format("Processing action " + action + " ..."));

        if (ACTION_AUTH.equals(action)) {
            executeAuth(callbackContext);
        } else if (ACTION_SIGN_OUT.equals(action)) {
            executeSignOut(callbackContext);
        } else if (ACTION_IS_SIGNEDIN.equals(action)) {
            executeIsSignedIn(callbackContext);
        } else if (ACTION_SUBMIT_SCORE.equals(action)) {
            executeSubmitScore(options, callbackContext);
        } else if (ACTION_SUBMIT_SCORE_NOW.equals(action)) {
            executeSubmitScoreNow(options, callbackContext);
        } else if (ACTION_GET_PLAYER_SCORE.equals(action)) {
            executeGetPlayerScore(options, callbackContext);
        } else if (ACTION_SHOW_ALL_LEADERBOARDS.equals(action)) {
            executeShowAllLeaderboards(callbackContext);
        } else if (ACTION_SHOW_LEADERBOARD.equals(action)) {
            executeShowLeaderboard(options, callbackContext);
        } else if (ACTION_SHOW_ACHIEVEMENTS.equals(action)) {
            executeShowAchievements(callbackContext);
        } else if (ACTION_UNLOCK_ACHIEVEMENT.equals(action)) {
            executeUnlockAchievement(options, callbackContext);
        } else if (ACTION_UNLOCK_ACHIEVEMENT_NOW.equals(action)) {
            executeUnlockAchievementNow(options, callbackContext);
        } else if (ACTION_REVEAL_ACHIEVEMENT.equals(action)) {
            executeRevealAchievement(options, callbackContext);
        } else if (ACTION_INCREMENT_EVENT.equals(action)) {
            executeIncrementEvent(options, callbackContext);
        } else if (ACTION_INCREMENT_ACHIEVEMENT.equals(action)) {
            executeIncrementAchievement(options, callbackContext);
        } else if (ACTION_INCREMENT_ACHIEVEMENT_NOW.equals(action)) {
            executeIncrementAchievementNow(options, callbackContext);
        } else if (ACTION_SHOW_PLAYER.equals(action)) {
            executeShowPlayer(callbackContext);
        } else {
            return false; // Tried to execute an unknown method
        }

        return true;
    }

    private void executeAuth(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeAuth");

        authCallbackContext = callbackContext;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gameHelper.beginUserInitiatedSignIn();
            }
        });
    }

    private void executeSignOut(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeSignOut");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gameHelper.signOut();
                callbackContext.success();
            }
        });
    }

    private void executeIsSignedIn(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeIsSignedIn");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject result = new JSONObject();
                    result.put("isSignedIn", gameHelper.isSignedIn());
                    callbackContext.success(result);
                } catch (JSONException e) {
                    Log.w(LOGTAG, "executeIsSignedIn: unable to determine if user is signed in or not", e);
                    callbackContext.error("executeIsSignedIn: unable to determine if user is signed in or not");
                }
            }
        });
    }

    private void executeSubmitScore(final JSONObject options, final CallbackContext callbackContext) throws JSONException {
        Log.d(LOGTAG, "executeSubmitScore");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (gameHelper.isSignedIn()) {
                        Games.Leaderboards.submitScore(gameHelper.getApiClient(), options.getString("leaderboardId"), options.getInt("score"));
                        callbackContext.success("executeSubmitScore: score submited successfully");
                    } else {
                        callbackContext.error("executeSubmitScore: not yet signed in");
                    }
                } catch (JSONException e) {
                    Log.w(LOGTAG, "executeSubmitScore: unexpected error", e);
                    callbackContext.error("executeSubmitScore: error while submitting score");
                }
            }
        });
    }

    private void executeSubmitScoreNow(final JSONObject options, final CallbackContext callbackContext) throws JSONException {
        Log.d(LOGTAG, "executeSubmitScoreNow");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (gameHelper.isSignedIn()) {
                        PendingResult<Leaderboards.SubmitScoreResult> result = Games.Leaderboards.submitScoreImmediate(gameHelper.getApiClient(), options.getString("leaderboardId"), options.getInt("score"));
                        result.setResultCallback(new ResultCallback<Leaderboards.SubmitScoreResult>() {
                            @Override
                            public void onResult(Leaderboards.SubmitScoreResult submitScoreResult) {
                                if (submitScoreResult.getStatus().isSuccess()) {
                                    ScoreSubmissionData scoreSubmissionData = submitScoreResult.getScoreData();

                                    if (scoreSubmissionData != null) {
                                        try {
                                            ScoreSubmissionData.Result scoreResult = scoreSubmissionData.getScoreResult(LeaderboardVariant.TIME_SPAN_ALL_TIME);
                                            JSONObject result = new JSONObject();
                                            result.put("leaderboardId", scoreSubmissionData.getLeaderboardId());
                                            result.put("playerId", scoreSubmissionData.getPlayerId());
                                            result.put("formattedScore", scoreResult.formattedScore);
                                            result.put("newBest", scoreResult.newBest);
                                            result.put("rawScore", scoreResult.rawScore);
                                            result.put("scoreTag", scoreResult.scoreTag);
                                            callbackContext.success(result);
                                        } catch (JSONException e) {
                                            Log.w(LOGTAG, "executeSubmitScoreNow: unexpected error", e);
                                            callbackContext.error("executeSubmitScoreNow: error while submitting score");
                                        }
                                    } else {
                                        callbackContext.error("executeSubmitScoreNow: can't submit the score");
                                    }
                                } else {
                                    callbackContext.error("executeSubmitScoreNow error: " + submitScoreResult.getStatus().getStatusMessage());
                                }
                            }
                        });
                    } else {
                        callbackContext.error("executeSubmitScoreNow: not yet signed in");
                    }
                } catch (JSONException e) {
                    Log.w(LOGTAG, "executeSubmitScoreNow: unexpected error", e);
                    callbackContext.error("executeSubmitScoreNow: error while submitting score");
                }
            }
        });
    }

    private void executeGetPlayerScore(final JSONObject options, final CallbackContext callbackContext) throws JSONException {
        Log.d(LOGTAG, "executeGetPlayerScore");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (gameHelper.isSignedIn()) {
                        PendingResult<Leaderboards.LoadPlayerScoreResult> result = Games.Leaderboards.loadCurrentPlayerLeaderboardScore(gameHelper.getApiClient(), options.getString("leaderboardId"), LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC);
                        result.setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
                            @Override
                            public void onResult(Leaderboards.LoadPlayerScoreResult playerScoreResult) {
                                if (playerScoreResult.getStatus().isSuccess()) {
                                    LeaderboardScore score = playerScoreResult.getScore();

                                    if (score != null) {
                                        try {
                                            JSONObject result = new JSONObject();
                                            result.put("playerScore", score.getRawScore());
                                            callbackContext.success(result);
                                        } catch (JSONException e) {
                                            Log.w(LOGTAG, "executeGetPlayerScore: unexpected error", e);
                                            callbackContext.error("executeGetPlayerScore: error while retrieving score");
                                        }
                                    } else {
                                        callbackContext.error("There isn't have any score record for this player");
                                    }
                                } else {
                                    callbackContext.error("executeGetPlayerScore error: " + playerScoreResult.getStatus().getStatusMessage());
                                }
                            }
                        });
                    } else {
                        callbackContext.error("executeGetPlayerScore: not yet signed in");
                    }
                } catch (JSONException e) {
                    Log.w(LOGTAG, "executeGetPlayerScore: unexpected error", e);
                    callbackContext.error("executeGetPlayerScore: error while retrieving score");
                }
            }
        });
    }

    private void executeShowAllLeaderboards(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowAllLeaderboards");

        final PlayGamesServices plugin = this;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (gameHelper.isSignedIn()) {
                    Intent allLeaderboardsIntent = Games.Leaderboards.getAllLeaderboardsIntent(gameHelper.getApiClient());
                    cordova.startActivityForResult(plugin, allLeaderboardsIntent, ACTIVITY_CODE_SHOW_LEADERBOARD);
                    callbackContext.success();
                } else {
                    Log.w(LOGTAG, "executeShowAllLeaderboards: not yet signed in");
                    callbackContext.error("executeShowAllLeaderboards: not yet signed in");
                }
            }
        });
    }

    private void executeShowLeaderboard(final JSONObject options, final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowLeaderboard");

        final PlayGamesServices plugin = this;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (gameHelper.isSignedIn()) {
                        Intent leaderboardIntent = Games.Leaderboards.getLeaderboardIntent(gameHelper.getApiClient(), options.getString("leaderboardId"));
                        cordova.startActivityForResult(plugin, leaderboardIntent, ACTIVITY_CODE_SHOW_LEADERBOARD);
                        callbackContext.success();
                    } else {
                        Log.w(LOGTAG, "executeShowLeaderboard: not yet signed in");
                        callbackContext.error("executeShowLeaderboard: not yet signed in");
                    }
                } catch (JSONException e) {
                    Log.w(LOGTAG, "executeShowLeaderboard: unexpected error", e);
                    callbackContext.error("executeShowLeaderboard: error while showing specific leaderboard");
                }
            }
        });
    }

    private void executeUnlockAchievement(final JSONObject options, final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeUnlockAchievement");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (gameHelper.isSignedIn()) {
                    Games.Achievements.unlock(gameHelper.getApiClient(), options.optString("achievementId"));
                    callbackContext.success();
                } else {
                    Log.w(LOGTAG, "executeUnlockAchievement: not yet signed in");
                    callbackContext.error("executeUnlockAchievement: not yet signed in");
                }
            }
        });
    }

    private void executeUnlockAchievementNow(final JSONObject options, final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeUnlockAchievementNow");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (gameHelper.isSignedIn()) {
                    PendingResult<Achievements.UpdateAchievementResult> result = Games.Achievements.unlockImmediate(gameHelper.getApiClient(), options.optString("achievementId"));
                    result.setResultCallback(new ResultCallback<Achievements.UpdateAchievementResult>() {
                            @Override
                            public void onResult(Achievements.UpdateAchievementResult achievementResult) {
                                if (achievementResult.getStatus().isSuccess()) {
                                    try {
                                        JSONObject result = new JSONObject();
                                        result.put("achievementId", achievementResult.getAchievementId());
                                        callbackContext.success(result);
                                    } catch (JSONException e) {
                                        Log.w(LOGTAG, "executeUnlockAchievementNow: unexpected error", e);
                                        callbackContext.error("executeUnlockAchievementNow: error while unlocking achievement");
                                    }
                                } else {
                                    callbackContext.error("executeUnlockAchievementNow error: " + achievementResult.getStatus().getStatusMessage());
                                }
                            }
                        });
                } else {
                    Log.w(LOGTAG, "executeUnlockAchievementNow: not yet signed in");
                    callbackContext.error("executeUnlockAchievementNow: not yet signed in");
                }
            }
        });
    }
    
	private void executeRevealAchievement(final JSONObject options,final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeRevealAchievement");

        final PlayGamesServices plugin = this;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (gameHelper.isSignedIn()) {
                    Games.Achievements.reveal(gameHelper.getApiClient(),options.optString("achievementId"));                    
                    callbackContext.success();
                } else {
                    Log.w(LOGTAG, "executeRevealAchievement: not yet signed in");
                    callbackContext.error("executeRevealAchievement: not yet signed in");
                }
            }
        });
    }

    private void executeIncrementEvent(final JSONObject options, final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeIncrementEvent");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (gameHelper.isSignedIn()) {
                    Games.Events.increment(gameHelper.getApiClient(), options.optString("eventId"), options.optInt("numSteps"));
                    callbackContext.success();
                } else {
                    Log.w(LOGTAG, "executeIncrementEvent: not yet signed in");
                    callbackContext.error("executeIncrementEvent: not yet signed in");
                }
            }
        });
    }
    
    private void executeIncrementAchievement(final JSONObject options, final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeIncrementAchievement");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (gameHelper.isSignedIn()) {
                    Games.Achievements.increment(gameHelper.getApiClient(), options.optString("achievementId"), options.optInt("numSteps"));
                    callbackContext.success();
                } else {
                    Log.w(LOGTAG, "executeIncrementAchievement: not yet signed in");
                    callbackContext.error("executeIncrementAchievement: not yet signed in");
                }
            }
        });
    }

    private void executeIncrementAchievementNow(final JSONObject options, final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeIncrementAchievementNow");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (gameHelper.isSignedIn()) {
                    PendingResult<Achievements.UpdateAchievementResult> result = Games.Achievements.incrementImmediate(gameHelper.getApiClient(), options.optString("achievementId"), options.optInt("numSteps"));
                    result.setResultCallback(new ResultCallback<Achievements.UpdateAchievementResult>() {
                            @Override
                            public void onResult(Achievements.UpdateAchievementResult achievementResult) {
                                if (achievementResult.getStatus().isSuccess()) {
                                    try {
                                        JSONObject result = new JSONObject();
                                        result.put("achievementId", achievementResult.getAchievementId());
                                        callbackContext.success(result);
                                    } catch (JSONException e) {
                                        Log.w(LOGTAG, "executeIncrementAchievementNow: unexpected error", e);
                                        callbackContext.error("executeIncrementAchievementNow: error while incrementing achievement");
                                    }
                                } else {
                                    callbackContext.error("executeIncrementAchievementNow error: " + achievementResult.getStatus().getStatusMessage());
                                }
                            }
                        });
                    callbackContext.success();
                } else {
                    Log.w(LOGTAG, "executeIncrementAchievement: not yet signed in");
                    callbackContext.error("executeIncrementAchievement: not yet signed in");
                }
            }
        });
    }
	


	
    private void executeShowAchievements(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowAchievements");

        final PlayGamesServices plugin = this;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (gameHelper.isSignedIn()) {
                    Intent achievementsIntent = Games.Achievements.getAchievementsIntent(gameHelper.getApiClient());
                    cordova.startActivityForResult(plugin, achievementsIntent, ACTIVITY_CODE_SHOW_ACHIEVEMENTS);
                    callbackContext.success();
                } else {
                    Log.w(LOGTAG, "executeShowAchievements: not yet signed in");
                    callbackContext.error("executeShowAchievements: not yet signed in");
                }
            }
        });
    }

    private void executeShowPlayer(final CallbackContext callbackContext) {
        Log.d(LOGTAG, "executeShowPlayer");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (gameHelper.isSignedIn()) {

                        Player player = Games.Players.getCurrentPlayer(gameHelper.getApiClient());

                        JSONObject playerJson = new JSONObject();
                        playerJson.put("displayName", player.getDisplayName());
                        playerJson.put("playerId", player.getPlayerId());
                        playerJson.put("title", player.getTitle());
                        playerJson.put("iconImageUrl", player.getIconImageUrl());
                        playerJson.put("hiResIconImageUrl", player.getHiResImageUrl());

                        callbackContext.success(playerJson);

                    } else {
                        Log.w(LOGTAG, "executeShowPlayer: not yet signed in");
                        callbackContext.error("executeShowPlayer: not yet signed in");
                    }
                }
                catch(Exception e) {
                    Log.w(LOGTAG, "executeShowPlayer: Error providing player data", e);
                    callbackContext.error("executeShowPlayer: Error providing player data");
                }
            }
        });
    }


    @Override
    public void onSignInFailed() {
        authCallbackContext.error("SIGN IN FAILED");
    }

    @Override
    public void onSignInSucceeded() {
        authCallbackContext.success("SIGN IN SUCCESS");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        gameHelper.onActivityResult(requestCode, resultCode, intent);
    }
}
