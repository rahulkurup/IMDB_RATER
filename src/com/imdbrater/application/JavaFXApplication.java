package com.imdbrater.application;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

public final class JavaFXApplication extends Application {

    private List<String> movieFileNameList = new ArrayList<String>();
    private final TableView<MovieInfo> guiTable = new TableView<MovieInfo>();
    private final ObservableList<MovieInfo> tableData = FXCollections.observableArrayList();
    //Video formats to be searched
    private final String[] videoFormats = {"avi", "divx", "mkv", "mpg", "mp4",
            "wmv", "bin", "ogm", "vob", "iso",
            "img", "nts", "rmvb", "3gp",
            "asf", "flv", "mov", "movx", "mpe",
            "mpeg", "mpg", "mpv", "ogg", "ram",
            "rm", "wm", "wmx", "x264", "xvid","dv"};
    final Set<String> videoFormatSet = new HashSet<String>(Arrays.asList(videoFormats));

    //Constructor
    public JavaFXApplication() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(final Stage stage) {

        // stage properties
        stage.setTitle("Movie Rater");
        stage.setResizable(true);
        stage.setMaximized(true);

        // Table elements and its properties
        final TableColumn<MovieInfo, String> FileNameCol = new TableColumn<MovieInfo, String>("File Name");
        final TableColumn<MovieInfo, String> movieNameCol = new TableColumn<MovieInfo, String>("Movie Name");
        final TableColumn<MovieInfo, String> imdbRatingCol = new TableColumn<MovieInfo, String>("IMDB Rating");
        final TableColumn<MovieInfo, String> yearCol = new TableColumn<MovieInfo, String>("Year");
        final TableColumn<MovieInfo, String> genreCol = new TableColumn<MovieInfo, String>("Genre");
        final TableColumn<MovieInfo, String> languageCol = new TableColumn<MovieInfo, String>("Language");

        FileNameCol.setCellValueFactory(new PropertyValueFactory<MovieInfo, String>("fileName"));
        movieNameCol.setCellValueFactory(new PropertyValueFactory<MovieInfo, String>("title"));
        imdbRatingCol.setCellValueFactory(new PropertyValueFactory<MovieInfo, String>("imdbRating"));
        yearCol.setCellValueFactory(new PropertyValueFactory<MovieInfo, String>("year"));
        genreCol.setCellValueFactory(new PropertyValueFactory<MovieInfo, String>("genre"));
        languageCol.setCellValueFactory(new PropertyValueFactory<MovieInfo, String>("language"));
        // Adding columns to table
        guiTable.getColumns().addAll(FileNameCol, movieNameCol, imdbRatingCol, yearCol,
                                     genreCol, languageCol);

        final Button selectButton = new Button("Select Movie Folder");
        selectButton.setStyle("-fx-font: 13 arial; -fx-base: #b6e7c9;");
        selectButton.setPrefSize(200d, 50d);
        selectButton.autosize();
        final Button rateButton = new Button("Get the Rating");
        rateButton.setStyle("-fx-font: 13 arial; -fx-base: #b6e7c9;");
        rateButton.setPrefSize(200d,  50d);
        rateButton.autosize();
        rateButton.setDisable(true);
        final ProgressBar guiProgressBar = new ProgressBar(0.0);
        guiProgressBar.setPrefSize(1000d, 50d);
        guiProgressBar.autosize();
        guiProgressBar.setVisible(false);

        //Creating a grid pane and adding two buttons & Progress bar to it
        final GridPane guiGridPane = new GridPane();
        GridPane.setConstraints(selectButton, 0, 0);
        GridPane.setConstraints(rateButton, 1, 0);
        guiGridPane.add(guiProgressBar, 2 , 0);
        guiGridPane.setHgap(25);
        guiGridPane.setVgap(25);
        guiGridPane.getChildren().addAll(selectButton, rateButton);

        // Create a pane (vBox) and add grid pane and table to it
        final Pane rootGroup = new VBox(25);
        rootGroup.getChildren().addAll(guiGridPane, guiTable);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));
        final Scene guiScene = new Scene(rootGroup, 1300, 600, Color.LIGHTBLUE);
        stage.setScene(guiScene);
        stage.show();

        //Set the select button action
        selectButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(final ActionEvent e) {
                final DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select Movie Folder");
                File folder = directoryChooser.showDialog(stage);

                // Chosen folder is invalid or it is disk (tool doesn't support disk)
                // TODO: Add disk check for Linux as well
                String patternString = "^([A-Z]):\\\\";
                // Create a Pattern object
                Pattern pattern = Pattern.compile(patternString);
                // Now create matcher object.
                Matcher matcher = pattern.matcher(folder.getAbsolutePath());
                //If the selected folder is a drive or selection is invalid
                if(matcher.matches() || folder == null) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error Dialog");
                    alert.setHeaderText("Invalid folder selection");
                    alert.setContentText("There is something wrong with the selection of folder. Please select a valid folder."
                            +"\n"
                            + "\nNote: Disk drives(Example: C:\\) are not suppported by the tool. Select a folder.");
                    alert.showAndWait();
                    return;
                }
                Thread getFilesThread = new Thread(new GetfilesThread(folder));
                getFilesThread.start();
                rateButton.setDisable(false);
            }
        });

        //set the rate button action
        rateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(final ActionEvent e) {
                rateButton.setDisable(true);
                guiProgressBar.setVisible(true);
                guiProgressBar.setProgress(0.05);
                Alert guiAlert = new Alert(AlertType.ERROR);
                Thread rateThread = new Thread(new RaterThread(guiProgressBar, guiAlert));
                rateThread.start();
            }
        });
    }

    //Main Method
    public static void main(String[] args) {
        Application.launch(args);
    }


    class GetfilesThread extends Thread {

        File folder;

        public GetfilesThread(File folder) {
            this.folder = folder;
        }

        public void run() {
            getFileNames(folder);
        }

        //Get files from the user computer
        public void getFileNames(File folder) {
            for (final File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    getFileNames(file);
                } else {
                    if (FilenameUtils.isExtension(file.getName().toLowerCase(), videoFormatSet)) {
                        //Don't consider video files less than 100 MB
                        final Long FileSizeInMB = file.length() / 1048576;
                        if (FileSizeInMB < 100) {
                            continue;
                        }
                        final String fileName = FilenameUtils.removeExtension(file.getName());
                        if (!movieNameFilter(fileName).isEmpty()) {
                            movieFileNameList.add(movieNameFilter(fileName));
                        } else {
                            movieFileNameList.add(fileName);
                        }
                    }
                }
            }
        }
    }

    class RaterThread extends Thread {

        ProgressBar guiProgressBar;
        Alert guiAlert;

        public RaterThread(final ProgressBar guiProgressBar, final Alert guiAlert) {
            this.guiProgressBar = guiProgressBar;
            this.guiAlert = guiAlert;
        }

        public void run() {
            doRating();
        }

        private void doRating() {

            final float totalProgressCount = movieFileNameList.size();
            float currentProgressCount = 0;
            float progressCount;

            //For each movie:
            for (final String movieName : movieFileNameList) {
                final String apiurl = "http://www.omdbapi.com/";
                String tempMovieName = movieName;

                while (true) {
                    try {
                        // Forming a complete URL ready to send
                        final String restURLLink = apiurl + "?t=" + tempMovieName + "&type=movie";
                        final URL url = new URL(restURLLink);
                        URLConnection omdbConnection = url.openConnection();

                        // Check if there is anything wrong with Internet.
                        if(omdbConnection.getContentLength() == -1) {
                            guiAlert.setTitle("Error Dialog");
                            guiAlert.setHeaderText("No Internet Connection");
                            guiAlert.setContentText("There is something wrong with your Internet Conection."
                                    +"\n"                    		
                                    + "\nPlease check your internet connection and try rerunning the tool.");
                            guiAlert.showAndWait();
                        }
                        final DataInputStream dataInputStream = new DataInputStream(omdbConnection.getInputStream());
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.setPropertyNamingStrategy(new MyNameStrategy());
                        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        final MovieInfo moviePojo = mapper.readValue(dataInputStream, MovieInfo.class);
                        if (!moviePojo.getResponse().equals("False") || tempMovieName.length() < 5) {
                            moviePojo.setFileName(movieName);
                            //Add to table
                            addDataToTable(moviePojo);
                            currentProgressCount++;
                            progressCount = currentProgressCount / totalProgressCount;
                            guiProgressBar.setProgress(progressCount);
                            break;
                        } else {
                            tempMovieName = tempMovieName.substring(0, tempMovieName.length() - 1);
                        }
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(JavaFXApplication.class.getName()).log(Level.SEVERE, null, ex);
                        break;
                    } catch (IOException ex) {
                        Logger.getLogger(JavaFXApplication.class.getName()).log(Level.SEVERE, null, ex);
                        break;
                    }
                }
            }
            guiProgressBar.setProgress(1);
        }

        private void addDataToTable(final MovieInfo moviePojo) {
            tableData.add(moviePojo);
            guiTable.setItems(tableData);
        }
    }

    private String movieNameFilter(String fileName) {

        //Replace every '.'  and '_'with spaces (They may be present in the movie Name
        fileName = fileName.replace(".", " ");
        fileName = fileName.replace("_", " ");

        //Replace all non word characters and following 
        fileName = fileName.replaceAll("[^\\w\\s].*$", " ");

        //If an extension is found,remove it along with all following data
        //Risk: a movie name might contain the extension
        for (String extensions : videoFormats) {
            //replace all non word characters with space
            fileName = fileName.replaceAll(extensions + ".*$", " ");
        }

        //Replace all two or more continuous spaces by single space
        fileName = fileName.replaceAll("\\s+", " ");

        //remove all spaces in front and back
        fileName = fileName.trim();

        //convert to lower case
        fileName = fileName.toLowerCase();

        //add + instead of spaces
        fileName = fileName.replace(" ", "+");

        return fileName;
    }
}