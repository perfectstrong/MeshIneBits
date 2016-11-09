package bitSlicer;

import java.util.Vector;

import bitSlicer.PatternTemplates.PatternTemplate;
import bitSlicer.PatternTemplates.PatternTemplate1;
import bitSlicer.PatternTemplates.PatternTemplate2;
import bitSlicer.Slicer.Slice;
import bitSlicer.Slicer.Config.CraftConfig;
import bitSlicer.util.Segment2D;
import bitSlicer.util.Vector2;

public class GeneratedPart {
	
	private Vector<Slice> slices;
	private double skirtRadius;
	private Vector<Layer> layers;
	private PatternTemplate patternTemplate;
	
	public GeneratedPart(Vector<Slice> slices){
		this.slices = slices;
		setSkirtRadius();
		build();
	}
	
	/*
	 * skirtRadius is the radius of the cylinder that fully contains the part.
	 */
	public void setSkirtRadius(){
		
		double radius = 0;
		
		for (Slice s : slices){
			for (Segment2D segment : s.getSegmentList()){
				if (segment.start.vSize2() > radius)
					radius = segment.start.vSize2();
				if(segment.end.vSize2() > radius)
					radius = segment.end.vSize2();
			}
		}
		
		skirtRadius = Math.sqrt(radius);
	}
	
	public void build(){
		setPatternTemplate(CraftConfig.patternNumber, CraftConfig.rotation, CraftConfig.offset);
		buildLayers();
	}
	
	public void setPatternTemplate(int templateNumber, Vector2 rotation, Vector2 offSet){
		switch (templateNumber){
		case 1:
			patternTemplate = new PatternTemplate1(rotation, offSet, skirtRadius);
			break;
		case 2:
			patternTemplate = new PatternTemplate2(rotation, offSet, skirtRadius);
		}
	}
	
	public PatternTemplate getPatternTemplate(){
		return patternTemplate;
	}
	
	public void buildLayers(){
		
		Vector<Slice> slicesCopy = slices;
		double bitThickness = CraftConfig.bitThickness;
		double sliceHeight = CraftConfig.sliceHeight;
		double z = CraftConfig.firstSliceHeightPercent * sliceHeight;
		int layerNumber = 1;
		
		while (!slicesCopy.isEmpty()){
			Vector<Slice> includedSlices = new Vector<Slice>();
			while ((z < bitThickness * layerNumber) && !slicesCopy.isEmpty()){
				includedSlices.add(slicesCopy.get(0));
				slicesCopy.remove(0);
				z = z + bitThickness;
			}
			layers.add(new Layer(includedSlices, layerNumber, this));
			layerNumber++;
		}
	}

}