/*
 * ImageSnatcher.java
 *
 * Copyright 2016 - 2020 Thomas Kuenneth
 *
 * ImageSnatcher is free software: you can redistribute it and/or modify
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
package com.thomaskuenneth.imagesnatcher;

import com.thomaskuenneth.modernjfx.PersistWindowStateHelper;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;

/**
 * The lock screen of Windows 10 can show random pictures provided by Microsoft.
 * Windows keeps a certain amount of them on the local machine, but peridocally
 * changes the set. This class can save copies of the corresponding list.
 * 
 * Run with: mvn clean javafx:run
 *
 * @author Thomas Kuenneth
 */
public class ImageSnatcher extends Application {

    private static final String CLASS_NAME = ImageSnatcher.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    private static final String PATH = "%s\\AppData\\Local\\Packages\\Microsoft.Windows.ContentDeliveryManager_cw5n1h2txyewy\\LocalState\\Assets";
    private static final String PNG = "png";
    private static final String EXT_PNG = ".png";

    private static final double MIN_WIDTH = 400;
    private static final int PREVIEW_WIDTH = 200;

    private static final ExtensionFilter FILTER_PNG = new ExtensionFilter("Portable Network Graphics", "*.png");

    private Stage _primaryStage;
    private FadeTransition transition;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        _primaryStage = primaryStage;
        transition = createFadeTransition();
        Pane imagesPane = createImagesPane();
        ScrollPane scrollPane = new ScrollPane(imagesPane);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(scrollPane);
        imagesPane.prefWidthProperty().bind(borderPane.widthProperty());
        imagesPane.prefHeightProperty().bind(borderPane.heightProperty());
        Scene scene = new Scene(borderPane);
        primaryStage.setScene(scene);
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.setTitle("Image Snatcher");

        PersistWindowStateHelper helper = new PersistWindowStateHelper();
        helper.register(Preferences.userRoot(), CLASS_NAME, primaryStage);
        helper.restore();

        primaryStage.show();
    }

    private FadeTransition createFadeTransition() {
        FadeTransition ft = new FadeTransition();
        ft.setDuration(new Duration(700));
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        return ft;
    }

    private List<File> getFiles() {
        List<File> list = new ArrayList<>();
        String osName = System.getProperty("os.name", "???").toLowerCase();
        if (osName.startsWith("windows")) {
            String strPath = String.format(PATH, System.getProperty("user.home"));
            File parent = new File(strPath);
            recurseFiles(list, parent);
        } else if (osName.startsWith("mac")) {
            File parent = new File("/Library/Screen Savers/Default Collections");
            recurseFiles(list, parent);
        }
        return list;
    }

    private void recurseFiles(List<File> list, File parent) {
        File[] files = parent.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    list.add(f);
                } else if (f.isDirectory()) {
                    recurseFiles(list, f);
                }
            }
        }
    }

    private Pane createImagesPane() {
        FlowPane pane = new FlowPane(10, 10);
        List<File> list = getFiles();
        for (File file : list) {
            Image image = new Image(file.toURI().toString(), MIN_WIDTH, MIN_WIDTH, true, true, true);
            image.progressProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                if ((!image.isError()) && (newValue.equals(1.0))) {
                    if (image.getWidth() >= MIN_WIDTH) {
                        ImageView imageView = createImageView(image);
                        MyStackPane stackPane = new MyStackPane(imageView, file);
                        stackPane.setPrefSize(PREVIEW_WIDTH, PREVIEW_WIDTH);
                        stackPane.setOnMouseEntered(event -> {
                            addFlowPane((MyStackPane) event.getSource());
                        });
                        stackPane.setOnMouseExited(event -> {
                            removeFlowPane((MyStackPane) event.getSource());
                        });
                        pane.getChildren().add(stackPane);
                    }
                }
            });
        }
        return pane;
    }

    private void addFlowPane(MyStackPane sp) {
        ObservableList<Node> children = sp.getChildren();
        ListIterator<Node> iter = children.listIterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            if (node instanceof ImageView) {
                FlowPane flowPane = createFlowPane(sp.file);
                transition.setNode(flowPane);
                transition.stop();
                children.add(flowPane);
                transition.play();
                break;
            }
        }
    }

    private void removeFlowPane(StackPane sp) {
        ObservableList<Node> children = sp.getChildren();
        ListIterator<Node> iter = children.listIterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            if (node instanceof FlowPane) {
                children.remove(node);
                break;
            }
        }
    }

    private FlowPane createFlowPane(File file) {
        Image image = new Image(file.toURI().toString(), true);
        Button buttonView = new Button("View");
        buttonView.setOnAction(action -> {
            show(image);
        });
        Button buttonSave = new Button("Save");
        buttonSave.setOnAction(action -> {
            save(image);
        });
        FlowPane flowPane = new FlowPane(Orientation.VERTICAL, 10, 10, buttonView, buttonSave);
        flowPane.setAlignment(Pos.CENTER);
        image.progressProperty().addListener((ObservableValue<? extends Number> observable,
                Number oldValue, Number newValue) -> {
            if (image.isError() && (newValue.equals(1.0))) {
                LOGGER.severe("could not load image");
            }
        });
        return flowPane;
    }

    private ImageView createImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(PREVIEW_WIDTH);
        imageView.setFitHeight(PREVIEW_WIDTH);
        imageView.setSmooth(true);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private void show(Image image) {
        new Thread(() -> {
            try {
                File file = File.createTempFile("image_", EXT_PNG);
                file.deleteOnExit();
                if (writeImage(image, file)) {
                    Desktop.getDesktop().open(file);
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "show()", ex);
            }
        }).start();
    }

    private void save(Image image) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(FILTER_PNG);
        File pickedFile = fileChooser.showSaveDialog(_primaryStage);
        new Thread(() -> {
            if (pickedFile != null) {
                try {
                    writeImage(image, pickedFile);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "createImageView()", ex);
                    showExceptionDialog(ex, pickedFile);
                }
            }
        }).start();
    }

    private void showExceptionDialog(Exception e, File file) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setHeaderText(String.format("Could not save %s", file.getName()));
        alert.setContentText(e.getLocalizedMessage());
        alert.showAndWait();
    }

    private boolean writeImage(Image image, File file) throws IOException {
        return ImageIO.write(SwingFXUtils.fromFXImage(image, null), PNG, file);
    }

    static class MyStackPane extends StackPane {

        final File file;

        private MyStackPane(ImageView iv, File file) {
            super(iv);
            this.file = file;
        }
    }
}
