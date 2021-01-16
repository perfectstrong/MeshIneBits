package meshIneBits.artificialIntelligence.deeplearning;

import meshIneBits.Bit2D;
import meshIneBits.artificialIntelligence.AI_Tool;
import meshIneBits.artificialIntelligence.GeneralTools;
import meshIneBits.artificialIntelligence.util.Curve;
import meshIneBits.config.CraftConfig;
import meshIneBits.slicer.Slice;
import meshIneBits.util.Polygon;
import meshIneBits.util.Segment2D;
import meshIneBits.util.Vector2;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.awt.geom.Area;
import java.util.Vector;
import java.util.stream.IntStream;

//todo @Etienne@Andre documenter aussi toutes les classes
public final class DataPreparation {

    /**
     * Returns the points of each bound of a given Slice
     * The points are rearranged to be in the correct order.
     * @see #rearrangeSegments
     * @see #rearrangePoints
     *
     * @param currentSlice the slice to get the bounds
     * @return the bounds of the given slice, once rearranged
     */
    @SuppressWarnings("unchecked")
    public static Vector<Vector<Vector2>> getBoundsAndRearrange(Slice currentSlice) {
        Vector<Vector<Vector2>> boundsList = new Vector<>();
        Vector<Vector<Segment2D>> borderList = rearrangeSegments((Vector<Segment2D>) currentSlice.getSegmentList().clone());

        for (Vector<Segment2D> border : borderList) {
            Vector<Vector2> unorderedPoints = computePoints(border);
            boundsList.add(rearrangePoints(unorderedPoints));
        }
        return boundsList;
    }

    /**
     * Rearranges the given segments so that each segment follows the previous one.
     * @param segmentList the segments to rearranged
     * @return the rearranged segments. Returns more than one Vector of Segment2D if there's more than one bound on the Slice
     */
    private static Vector<Vector<Segment2D>> rearrangeSegments(Vector<Segment2D> segmentList) {
        Vector<Vector<Segment2D>> list = new Vector<>();
        Vector<Segment2D> newSegmentList = new Vector<>();
        newSegmentList.add(segmentList.get(0));
        list.add(newSegmentList);

        while (!segmentList.isEmpty()) {
            searchNextSegment(segmentList.get(0), segmentList, newSegmentList);
            newSegmentList = new Vector<>();
            list.add(newSegmentList);
        }
        list.removeIf(Vector::isEmpty);
        return list;
    }

    /**
     * Rearranges the given points so that the list begins at the rightmost point
     * @param pointList the points to be rearranged.
     * @return  the rearranged points.
     */
    private static Vector<Vector2> rearrangePoints(Vector<Vector2> pointList) {
        Vector<Vector2> newPointList = new Vector<>();
        int PointIndex;
        double maxX = -1000000000;
        int indexMax = 0;

        for (PointIndex = 0; PointIndex < pointList.size(); PointIndex++) {
            Vector2 actualPoint = pointList.get(PointIndex);
            if (actualPoint.x > maxX) {
                maxX = actualPoint.x;
                indexMax = PointIndex;
            }
        }

        IntStream.range(indexMax, pointList.size()).mapToObj(pointList::get).forEachOrdered(newPointList::add);
        IntStream.range(0, indexMax + 1).mapToObj(pointList::get).forEachOrdered(newPointList::add);

        return newPointList;
    }

    /**
     * Returns a point list from a segment list
     *
     * @param segmentList the segment list
     * @return  the list of point computed from the segment list
     */
    private static Vector<Vector2> computePoints(Vector<Segment2D> segmentList) {
        Vector<Vector2> pointList = new Vector<>();
        for (Segment2D segment : segmentList) {
            pointList.add(new Vector2(segment.start.x, segment.start.y));
        }
        pointList.remove(0);
        return pointList;
    }

    /**
     * Searches the next segment of the given segment, in a list of segments.
     * And returns the rearranged list.
     * @see #rearrangeSegments
     * @param segment the current segment.
     * @param segmentList   the list of all segments.
     * @param newSegmentList the list segments that have already been rearranged.
     * @return the rearranged list.
     */
    private static Vector<Segment2D> searchNextSegment(Segment2D segment, Vector<Segment2D> segmentList, Vector<Segment2D> newSegmentList) {
        for (Segment2D segmentSearch : segmentList) {
            if (segmentSearch.start == segment.end) {
                newSegmentList.add(segmentSearch);
                segmentList.remove(segmentSearch);
                newSegmentList = searchNextSegment(segmentSearch, segmentList, newSegmentList);
                return newSegmentList;
            }
        }
        return newSegmentList;
    }

    //todo doc @Andre
    public static double getLocalCoordinateSystemAngle(Vector<Vector2> sectionpoints) {

        //map for more accurate result
        Vector<Vector2> mappedPoints = repopulateWithNewPoints(30, sectionpoints);

        //get an angle in degrees
        double angle = getSectionOrientation(mappedPoints);


        //check if abscissa axe of local coordinate system and and section are directed in the same direction.
        if (arePointsMostlyOrientedToTheLeft(sectionpoints, sectionpoints.firstElement())) {
            angle += 180; //rotate coordinate system
        }


        if (angle > 180) { // make sure that the angle is between -180 and 180 degrees
            angle -= 360;
        }

        return angle;
    }

    //todo doc @Andre
    public static Vector<Vector2> getSectionInLocalCoordinateSystem(Vector<Vector2> sectionPoints) {

        double angle = getLocalCoordinateSystemAngle(sectionPoints);

        return transformCoordinateSystem(sectionPoints, angle);
    }


    /**
     * Rotates a list of points by a given angle
     * @param points the points to transform
     * @param angle  the angle in degrees
     * @return the rotated list of points
     */
    private static Vector<Vector2> transformCoordinateSystem(Vector<Vector2> points, double angle) {
        angle = Math.toRadians(angle);
        Vector<Vector2> finalPoints = new Vector<>();
        finalPoints.add(new Vector2(0, 0)); // first point is always on origin
        double translatX = points.firstElement().x * Math.cos(angle) + points.firstElement().y * Math.sin(angle);
        double translatY = -points.firstElement().x * Math.sin(angle) + points.firstElement().y * Math.cos(angle);
        for (int i = 1; i < points.size(); i++) {
            double x = points.get(i).x * Math.cos(angle) + points.get(i).y * Math.sin(angle) - translatX;
            double y = -points.get(i).x * Math.sin(angle) + points.get(i).y * Math.cos(angle) + -translatY;
            finalPoints.add(new Vector2(x, y));
        }
        return finalPoints;
    }


    /**
     * Takes a list of points, and returns the part of the polygon which can be used to place a bit.
     * Section acquisition is done clockwise.
     *
     * @param polyPoints the points on which the bit will be placed
     * @param startPoint the point on which the left side of the bit will be placed. startPoint must be on the polygon.
     * @return a vector of vector2, the part of the polygon which can be used to place a bit
     */
    public static Vector<Vector2> getSectionPointsFromBound(Vector<Vector2> polyPoints, Vector2 startPoint) {

        double bitLength = CraftConfig.bitLength;

        // first we look for the segment on which the startPoint is.
        int startIndex = 0;
        for (int i = 0; i < polyPoints.size() - 1; i++) {
            Segment2D segment2D = new Segment2D(polyPoints.get(i), polyPoints.get(i + 1));
            if (GeneralTools.isPointOnSegment(startPoint, segment2D)) {
                startIndex = i + 1;
                break;
            }
        }

        // so we will get the points starting from the one situated at startIndex, and add them to sectionPoints Vector, plus the startPoint
        Vector<Vector2> sectionPoints = new Vector<>();
        sectionPoints.add(startPoint); // first we add the startPoint which is the first point of the section

        // direct distance between start point and selected point.
        double d = Vector2.dist(startPoint, polyPoints.get(startIndex));
        int iPoint = startIndex;

        // we add all the point that are at less than bitLength distance from the startPoint
        boolean revolutionCompleted = false;
        while (d < bitLength && !revolutionCompleted) {
            sectionPoints.add(polyPoints.get(iPoint));
            iPoint++;
            if (iPoint == startIndex) { // we have browsed all the points of the bound
                revolutionCompleted = true;
            }
            if (iPoint == polyPoints.size()) { // come back to index 0
                iPoint = 0;
            }
            d = Vector2.dist(startPoint, polyPoints.get(iPoint));
        }

        // this segment intersects with a circle : center -> startPoint; radius -> bitLength
        Segment2D segment = new Segment2D(polyPoints.get(iPoint - 1), polyPoints.get(iPoint));

        // find this intersection : this is the last point of the section
        sectionPoints.add(GeneralTools.circleAndSegmentIntersection(startPoint, bitLength,
                segment));

        //AI_Tool.dataPrep.pointsADessiner.addAll(sectionPoints);

        return sectionPoints;
    }

    /**
     * Returns the angle of the line that fit a list of points
     * @param points the points
     * @return an angle between -90 and 90 degrees
     */
    public static Double getSectionOrientation(Vector<Vector2> points) {
        // prepare fitting
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);//degree 1
        WeightedObservedPoints weightedObservedPoints = new WeightedObservedPoints();
        weightedObservedPoints.add(1000, points.get(0).x, points.get(0).y);

        for (int i = 1; i < points.size(); i++) {
            weightedObservedPoints.add(points.get(i).x, points.get(i).y);
        }

        // fit
        double[] coefs_inverse = fitter.fit(weightedObservedPoints.toList());
        return Math.toDegrees(Math.atan(coefs_inverse[1]));
    }

    //todo doc @Andre
    public static boolean arePointsMostlyOrientedToTheLeft(Vector<Vector2> points, Vector2 refPoint) {
        int leftPoints = 0;
        int rightPoints = 0;
        for (Vector2 point : points) {
            if (point.x < refPoint.x) {
                leftPoints++;
            } else {
                rightPoints++;
            }
        }
        return leftPoints >= rightPoints;
    }


    //todo doc @Andre
    public static Vector<Vector2> getInputPointsForDL(Vector<Vector2> sectionPoints) {
        int nbPoints = 30;
        return repopulateWithNewPoints(nbPoints, sectionPoints);
    }

    /**
     * Repopulate a section a points with new points. Doesn't keep old points.
     *
     * @param nbNewPoints the number of points to add between two points.
     * @param points      the section of points to repopulate
     * @return the section repopulated with new points.
     */
    private static Vector<Vector2> repopulateWithNewPoints(int nbNewPoints, Vector<Vector2> points) {

        Vector<Vector2> newPoints = new Vector<>();
        Vector<Double> segmentLength = new Vector<>();
        // faire un tableau de longueurs des segments initiaux
        for (int i = 0; i < points.size() - 1; i++) {
            double size = Math.sqrt(Math.pow(points.get(i).x - points.get(i + 1).x, 2)
                    + Math.pow(points.get(i).y - points.get(i + 1).y, 2));
            segmentLength.add(size);
        }
        double spacing = segmentLength.stream().mapToDouble(Double::valueOf).sum() / (nbNewPoints - 1);

        double baseSegmentSum = 0;
        double newSegmentSum = 0;
        int basePointsIndex = 0;

        // --- Placer chaque nouveau point l'un après l'autre ---

        for (int i = 0; i < nbNewPoints; i++) { // Placer un nouveau point

            double absNewPoint;
            double ordNewPoint;

            // --- selection du segment initial sur lequel on va placer le nouveau point---
            //System.out.println("baseSegmentSum + segmentLength = " + baseSegmentSum + segmentLength[basePointsIndex] + " newSegmentSum = " + newSegmentSum);
            while (basePointsIndex < points.size() - 2 && baseSegmentSum + segmentLength.get(basePointsIndex) <= newSegmentSum) {
                baseSegmentSum += segmentLength.get(basePointsIndex);
                basePointsIndex += 1;
            }

            //Calculer l'angle du segment par rapport à l'horizontale
            double segmentAngle;
            //FIXME @Andre la condition est en double !!
            if (points.get(basePointsIndex).x == points.get(basePointsIndex + 1).x
                    && points.get(basePointsIndex).y <= points.get(basePointsIndex + 1).y) { // alors segment vertical vers le haut
                segmentAngle = Math.PI / 2;
            } else if (points.get(basePointsIndex).x == points.get(basePointsIndex + 1).x
                    && points.get(basePointsIndex).y <= points.get(basePointsIndex + 1).y) { // alors segment vertical vers le haut)
                segmentAngle = -Math.PI / 2;
            } else {
                segmentAngle = Math.atan((points.get(basePointsIndex + 1).y - points.get(basePointsIndex).y)
                        / (points.get(basePointsIndex + 1).x - points.get(basePointsIndex).x)); // Coef directeur du segment
            }

            int sign = 1;
            if (points.get(basePointsIndex + 1).x < points.get(basePointsIndex).x) {
                sign = -1;
            }

            absNewPoint = points.get(basePointsIndex).x + sign * (newSegmentSum - baseSegmentSum) * Math.cos(segmentAngle);
            ordNewPoint = points.get(basePointsIndex).y + sign * (newSegmentSum - baseSegmentSum) * Math.sin(segmentAngle);

            newPoints.add(new Vector2(absNewPoint, ordNewPoint));

            newSegmentSum += spacing;

        }
        return newPoints;
    }


    //todo doc @Andre et dire comment les suivants pourraient l'utiliser
    @SuppressWarnings({"unused", "rawtypes"})
    private static Vector<Vector> getInputSlopesForDL(Vector<Vector2> sectionPoints) {
        Curve inputCurve = new Curve("input curve");
        inputCurve.generateCurve(sectionPoints);
        Curve[] splitCurve = inputCurve.splitCurveInTwo();
        Curve xCurve = splitCurve[0];
        Curve yCurve = splitCurve[1];

        // prepare fitting
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(4);//degree
        WeightedObservedPoints weightedObservedPointsX = new WeightedObservedPoints();
        WeightedObservedPoints weightedObservedPointsY = new WeightedObservedPoints();
        for (int i = 0; i < inputCurve.getN_points(); i++) {
            weightedObservedPointsX.add(xCurve.getPoints().get(i).x, xCurve.getPoints().get(i).y);
            weightedObservedPointsY.add(yCurve.getPoints().get(i).x, yCurve.getPoints().get(i).y);
        }
        // fit
        double[] coefficients_inverseX = fitter.fit(weightedObservedPointsX.toList());
        double[] coefficients_inverseY = fitter.fit(weightedObservedPointsY.toList());
        // invert coefficients
        Vector<Double> coefsX = new Vector<>();
        Vector<Double> coefsY = new Vector<>();
        for (int i = 0; i < coefficients_inverseX.length; i++) {
            coefsX.add(coefficients_inverseX[coefficients_inverseX.length - i - 1]);
            coefsY.add(coefficients_inverseY[coefficients_inverseX.length - i - 1]);
        }
        // return result
        Vector<Vector> coefficients = new Vector<>();
        coefficients.add(coefsX);
        coefficients.add(coefsY);
        return coefficients;
    }

    /**
     * Return the next Bit2D start point.
     * It is the intersection between the slice and the end side of the Bit2D.
     *
     * @param bit         the current Bit2D (the last placed Bit2D by AI).
     * @param boundPoints the points of the bounds on which stands the bit.
     * @return the next bit start point. Returns <code>null</code> if none was found.
     */
    public static Vector2 getNextBitStartPoint(Bit2D bit, Vector<Vector2> boundPoints) throws Exception {

        Vector2 nextBitStartPoint = getBitAndContourSecondIntersectionPoint(bit, boundPoints);

        if (nextBitStartPoint != null) {
            return nextBitStartPoint;
        } else {
            throw new Exception("The bit start point has not been found.");
        }
    }


    /**
     * Returns points all points associated with a Bit2D.
     * Points associated are the points of the Slice from the startPoint of the Bit2D,
     * till the distance with the point become greater than the lengh of a Bit2D.
     *
     * @param bit2D The Bit2D we want to get the points associated with.
     * @return the associated points.
     */
    public static Vector<Vector2> getCurrentLayerBitAssociatedPoints(Bit2D bit2D) throws Exception {

        //First we get all the points of the Slice. getContours returns the points already rearranged.
        Vector<Vector<Vector2>> boundsList = getBoundsAndRearrange(AI_Tool.getMeshController().getCurrentLayer().getHorizontalSection());

        // finds the startPoint (if exists) and the bound related to this startPoint
        int iContour = 0;
        Vector2 startPoint = null;
        boolean boundfound = false;
        while (iContour < boundsList.size() && !boundfound) {
            startPoint = getBitAndContourFirstIntersectionPoint(bit2D, boundsList.get(iContour));
            if (startPoint != null)
                boundfound = true;
            iContour++;
        }

        // finds the points associated whith the bit, using the startPoint and the bound previously found
        if (startPoint != null)
            return getSectionPointsFromBound(boundsList.get(iContour - 1), startPoint);
        else
            throw new Exception("The bit start point has not been found.");
    }

    /**
     * returns the first intersection point between the bound and bit's edges
     *
     * @param bit         a bit
     * @param boundPoints the points of the bound
     * @return the first intersection point between the bound and bit's edges
     */
    public static Vector2 getBitAndContourFirstIntersectionPoint(Bit2D bit, Vector<Vector2> boundPoints) {
        // get sides of the bit as Segment2Ds (will be used later)
        Vector<Segment2D> bitSides = GeneralTools.getBitSidesSegments(bit);

        // first we fill an vector of segments with the points of the bound :
        Vector<Segment2D> boundSegments = new Vector<>();
        for (int i = 0; i < boundPoints.size() - 1; i++) {
            boundSegments.add(
                    new Segment2D(boundPoints.get(i), boundPoints.get(i + 1)));
        }

        // We will have to scan each segment of the bound, to check if an edge of the bit intersects with it.
        // But we have to start scanning by a segment whose its start is not under the bit, otherwise the intersection
        // point found won't be the good one.
        // So first we have to find a segment whose its start is not under the bit.

        Polygon rectangle = new Polygon();
        GeneralTools.getBitSidesSegments(bit).forEach(rectangle::addEnd);
        Area bitRectangleArea = new Area(rectangle.toPath2D());

        int startSegIndex = 0;

        while (bitRectangleArea.contains(boundSegments.get(startSegIndex).start.x,
                boundSegments.get(startSegIndex).start.y)) {
            startSegIndex++;
        }


        // finally we can scan the bound, starting with segment at index startSegIndex.
        boolean scanCompleted = false;
        int iSeg = startSegIndex;

        while (!scanCompleted) { //look for an intersecion


            // sometimes there will be more than 1 bit's edges intersecting a segment. We have to make sure that
            // we return the first of theses intersections. So we will store all intersection points and return
            // the one which its distance with segment's start is the lowest.
            Vector<Vector2> intersectionPoints = new Vector<>();


            //fill intersectionPoints Vector<> by checking intersections with all bit's sides
            for (Segment2D bitSide : bitSides) {
                Vector2 intersectionPoint = GeneralTools.getIntersectionPoint(bitSide, boundSegments.get(iSeg));
                if (intersectionPoint != null) { // then we store this intersection
                    intersectionPoints.add(intersectionPoint);
                }
            }

            // if we have some intersections we have to return the first one (as explained above)
            if (!intersectionPoints.isEmpty()) {
                double maxDist2 = 1000000; //todo @Andre remplacer par Double.positiveinfinity et tester
                Vector2 firstIntersectionPoint = null; // can't be null
                for (Vector2 intersectPoint : intersectionPoints) {

                    double dist2 = Vector2.dist2(boundSegments.get(iSeg).start, intersectPoint);
                    if (dist2 < maxDist2) {
                        maxDist2 = dist2;
                        firstIntersectionPoint = intersectPoint;
                    }

                }
                return firstIntersectionPoint;
            }


            // increment
            iSeg++;
            if (iSeg == boundSegments.size()) {
                iSeg = 0;
            }

            // check if scan completed = we reached the segment at index startSegIndex again
            if (iSeg == startSegIndex) {
                scanCompleted = true;
            }
        }

        return null;
    }

    //todo doc @Andre
    public static Vector2 getBitAndContourSecondIntersectionPoint(Bit2D bit, Vector<Vector2> boundPoints) {

        Vector<Segment2D> boundSegments = GeneralTools.getSegment2DS(boundPoints);
        Vector<Segment2D> bitSegments = GeneralTools.getBitSidesSegments(bit);

        Vector<Vector2> intersectionPoints = new Vector<>();

        for (Segment2D boundSegment : boundSegments) {

            Vector<Vector2> intersectionsWithSegment = new Vector<>();

            for (Segment2D bitSegment : bitSegments) {

                if (GeneralTools.doSegmentsIntersect(boundSegment, bitSegment)) {
                    Vector2 inter = GeneralTools.getIntersectionPoint(bitSegment, boundSegment);
                    intersectionsWithSegment.add(inter);
                }
            }


            while (!intersectionsWithSegment.isEmpty()) {

                double distMin = Double.POSITIVE_INFINITY;
                Vector2 firstPoint = null;
                for (Vector2 inter : intersectionsWithSegment) {
                    if (Vector2.dist2(inter, boundSegment.start) < distMin) {
                        firstPoint = inter;
                        distMin = Vector2.dist2(inter, boundSegment.start);
                    }
                }
                intersectionPoints.add(firstPoint);
                intersectionsWithSegment.remove(firstPoint);
            }

        }
        return intersectionPoints.get(1);
    }
}