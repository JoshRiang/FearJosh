package com.fearjosh.frontend;

import com.badlogic.gdx.Game;
import com.fearjosh.frontend.screen.PlayScreen;

public class FearJosh extends Game {

    @Override
    public void create() {
        setScreen(new PlayScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
