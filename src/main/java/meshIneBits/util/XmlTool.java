package meshIneBits.util;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Vector;

import meshIneBits.Bit3D;
import meshIneBits.GeneratedPart;
import meshIneBits.Layer;
import meshIneBits.config.CraftConfig;

public class XmlTool {

	private GeneratedPart part;
	private PrintWriter writer;
	private Path filePath;

	public XmlTool(GeneratedPart part, Path fileLocation) {
		this.part = part;
		this.filePath = fileLocation;
		setFileToXml();
	}

	public String getNameFromFileLocation() {
		return filePath.getFileName().toString().split("[.]")[0];
	}

	private boolean liftableBit(Bit3D bit) {
		int liftableSubBit = 0;
		for (Vector2 p : bit.getLiftPoints()) {
			if (p != null) {
				liftableSubBit++;
			}
		}
		if (liftableSubBit > 0) {
			return true;
		} else {
			return false;
		}
	}

	private void setFileToXml() {
		String fileName = filePath.getFileName().toString();
		if (fileName.split("[.]").length >= 2) {
			fileName = fileName.split("[.]")[0];
		}
		fileName = fileName + "." + "xml";
		filePath = Paths.get(filePath.getParent().toString() + "\\" + fileName);
	}

	private void startFile() {
		writer.println("	<name>" + getNameFromFileLocation() + "</name>");
		writer.println("	<date>" + (new Date()).toString() + "</date>");
		writer.println("	<bitDimension>");
		writer.println("		<height>" + CraftConfig.bitThickness + "</height>");
		writer.println("		<width>" + CraftConfig.bitWidth + "</width>");
		writer.println("		<length>" + CraftConfig.bitLength + "</length>");
		writer.println("	</bitDimension>");
		writer.println("	<partSkirt>");
		writer.println("		<height>" + (((part.getLayers().size() + CraftConfig.layersOffset) * CraftConfig.bitThickness) - CraftConfig.layersOffset) + "</height>");
		writer.println("		<radius>" + part.getSkirtRadius() + "</radius>");
		writer.println("	</partSkirt>");
	}

	private void writeBit(Bit3D bit, int id) {

		if (!liftableBit(bit)) {
			return;
		}

		writer.println("		<bit>");
		writer.println("			<id>" + id + "</id>");
		writer.println("			<cut>");
		if (bit.getCutPaths() != null) {
			for (Path2D p : bit.getCutPaths()) {
				writeCutPaths(p);
			}
		}
		writer.println("			</cut>");
		writeSubBits(bit);
		writer.println("		</bit>");
	}

	private void writeCutPaths(Path2D p) {

		Vector<double[]> points = new Vector<double[]>();
		for (PathIterator pi = p.getPathIterator(null); !pi.isDone(); pi.next()) {
			double[] coords = new double[6];
			int type = pi.currentSegment(coords);
			double[] point = { type, coords[0], coords[1] };
			points.add(point);
		}

		boolean waitingForMoveTo = true;
		Vector<double[]> pointsToAdd = new Vector<double[]>();
		for (double[] point : points) {
			if ((point[0] == PathIterator.SEG_LINETO) && waitingForMoveTo) {
				pointsToAdd.add(point);
			} else if ((point[0] == PathIterator.SEG_LINETO) && !waitingForMoveTo) {
				writer.println("				<lineTo>");
				writer.println("					<x>" + point[1] + "</x>");
				writer.println("					<y>" + point[2] + "</y>");
				writer.println("				</lineTo>");
			} else {
				writer.println("				<moveTo>");
				writer.println("					<x>" + point[1] + "</x>");
				writer.println("					<y>" + point[2] + "</y>");
				writer.println("				</moveTo>");
				waitingForMoveTo = false;
			}
		}

		for (double[] point : pointsToAdd) {
			writer.println("					<lineTo>");
			writer.println("						<x>" + point[1] + "</x>");
			writer.println("						<y>" + point[2] + "</y>");
			writer.println("					</lineTo>");
		}

	}

	private void writeLayer(Layer layer) {
		writer.println("	<layer>");
		writer.println("		<z>" + (layer.getLayerNumber() * (CraftConfig.bitThickness + CraftConfig.layersOffset)) + "</z>");
		for (int i = 0; i < layer.getBits3dKeys().size(); i++) {
			writeBit(layer.getBit3D(layer.getBits3dKeys().get(i)), i);
		}
		writer.println("	</layer>");
	}

	private void writeSubBits(Bit3D bit) {
		for (int id = 0; id < bit.getLiftPoints().size(); id++) {
			if (bit.getLiftPoints().get(id) != null) {
				writer.println("			<subBit>");
				writer.println("				<id>" + id + "</id>");
				writer.println("				<liftPoint>");
				writer.println("					<x>" + bit.getLiftPoints().get(id).x + "</x>");
				writer.println("					<y>" + bit.getLiftPoints().get(id).y + "</y>");
				writer.println("				</liftPoint>");
				writer.println("				<rotation>" + bit.getOrientation().getEquivalentAngle() + "</rotation>");
				writer.println("				<position>");
				writer.println("					<x>" + bit.getDepositPoints().get(id).x + "</x>");
				writer.println("					<y>" + bit.getDepositPoints().get(id).y + "</y>");
				writer.println("				</position>");
				writer.println("			</subBit>");
			}
		}
	}

	public void writeXmlCode() {
		try {
			writer = new PrintWriter(filePath.toString(), "UTF-8");
			writer.println("<part>");
			startFile();
			Logger.updateStatus("Generating XML file");
			for (int i = 0; i < part.getLayers().size(); i++) {
				writeLayer(part.getLayers().get(i));
				Logger.setProgress(i, part.getLayers().size() - 1);
			}
			writer.println("</part>");
			Logger.message("The XML file has been generated and saved in " + filePath);
		} catch (Exception e) {
			Logger.error("The XML file has not been generated");
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

}