package aim4.map.lane;

import aim4.util.GeomMath;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

/**
 * A lane class that can be represented by a directed arc segment.
 */
public class ArcSegmentLane {
    /////////////////////////////////
    // PRIVATE FIELDS
    /////////////////////////////////

    /**
     * The length of the Lane.  Won't change, so no point in recalculating every
     * time.
     */
    private double length;


    /**
     * The width of the Lane, in meters.
     */
    private double width;

    /**
     * Half the width of the Lane, in meters.
     */
    private double halfWidth;

    /**
     * The arc segment that represents the lane
     */
    private Arc2D arc;

    /**
     * A Shape describing the lane, including its width.
     */
    private Shape laneShape;

    /**
     * The arc that represents the left border of this Lane.
     */
    private Arc2D leftBorder;

    /**
     * The arc that represents the right border of this Lane.
     */
    private Arc2D rightBorder;

    /////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////

    /**
     * Constructs an arc-segment lane using an Arc.
     *
     * @param arc        the arc segment representing the center of the Lane
     * @param width      the width of the Lane, in meters
     * @param speedLimit the speed limit of the Lane, in meters per second
     */
    public ArcSegmentLane(Arc2D arc, double width, double speedLimit) {
        //super(speedLimit);

        this.arc = arc;
        this.width = width;
        this.halfWidth = width / 2;
        this.length = GeomMath.calculateArcLength(arc);
        calculateLaneBorders(arc);
        this.laneShape = calculateLaneShape(this.leftBorder, this.rightBorder);
    }

    /////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////

    // lanes as lines

    /**
     * {@inheritDoc}
     */
    //@Override
    public double getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public Point2D getStartPoint() {
        return arc.getStartPoint();
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public Point2D getEndPoint() {
        return arc.getEndPoint();
    }

    /**
     * Given a normalized distance, get the coordinates of a point on the arc
     * by moving along the arc.
     * {@inheritDoc}
     */
    //@Override
    public Point2D getPointAtNormalizedDistance(double normalizedDistance) {
        double radius = arc.getWidth() / 2.0;
        double originX = arc.getX() + radius;
        double originY = arc.getY() + radius;
        double startAngle = arc.getAngleStart();
        double theta = startAngle - (normalizedDistance * Math.toDegrees(GeomMath.PI)) / (GeomMath.PI * radius);

        double xCoordinate = originX + radius * Math.cos(Math.toRadians(theta));
        double yCoordinate = originY - radius * Math.sin(Math.toRadians(theta));
        return new Point2D.Double(xCoordinate, yCoordinate);
    }

    /**
     * {@inheritDoc}
     */

    public Shape getShape() {
        return laneShape;
    }

    /**
     * {@inheritDoc}
     */

    public Arc2D leftBorder() {
        return leftBorder;
    }

    /**
     * {@inheritDoc}
     */

    public Arc2D rightBorder() {
        return rightBorder;
    }

    /////////////////////////////////
    // PRIVATE METHODS
    /////////////////////////////////

    /**
     * Calculate lane left and right borders based on lane's width.
     */
    private void calculateLaneBorders(Arc2D arc) {
        Point2D origin = new Point2D.Double(arc.getX() + arc.getWidth() / 2, arc.getY() + arc.getWidth() / 2);
        this.leftBorder = new Arc2D.Double();
        this.rightBorder = new Arc2D.Double();
        leftBorder.setArcByCenter(origin.getX(), origin.getY(), arc.getWidth() / 2 + width / 2, arc.getAngleStart(), arc.getAngleExtent(), arc.getArcType());
        rightBorder.setArcByCenter(origin.getX(), origin.getY(), arc.getWidth() / 2 - width / 2, arc.getAngleStart(), arc.getAngleExtent(), arc.getArcType());
    }

    /**
     * Calculate lane shape, including the width of the lane.
     *
     * @return a Shape describing the lane, including its width.
     */
    private Shape calculateLaneShape(Arc2D leftBorder, Arc2D rightBorder) {
        GeneralPath result = new GeneralPath();

        //Move left of arc to start point of left border
        result.moveTo((float) (leftBorder.getStartPoint().getX()),
                (float) (leftBorder.getStartPoint().getY()));
        //Shape the left arc
        result.append(leftBorder, false);
        //Line to end of right border
        result.lineTo(rightBorder.getEndPoint().getX(), rightBorder.getEndPoint().getY());
        //Move start of right border
        result.moveTo(rightBorder.getStartPoint().getX(), rightBorder.getStartPoint().getY());
        //Shape the right arc
        result.append(rightBorder, false);
        //Move back again to start of right border
        result.moveTo(rightBorder.getStartPoint().getX(), rightBorder.getStartPoint().getY());
        //Close the path by connecting the ends
        result.lineTo(leftBorder.getStartPoint().getX(), leftBorder.getStartPoint().getY());
        //Fully close the path in case of missing precision
        result.closePath();
        return result;
    }
}
