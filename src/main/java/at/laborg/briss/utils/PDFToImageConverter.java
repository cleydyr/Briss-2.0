package at.laborg.briss.utils;

import org.openpdf.renderer.PDFFile;
import org.openpdf.renderer.PDFPage;
import org.openpdf.renderer.decrypt.PDFPassword;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class PDFToImageConverter implements AutoCloseable {
    private final FileInputStream fis;

    private final FileChannel fc;

    private final PDFFile pdfFile;

    public PDFToImageConverter(String path, String password) throws IOException {
        File file = new File(path);

        this.fis = new FileInputStream(file);

        this.fc = fis.getChannel();

        ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

        this.pdfFile = new PDFFile(bb, new PDFPassword(password));
    }

    public BufferedImage getAsImage(int pageIndex) {
        PDFPage page = this.pdfFile.getPage(pageIndex);

        int width = (int) Math.ceil(page.getBBox().getWidth());
        int height = (int) Math.ceil(page.getBBox().getHeight());

        if (width <= 0 || height <= 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        Image pageImage = page.getImage(width, height, null, null, true, true);

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        if (pageImage != null) {
            graphics.drawImage(pageImage, 0, 0, width, height, null);
        }

        graphics.dispose();

        return bufferedImage;
    }

    public static void main(String[] args) {

    }

    public void close() throws Exception {
        this.fis.close();
        this.fc.close();
    }
}
