import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;

public class KmeansLab extends Application {

    Cluster[] clusters;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8));

        Button buttonLoad = new Button("Load Image");
        Button buttonCalc = new Button("Do K-means");
        buttonCalc.setDisable(true);

        Spinner<Integer> spinnerCount = new Spinner<>(1, 30, 1);
        spinnerCount.setPrefWidth(64);

        ImageView defaultImage = new ImageView();

        TitledPane pane1 = new TitledPane("Default Image", defaultImage);
        pane1.setAlignment(Pos.CENTER);
        pane1.setCollapsible(false);
        pane1.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setFillWidth(pane1, true);
        GridPane.setFillHeight(pane1, true);

        defaultImage.fitWidthProperty().bind(stage.widthProperty().divide(2).subtract(100));
        defaultImage.fitHeightProperty().bind(stage.heightProperty().subtract(200));
        defaultImage.setPreserveRatio(true);
        ImageView kmeansImage = new ImageView();

        TitledPane pane2 = new TitledPane("K-means Image", kmeansImage);
        pane2.setAlignment(Pos.CENTER);
        pane2.setCollapsible(false);
        pane2.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setFillWidth(pane2, true);
        GridPane.setFillHeight(pane2, true);

        kmeansImage.fitWidthProperty().bind(stage.widthProperty().divide(2).subtract(100));
        kmeansImage.fitHeightProperty().bind(stage.heightProperty().subtract(200));
        kmeansImage.setPreserveRatio(true);

        HBox hboxColors = new HBox(10);
        hboxColors.setAlignment(Pos.CENTER);

            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image", "*.jpg", "*.jpeg", "*.bmp", "*.png"),
                    new FileChooser.ExtensionFilter("JPEG Image", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("BMP Image", "*.bmp"),
                    new FileChooser.ExtensionFilter("PNG Image", "*.png"));

        grid.add(buttonLoad, 0, 0);
        grid.add(spinnerCount, 1,0);
        grid.add(buttonCalc, 2,0);

        grid.add(pane1, 0, 1);
        grid.add(pane2, 2, 1);

        grid.add(hboxColors, 0, 2, 3, 1);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        column1.setFillWidth(true);
        column1.setHalignment(HPos.CENTER);
        grid.getColumnConstraints().add(column1);

        ColumnConstraints column2 = new ColumnConstraints(100);
        column2.setMinWidth(100);
        column2.setHalignment(HPos.CENTER);
        grid.getColumnConstraints().add(column2);

        ColumnConstraints column3 = new ColumnConstraints();
        column3.setHgrow(Priority.ALWAYS);
        column3.setFillWidth(true);
        column3.setHalignment(HPos.CENTER);
        grid.getColumnConstraints().add(column3);

        RowConstraints row1 = new RowConstraints(50);
        grid.getRowConstraints().add(row1);

        RowConstraints row2 = new RowConstraints();
        row2.setVgrow(Priority.ALWAYS);
        row2.setFillHeight(true);
        grid.getRowConstraints().add(row2);

        RowConstraints row3 = new RowConstraints(50);
        grid.getRowConstraints().add(row3);

        final BufferedImage[] bufferedImage = { null, null };
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        buttonLoad.setOnAction(event -> {
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                Image input = new Image(selectedFile.toURI().toString());
                defaultImage.setImage(input);
                hboxColors.getChildren().clear();
                try {
                    bufferedImage[0] = ImageIO.read(selectedFile);
                    int w = bufferedImage[0].getWidth();
                    int h = bufferedImage[0].getHeight();

                    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    Graphics2D gr = bi.createGraphics();
                    gr.setBackground(java.awt.Color.WHITE);
                    gr.clearRect(0, 0, w, h);

                    bufferedImage[1] = bi;
                    Image output = SwingFXUtils.toFXImage(bi, null);

                    kmeansImage.setImage(output);
                    kmeansImage.setVisible(false);
                    buttonCalc.setDisable(false);
                } catch (IOException e) {
                    System.out.println("Can't read selected file");
                    buttonCalc.setDisable(true);
                }
            }
            else {
                alert.setTitle("Information");
                alert.setHeaderText(null);
                alert.setContentText("You have not selected an image");
                System.out.println("A file is invalid");
                alert.showAndWait();
            }
        });
        buttonCalc.setOnAction(event -> {
            if (bufferedImage[0] != null) {
                bufferedImage[1] = clustering(bufferedImage[0], spinnerCount.getValue());
                Image output = SwingFXUtils.toFXImage(bufferedImage[1], null);
                kmeansImage.setImage(output);
                kmeansImage.setVisible(true);

                hboxColors.getChildren().clear();

                for (Cluster c : clusters) {
                    Label label = new Label();
                    label.setPadding(new Insets(10,12,10,12));
                    label.setBackground(new Background(new BackgroundFill(
                            Color.rgb(c.red, c.green, c.blue),
                            CornerRadii.EMPTY,
                            Insets.EMPTY)));
                    hboxColors.getChildren().add(label);
                }
            }
        });

        Scene scene = new Scene(grid, 800, 500);
        stage.setTitle("K-means");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.show();
    }
    public BufferedImage clustering(BufferedImage image, int k) {

        int w = image.getWidth();
        int h = image.getHeight();
        clusters = createClusters(image, k);
        int[] lut = new int[w * h];
        Arrays.fill(lut, -1);
        boolean pixelChangedCluster = true;
        while (pixelChangedCluster) {
            pixelChangedCluster = false;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = image.getRGB(x, y);
                    Cluster cluster = findMinimalCluster(pixel);
                    if (lut[w * y + x] != cluster.getId()) {
                            if (lut[w * y + x] != -1) {
                                clusters[lut[w * y + x]].removePixel(pixel);
                            }
                            cluster.addPixel(pixel);
                        pixelChangedCluster = true;
                        lut[w * y + x] = cluster.getId();
                    }
                }
            }
        }

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int clusterId = lut[w * y + x];
                result.setRGB(x, y, clusters[clusterId].getRGB());
            }
        }
        return result;
    }

    public Cluster[] createClusters(BufferedImage image, int k) {
        Cluster[] result = new Cluster[k];
        int x = 0;
        int y = 0;
        int dx = image.getWidth() / k;
        int dy = image.getHeight() / k;
        for (int i = 0; i < k; i++) {
            result[i] = new Cluster(i, image.getRGB(x, y));
            x += dx;
            y += dy;
        }

        return result;
    }

    public Cluster findMinimalCluster(int rgb) {
        Cluster cluster = null;
        int min = Integer.MAX_VALUE;
        for (Cluster value : clusters) {
            int distance = value.distance(rgb);
            if (distance < min) {
                min = distance;
                cluster = value;
            }
        }
        return cluster;
    }

    class Cluster {
        int id;
        int pixelCount;
        int red;
        int green;
        int blue;
        int reds;
        int greens;
        int blues;

        public Cluster(int id, int rgb) {
            int r = rgb >> 16 & 0x000000FF;
            int g = rgb >> 8 & 0x000000FF;
            int b = rgb >> 0 & 0x000000FF;
            red = r;
            green = g;
            blue = b;
            this.id = id;
            addPixel(rgb);
        }

        int getId() {
            return id;
        }

        int getRGB() {
            int r = reds / pixelCount;
            int g = greens / pixelCount;
            int b = blues / pixelCount;

            return 0xff000000 | r << 16 | g << 8 | b;
        }

        void addPixel(int color) {
            int r = color >> 16 & 0x000000FF;
            int g = color >> 8 & 0x000000FF;
            int b = color >> 0 & 0x000000FF;
            reds += r;
            greens += g;
            blues += b;
            pixelCount++;
            red = reds / pixelCount;
            green = greens / pixelCount;
            blue  = blues / pixelCount;
        }

        void removePixel(int color) {
            int r = color >> 16 & 0x000000FF;
            int g = color >> 8 & 0x000000FF;
            int b = color >> 0 & 0x000000FF;
            reds -= r;
            greens -= g;
            blues -= b;
            pixelCount--;
            red = reds / pixelCount;
            green = greens / pixelCount;
            blue = blues / pixelCount;
        }

        int distance(int color) {
            int r = color >> 16 & 0x000000FF;
            int g = color >> 8 & 0x000000FF;
            int b = color >> 0 & 0x000000FF;
            int rx = Math.abs(red - r);
            int gx = Math.abs(green - g);
            int bx = Math.abs(blue - b);
            int d = (rx + gx + bx) / 3;

            return d;
        }
    }
}