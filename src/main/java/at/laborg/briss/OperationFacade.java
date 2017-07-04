package at.laborg.briss;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;

import com.itextpdf.text.DocumentException;

import at.laborg.briss.exception.CropException;
import at.laborg.briss.model.ClusterDefinition;
import at.laborg.briss.model.CropDefinition;
import at.laborg.briss.model.PageCluster;
import at.laborg.briss.model.WorkingSet;
import at.laborg.briss.utils.DesktopHelper;
import at.laborg.briss.utils.DocumentCropper;

public class OperationFacade {
	public static void cropAndSave(File file, WorkingSet workingSet, File lastOpenDir)
			throws IOException, DocumentException, CropException {
		CropDefinition cropDefinition = CropDefinition.createCropDefinition(workingSet.getSourceFile(),
		        file, workingSet.getClusterDefinition());
		File result = DocumentCropper.crop(cropDefinition);
		if (result != null) {
		    DesktopHelper.openFileWithDesktopApp(result);
		    lastOpenDir = result.getParentFile();
		}
	}

	public static void createAndExecuteCropJobForPreview(WorkingSet workingSet)
			throws IOException, DocumentException, CropException {
        File tmpCropFileDestination = File.createTempFile("briss", ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
        CropDefinition cropDefinition = CropDefinition.createCropDefinition(workingSet.getSourceFile(),
                tmpCropFileDestination, workingSet.getClusterDefinition());
        File result = DocumentCropper.crop(cropDefinition);
        DesktopHelper.openFileWithDesktopApp(result);;
    }

    public static void copyCropsToClusters(ClusterDefinition oldClusters, ClusterDefinition newClusters) {

        for (PageCluster newCluster : newClusters.getClusterList()) {
            for (Integer pageNumber : newCluster.getAllPages()) {
                PageCluster oldCluster = oldClusters.getSingleCluster(pageNumber);
                for (Float[] ratios : oldCluster.getRatiosList()) {
                    newCluster.addRatios(ratios);
                }
            }
        }
    }
    
    public static void processClusters(ClusterDefinition clusterDefinition, File source, Consumer<Integer> callback) {
        
		int workerUnitCounter = 1;
		PdfDecoder pdfDecoder = new PdfDecoder();
		try {
			pdfDecoder.openPdfFile(source.getAbsolutePath());
		} catch (PdfException e1) {
			e1.printStackTrace();
		}

		for (PageCluster cluster : clusterDefinition.getClusterList()) {
			for (Integer pageNumber : cluster.getPagesToMerge()) {
				// TODO jpedal isn't able to render big images
				// correctly, so let's check if the image is big an
				// throw it away
				try {
					if (cluster.getImageData().isRenderable()) {
						BufferedImage renderedPage = pdfDecoder.getPageAsImage(pageNumber);
						cluster.getImageData().addImageToPreview(renderedPage);
						callback.accept(100*workerUnitCounter++/clusterDefinition.getNrOfPagesToRender());
					}
				} catch (PdfException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		// now close the reader as it's not used anymore
		pdfDecoder.closePdfFile();
	}
}
