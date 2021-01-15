package meshIneBits.IA;

import meshIneBits.Bit2D;
import meshIneBits.IA.deeplearning.Acquisition;
import meshIneBits.IA.deeplearning.DataPreparation;
import meshIneBits.IA.deeplearning.NNExploitation;
import meshIneBits.IA.genetics.Genetic;
import meshIneBits.config.CraftConfig;
import meshIneBits.gui.view2d.MeshController;
import meshIneBits.slicer.Slice;
import meshIneBits.util.Segment2D;
import meshIneBits.util.Vector2;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/**
 * An AI_Tool lets the user pave the whole mesh, with artificial intelligence.
 * AI_Tool is based on a neural network that learns from how does a human place bits on the bounds of a Slice.
 */
public class AI_Tool {
    public static Genetic genetic;
    private static MeshController meshController;
    private static final Map<Slice, Vector<Segment2D>> sliceMap = new LinkedHashMap();

    private static final Vector<Bit2D> bits = new Vector<>(); //the bits placed by the AI

    /**
     * Pave the whole mesh with AI.
     */
    public static void startAI(Slice slice) throws Exception {
        bits.clear();
        Vector<Slice> slicesList = meshController.getMesh().getSlices();
        for (Slice currentSlice : slicesList) {
            sliceMap.put(
                    currentSlice,
                    (Vector<Segment2D>) currentSlice.getSegmentList().clone()
            );
        }
        meshController.AIneedPaint = true;

        Vector<Vector<Vector2>> bounds = DataPreparation.getBoundsAndRearrange(slice);
        for (Vector<Vector2> bound : bounds) {
            Vector2 veryFirstStartPoint = bound.get(0);
            Vector2 startPoint = bound.get(0);
            Vector<Vector2> associatedPoints = DataPreparation.getSectionPointsFromBound(bound, startPoint);

            int nbIterations = 0;
            while (!hasCompletedTheBound(veryFirstStartPoint, associatedPoints)) { //Add each bit on the bound

                nbIterations++;
                if (nbIterations > 50)//number max of bits to place before stopping
                    break;

                Vector<Vector2> sectionPoints = DataPreparation.getSectionPointsFromBound(bound, startPoint);
                double angleLocalSystem = DataPreparation.getLocalCoordinateSystemAngle(sectionPoints);

                NNExploitation nnExploitation = new NNExploitation();
                Vector<Vector2> sectionPointsReg = DataPreparation.getInputPointsForDL(sectionPoints);
                Bit2D bit = nnExploitation.getBit(sectionPointsReg, startPoint, angleLocalSystem);
                bits.add(bit);
                System.out.println("size:" + bits.size());

                startPoint = DataPreparation.getNextBitStartPoint(bit, bound);

            }
        }
    }

    /**
     * Check if the bound of the Slice has been entirely paved.
     *
     * @param veryFirststartPoint       the point of the bound on which the very first bit was placed.
     * @param associatedPoints the points on which a bit has just been placed.
     * @return <code>true</code> if the bound of the Slice has been entirely paved. <code>false</code> otherwise.
     */
    private static boolean hasCompletedTheBound(Vector2 veryFirststartPoint, Vector<Vector2> associatedPoints) {
        //todo @Etienne debug hasCompletedTheBound
        if (associatedPoints.firstElement() == veryFirststartPoint)
            return false;
        if (Vector2.dist(veryFirststartPoint,associatedPoints.firstElement())< CraftConfig.errorAccepted)
            return true;
        return associatedPoints.contains(veryFirststartPoint);
    }

    /**
     * @return the meshController
     */
    public static MeshController getMeshController() {
        return meshController;
    }

    public void setMeshController(MeshController meshController) {
        AI_Tool.meshController = meshController;
    }

    /**
     * @return the map of the slices, with Vector of Segment2D associated to each Slice
     */
    public static Map<Slice, Vector<Segment2D>> getSliceMap() {
        return sliceMap;
    }

    public static Vector<Bit2D> getBits() {
        return bits;
    }
}