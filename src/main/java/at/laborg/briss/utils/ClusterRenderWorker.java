/**
 *
 */
package at.laborg.briss.utils;

import at.laborg.briss.model.ClusterDefinition;
import at.laborg.briss.model.PageCluster;

import java.awt.image.BufferedImage;
import java.io.File;

public class ClusterRenderWorker extends Thread {

	public int workerUnitCounter = 1;
	private final File source;
	private final ClusterDefinition clusters;
	private final String password;

	public ClusterRenderWorker(final File source, String password, final ClusterDefinition clusters) {
		super();
		this.source = source;
		this.clusters = clusters;
		this.password = password;
	}

	@Override
	public final void run() {
        try (PDFToImageConverter converter = new PDFToImageConverter(source.getAbsolutePath(), password)) {
            for (PageCluster cluster : clusters.getClusterList()) {
                for (Integer pageNumber : cluster.getPagesToMerge()) {
                    BufferedImage renderedPage = converter.getAsImage(pageNumber);
                    cluster.getImageData().addImageToPreview(renderedPage);
                    workerUnitCounter++;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
	}
}
