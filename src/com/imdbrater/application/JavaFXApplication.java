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
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.scene.paint.Color;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

public final class JavaFXApplication extends Application {

    private final List<MovieInfo> movieInfoList = new ArrayList<>();
    private List<String> yourMovieNameList = new ArrayList<>();
    private final TableView<MovieInfo> table = new TableView<>();
    private final ObservableList<MovieInfo> tableData = FXCollections.observableArrayList();
    //Video formats to be searched                
    private final String[] videoFormats = {"avi", "divx", "mkv", "mpg", "mp4",
        "wmv", "bin", "ogm", "vob", "iso",
        "img", "nts", "rmvb", "3gp",
        "asf", "flv", "mov", "movx", "mpe",
        "mpeg", "mpg", "mpv", "ogg", "ram",
        "rm", "wm", "wmx", "x264", "xvid",
        "dv"};
    final Set<String> videoFormatSet = new HashSet<>(Arrays.asList(videoFormats));

    //Constructor
    public JavaFXApplication() {
    }

    @Override
    public void start(final Stage stage) {

        stage.setTitle("Movie Rater");
        stage.setResizable(true);
        stage.centerOnScreen();

        //Table elements and its properties
        final TableColumn FileNameCol = new TableColumn("File Name");
        final TableColumn movieNameCol = new TableColumn("Movie Name");
        final TableColumn imdbRatingCol = new TableColumn("IMDB Rating");
        final TableColumn yearCol = new TableColumn("Year");
        final TableColumn genreCol = new TableColumn("Genre");
        final TableColumn languageCol = new TableColumn("Language");

        FileNameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        movieNameCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        imdbRatingCol.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        languageCol.setCellValueFactory(new PropertyValueFactory<>("language"));

        table.getColumns().addAll(FileNameCol, movieNameCol, imdbRatingCol, yearCol,
                			      genreCol, languageCol);
        
        final Button selectButton = new Button("Select Movie Folder");
        selectButton.setStyle("-fx-font: 13 arial; -fx-base: #b6e7c9;");
        final Button rateButton = new Button("Get the Rating");
        rateButton.setStyle("-fx-font: 13 arial; -fx-base: #b6e7c9;");
        rateButton.setDisable(true);

        final GridPane inputGridPane = new GridPane();
        GridPane.setConstraints(selectButton, 0, 0);
        GridPane.setConstraints(rateButton, 1, 0);
        inputGridPane.setHgap(25);
        inputGridPane.setVgap(25);
        inputGridPane.getChildren().addAll(selectButton, rateButton);

        final Pane rootGroup = new VBox(25);
        rootGroup.getChildren().addAll(inputGridPane, table);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));
        //stage.setFullScreen(true);
        stage.setScene(new Scene(rootGroup, 700, 300, Color.LIGHTBLUE));
        stage.show();

        //Set the select button action
        selectButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(final ActionEvent e) {
                final DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select Movie Folder");
                final File folder = directoryChooser.showDialog(stage);
                if (folder != null) {
                    Thread getFilesThread = new Thread(new GetfilesThread(folder));
                    getFilesThread.start();
                    rateButton.setDisable(false);
                    //TODO: Notify user if the file is not a folder
                    //Handle case where it is a disk
                }
                //TODO Display that selected folder is not valid
            }
        });

        //set the rate button action        
        rateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(final ActionEvent e) {
                Thread rateThread = new Thread(new RaterThread());
                rateThread.start();
                rateButton.setDisable(true);
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
                        //Donot consider video files less than 100 mb
                        final Long FileSizeInMB = file.length() / 1048576;
                        if (FileSizeInMB < 100) {
                            continue;
                        }
                        final String fileName = FilenameUtils.removeExtension(file.getName());
                        if (!movieNameFilter(fileName).isEmpty()) {
                            yourMovieNameList.add(movieNameFilter(fileName));
                        } else {
                            yourMovieNameList.add(fileName);
                        }
                    }
                }
            }
        }
    }

    class RaterThread extends Thread {

        public void run() {
            doRating();
        }

        private void doRating() {
            //For each movie:
            for (final String movieName : yourMovieNameList) {

                final String apiurl = "http://www.omdbapi.com/";

                String tempMovieName = movieName;

                while (true) {
                    try {
                        // Forming a complete url ready to send
                        final String restURLLink = apiurl + "?t=" + tempMovieName + "&type=movie";
                        final URL url = new URL(restURLLink);
                        URLConnection omdbConnection = url.openConnection();
                        final DataInputStream dataInputStream = new DataInputStream(omdbConnection.getInputStream());

                        ObjectMapper mapper = new ObjectMapper();
                        mapper.setPropertyNamingStrategy(new MyNameStrategy());
                        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        final MovieInfo moviePojo = mapper.readValue(dataInputStream, MovieInfo.class);
                        if (!moviePojo.getResponse().equals("False") || tempMovieName.length() < 5) {
                            moviePojo.setFileName(movieName);
                            //Add to table
                            addDataToTable(moviePojo);
                            break;
                        } else {
                            tempMovieName = tempMovieName.substring(0, tempMovieName.length() - 1);
                        }
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(JavaFXApplication.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(JavaFXApplication.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }

        }

        private void addDataToTable(final MovieInfo moviePojo) {
            tableData.add(moviePojo);
            table.setItems(tableData);
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

        //Replace all two or more continious spaces by single space
        fileName = fileName.replaceAll("\\s+", " ");

        //remove all spaces in front and back
        fileName = fileName.trim();

        //convert to lowercase
        fileName = fileName.toLowerCase();

        //add + instead of spaces
        fileName = fileName.replace(" ", "+");

        return fileName;
    }
}