package pw.chew.mlb.objects;

import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.utils.FileUpload;
import pw.chew.mlb.commands.GameInfoCommand;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ImageUtil {
    /**
     * Creates a match up banner for the given teams
     *
     * @param awayId the away team ID
     * @param homeId the home team ID
     * @return a FileUpload of the banner
     */
    public static GeneratedImage matchUpBanner(int awayId, int homeId) {
        Graphics g;

        BufferedImage image = new BufferedImage(1600, 640, BufferedImage.TYPE_INT_RGB);
        g = image.createGraphics();  // not sure on this line, but this seems more right

        // Add team logos
        // Add images to the imageBufferedImage
        // download https://midfield.mlbstatic.com/v1/team/140/spots/256 (a png)
        Image awayLogo = null, homeLogo = null;
        try {
            // now we can get the photos
            awayLogo = Toolkit.getDefaultToolkit().createImage(new URL("https://midfield.mlbstatic.com/v1/team/%s/spots/256".formatted(awayId)));
            homeLogo = Toolkit.getDefaultToolkit().createImage(new URL("https://midfield.mlbstatic.com/v1/team/%s/spots/256".formatted(homeId)));

            // wait for the images to load
            MediaTracker tracker = new MediaTracker(new java.awt.Container());
            tracker.addImage(awayLogo, 0);
            tracker.addImage(homeLogo, 0);

            tracker.waitForAll();

            Toolkit.getDefaultToolkit().prepareImage(awayLogo, -1, -1, null);
            Toolkit.getDefaultToolkit().prepareImage(homeLogo, -1, -1, null);
        } catch (MalformedURLException | InterruptedException ignored) {}
        if (awayLogo == null || homeLogo == null) {
            return null;
        }

        // grab the background color, by getting the color of the pixel at 0,0
        // this is the color of the background of the logos
        // we can't do getGraphics(), because it's not valid since we used createImage()
        // first we make the rangersLogo a BufferedImage, then we get the color of the pixel at 0,0
        BufferedImage awayLogoBufferedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        awayLogoBufferedImage.getGraphics().drawImage(awayLogo, 0, 0, null);
        Color awayColor = new Color(awayLogoBufferedImage.getRGB(128, 10));

        BufferedImage homeLogoBufferedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        homeLogoBufferedImage.getGraphics().drawImage(homeLogo, 0, 0, null);
        Color homeColor = new Color(homeLogoBufferedImage.getRGB(128, 10));

        // AWAY TEAM
        g.setColor(awayColor);

        // The colors will be split down the middle, at about 400px, but the line is at a 10degree angle
        g.fillPolygon(new int[]{0, 900, 700, 0}, new int[]{0, 0, 640, 640}, 4);

        // HOME TEAM
        g.setColor(homeColor);
        g.fillPolygon(new int[]{1600, 900, 700, 1600}, new int[]{0, 0, 640, 640}, 4);

        // White line down the middle lol
        g.setColor(Color.WHITE);
        g.fillPolygon(new int[]{890, 900, 710, 700}, new int[]{0, 0, 640, 640}, 4);

        g.drawImage(awayLogo, 220, 140, 360, 360, null);
        g.drawImage(homeLogo, 1050, 140, 360, 360, null);

        // Convert BufferedImage to InputStream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());

            return new GeneratedImage(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a table (2D Array) of data into an image.
     *
     * @param data the data to convert
     * @param set the data set to use. pulls headers and widths.
     * @return a GeneratedImage object
     */
    public static GeneratedImage createTable(String[][] data, GameInfoCommand.BoxScoreDataSets set) {
        // set widths per column
        int[] cellWidths = set.columnWidths();
        int currentWidth = 0;
        int totalWidth = set.totalWidth();

        // set cell height and padding
        int cellHeight = 30;
        int padding = 5;

        // calculate final image size
        int imageWidth = (data[0].length * (totalWidth / set.length()) + padding * (data[0].length + 1)) + 10;
        int imageHeight = data.length * cellHeight + padding * (data.length + 1);

        // create image and graphics
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // set background color to #141A1F
        g2d.setColor(new Color(0x141A1F));
        g2d.fillRect(0, 0, imageWidth, imageHeight);

        // Set font to MLB font
        Font font = new Font("Proxima Nova", Font.PLAIN, 14);
        g2d.setFont(font);

        // iterate through data and write it onto the image
        for (int row = 0; row < data.length; row++) {
            boolean lastRow = data[row][0].equals("Totals");
            if (lastRow || row == 0) {
                // make font bold
                g2d.setFont(new Font("Proxima Nova", Font.BOLD, 14));
            } else {
                // make font normal
                g2d.setFont(font);
            }
            for (int col = 0; col < data[row].length; col++) {
                // determine where to put it
                int x = col + currentWidth + (col + 1) * padding;
                int y = row * cellHeight + (row + 1) * padding;

                g2d.setColor(Color.WHITE);

                // first column is left aligned
                if (col == 0) {
                    g2d.drawString(data[row][col], x + padding, y + cellHeight - padding - 2);
                } else { // the rest are centered
                    g2d.drawString(data[row][col], x + (cellWidths[col] - g2d.getFontMetrics().stringWidth(data[row][col])) / 2, y + cellHeight - padding - 2);
                }

                // set the width of the cell
                currentWidth += cellWidths[col];
            }
            // set width back to 0
            currentWidth = 0;
        }

        // add a border-line between the 1st and 2nd row
        g2d.setColor(new Color(0x263340));
        g2d.drawLine(0, cellHeight + padding, imageWidth, cellHeight + padding);

        // we're done with the image
        g2d.dispose();

        // Convert BufferedImage to InputStream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());

            // return generated image
            return new GeneratedImage(is);
        } catch (IOException e) {
            e.printStackTrace();
            return new GeneratedImage(null);
        }
    }

    public record GeneratedImage(InputStream stream) {
        public boolean failed() {
            return stream == null;
        }

        public FileUpload asFileUpload() {
            return FileUpload.fromData(stream, "image.png");
        }

        public Icon asIcon() {
            try {
                return Icon.from(stream);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
