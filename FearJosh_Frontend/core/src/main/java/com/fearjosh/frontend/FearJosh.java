package com.fearjosh.frontend;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.fearjosh.frontend.screen.MainMenuScreen;

public class FearJosh extends Game {

    @Override
    public void create() {
        // Enable debug logging
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
