package io.github.jmecn.tiled.enums;

public enum ObjectType {
    /**
     * No need to explain.
     */
    RECTANGLE,
    /**
     * Used to mark an object as an ellipse. The existing x, y, width and
     * height attributes are used to determine the size of the ellipse.
     */
    ELLIPSE,
    /**
     * Used to mark an object as a point. The existing x and y attributes
     * are used to determine the position of the point.
     */
    POINT,
    /**
     * A list of x,y coordinates in pixels.
     *
     * Each polygon object is made up of a space-delimited list of x,y
     * coordinates. The origin for these coordinates is the location of the
     * parent object. By default, the first point is created as 0,0 denoting
     * that the point will originate exactly where the object is placed.
     */
    POLYGON,
    /**
     * A polyline follows the same placement definition as a polygon object.
     */
    POLYLINE,
    /**
     * Used to mark an object as a text object. Contains the actual text as character data.
     *
     * For alignment purposes, the bottom of the text is the descender height of the font,
     * and the top of the text is the ascender height of the font. For example, bottom
     * alignment of the word “cat” will leave some space below the text, even though it is
     * unused for this word with most fonts. Similarly, top alignment of the word “cat” will
     * leave some space above the “t” with most fonts, because this space is used for diacritics.
     *
     * If the text is larger than the object’s bounds, it is clipped to the bounds of the object.
     */
    TEXT,
    /**
     * An tile references to a tile with it's gid.
     */
    TILE,
    /**
     * An image
     */
    IMAGE;
}