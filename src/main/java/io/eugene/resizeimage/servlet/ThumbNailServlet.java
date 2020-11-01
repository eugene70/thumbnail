package io.eugene.resizeimage.servlet;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.MultiStepRescaleOp;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class ThumbNailServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        if (type == null) {
            type = "1";
        }
        String servletPath = req.getServletPath();
        String imagePath = servletPath.substring(0, servletPath.lastIndexOf(".thumb"));
        ServletContext context = this.getServletContext();
        String path = context.getRealPath(imagePath);
        File file = new File(path);
        BufferedImage img = ImageIO.read(file);
        BufferedImage resized;
        switch (type) {
            case "2":
                resized = getResizedToWidth(img, 250);
                break;
            case "1":
            default:
                resized = getResizedToSquare(img, 250, 0.0);
                break;
        }
        ServletOutputStream out = resp.getOutputStream();
        ImageIO.write(resized, "JPG", out);
        img.flush();
        resized.flush();
        out.close();
    }

    private BufferedImage getResizedToWidth(BufferedImage img, int width) {
        if (width > img.getWidth())
            throw new IllegalArgumentException("Width "+ width +" exceeds width of image, which is "+ img.getWidth());
        int nHeight = width * img.getHeight() / img.getWidth();
        MultiStepRescaleOp rescale = new MultiStepRescaleOp(width, nHeight);
        rescale.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Soft);
        return rescale.filter(img, null);
    }


    private BufferedImage crop(BufferedImage img, int x1, int y1, int x2, int y2) {
        if (x1 < 0 || x2 <= x1 || y1 < 0 || y2 <= y1 || x2 > img.getWidth() || y2 > img.getHeight())
            throw new IllegalArgumentException("invalid crop coordinates");

        int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType();
        int nNewWidth = x2 - x1;
        int nNewHeight = y2 - y1;
        BufferedImage cropped = new BufferedImage(nNewWidth, nNewHeight, type);
        Graphics2D g = cropped.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setComposite(AlphaComposite.Src);

        g.drawImage(img, 0, 0, nNewWidth, nNewHeight, x1, y1, x2, y2, null);
        g.dispose();

        return cropped;
    }

    private BufferedImage getResizedToSquare(BufferedImage img, int width, double cropEdgesPct) {
        if (cropEdgesPct < 0 || cropEdgesPct > 0.5)
            throw new IllegalArgumentException("Crop edges pct must be between 0 and 0.5. "+ cropEdgesPct +" was supplied.");
        if (width > img.getWidth())
            throw new IllegalArgumentException("Width "+ width +" exceeds width of image, which is "+ img.getWidth());
        //crop to square first. determine the coordinates.
        int cropMargin = (int)Math.abs(Math.round(((img.getWidth() - img.getHeight()) / 2.0)));
        int x1 = 0;
        int y1 = 0;
        int x2 = img.getWidth();
        int y2 = img.getHeight();
        if (img.getWidth() > img.getHeight()) {
            x1 = cropMargin;
            x2 = x1 + y2;
        }
        else {
            y1 = cropMargin;
            y2 = y1 + x2;
        }

        //should there be any edge cropping?
        if (cropEdgesPct != 0) {
            int cropEdgeAmt = (int)((double)(x2 - x1) * cropEdgesPct);
            x1 += cropEdgeAmt;
            x2 -= cropEdgeAmt;
            y1 += cropEdgeAmt;
            y2 -= cropEdgeAmt;
        }

        // generate the image cropped to a square
        BufferedImage cropped = crop(img, x1, y1, x2, y2);

        // now resize. we do crop first then resize to preserve detail
        BufferedImage resized = getResizedToWidth(cropped, width);

        cropped.flush();

        return resized;
    }

}
