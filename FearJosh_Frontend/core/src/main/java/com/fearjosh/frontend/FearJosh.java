package com.fearjosh.frontend;

import com.badlogic.gdx.Game;
import com.fearjosh.frontend.screen.MainMenuScreen;

public class FearJosh extends Game {

    @Override
    public void create() {
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
