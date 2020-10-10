/*
 * PersistWindowStateHelper.java
 *
 * Copyright 2015 Thomas Kuenneth
 *
 * PersistWindowStateHelper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.thomaskuenneth.modernjfx;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * This helper class is used to store size, location and state of a JavaFX
 * window. To use, just register an instance using
 * {@code new PersistWindowStateHelper()}, then invoke {@code register()} and
 * {@code restore()}.
 *
 * @author Thomas
 */
public final class PersistWindowStateHelper {

    private static final Logger LOGGER = Logger.getLogger(PersistWindowStateHelper.class.getName());

    private final ChangeListener l = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
        maybeSave();
    };

    private long last = 0l;
    private Preferences node;
    private Stage _stage;

    /**
     * Register a stage.
     *
     * @param root node that should store the data, for example
     * <code>Preferences.userRoot()</code>
     * @param name name
     * @param stage stage
     */
    public void register(Preferences root, String name, Stage stage) {
        node = root.node(name);
        _stage = stage;
        stage.setOnCloseRequest((WindowEvent event) -> {
            node.putBoolean("maximized", _stage.isMaximized());
            node.putBoolean("fullscreen", _stage.isFullScreen());
            if (!isMaximizedOrFullScreen()) {
                writeBounds();
            }
        });
        stage.xProperty().addListener(l);
        stage.yProperty().addListener(l);
        stage.widthProperty().addListener(l);
        stage.heightProperty().addListener(l);
    }

    /**
     * Restore a previously saved state
     */
    public void restore() {
        _stage.setX(node.getDouble("x", _stage.getX()));
        _stage.setY(node.getDouble("y", _stage.getY()));
        _stage.setWidth(node.getDouble("width", _stage.getWidth()));
        _stage.setHeight(node.getDouble("height", _stage.getHeight()));
        _stage.setMaximized(node.getBoolean("maximized", _stage.isMaximized()));
        _stage.setFullScreen(node.getBoolean("fullscreen", _stage.isFullScreen()));
    }

    private void writeBounds() {
        node.putDouble("x", _stage.getX());
        node.putDouble("y", _stage.getY());
        node.putDouble("width", _stage.getWidth());
        node.putDouble("height", _stage.getHeight());
    }

    private boolean isMaximizedOrFullScreen() {
        return _stage.isMaximized() || _stage.isFullScreen();
    }

    private void maybeSave() {
        long now = System.currentTimeMillis();
        if ((now - last) >= 1000) {
            last = now;
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(1000l);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "maybeSave()", ex);
                } finally {
                    Platform.runLater(() -> {
                        if (!isMaximizedOrFullScreen()) {
                            writeBounds();
                        }
                    });
                }
            });
            t.start();
        }
    }
}
