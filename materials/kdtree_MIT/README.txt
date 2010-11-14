This directory contains the sources for a Java program that reads maps in the form presented for southern Iran for the HPKB BACP.  It uses the internal representation of the map to answer the following question:

Given a point expressed as a latitude and longitude, what is the nearest road link to it, and what is its distance from that road link?

The answer is computed reasonably efficiently by organizing the nodes into a KD-tree, which can be searched in time logarithmic in the number of nodes.

The source files are in two forms in the directory:

1.  Mapp.zip contains a zipped set of files as developed in Symantec Visual Cafe under Windows.  This contains a top-level Frame1.java file that defines a rudimentary user interface, allowing one to specify the names of the node and link files, to get those files read in, and to query individual latitude/longitude points to see which road links are closest, and how close.  Road segments are identified by the numeric ids of the nodes that start and end them, and distances are expressed in degrees (with no correction for the fact that degrees of longitude are shorter than degrees of latitude).

2.  The source files (*.java) are also present in ASCII form.  For those implementing larger projects, these files may be used to accomplish the same tasks that are done by the interface described above.  Frame1.java is included to show how one may call them.

The following data structures are defined:

Cons	a simple implementation of Lisp-like lists
Pt	geometric points
Seg	line segments
Region	rectangular regions of the plane, bounded below and above in x and y
MapNode	ako Pt, represents nodes
MapLink	ako Seg, represents links
Map	map, including auxiliary data and structures

Other classes, namely ListEnumerator, ListSorter, ObjectComparator, and PtComparatorY, are additional Java cruft needed to make the above work.  Note that this source refers to components of the JDK 1.2 Collections classes, especially anticipating the general mechanism for Comparators.  These can be used in JDK 1.1 (as here), but comparison of Numbers had to be special-cased.

Sorry, but all the details are "documented in the source."  I include below a note on the geometric calculations, for those interested.

Please let me know of interesting bugs by email to psz@mit.edu.  Almost nothing fails gracefully; e.g., trying to read an incorrectly formatted node or link file will surely blow up.  I don't consider that interesting.

--Peter Szolovits
  MIT Lab for Computer Science
  May 19, 1998

__________________________________
To: Adam Pease <apease@teknowledge.com>
From: Peter Szolovits <psz@mit.edu>
Subject: Geometry
Cc: Marie desJardins <marie@erg.sri.com>, jli@teknowledge.com, gurer@erg.sri.com, agu@erg.sri.com, moises@erg.sri.com, marie@erg.sri.com, fritz@cyc.com, belasco@cyc.com, ccondora@argon.Teknowledge.COM, ben@cyc.com, kat@cyc.com, doyle@medg.lcs.mit.edu
Bcc: 
X-Attachments: 
In-Reply-To: <3.0.32.19980508105846.009062c0@mailhost.teknowledge.com>
References: 

Adam,
	"Pete's stuff" is really useful if you are interested in finding out the distance between a point and the closest of a large number of line segments.  The current definition of the FLOT contains only a handful of line segments, so the issues of scale that motivated me to write code for the map are not applicable.  I am happy to share my Lisp code, which does two things of potential use.  The first is how to compute the distance from a point to a single line segment.  This is not hard, and it may be easier for someone to reimplement at home than to figure out how to mix and match implementation languages.  The second is how to find, efficiently, the closest line segment to a point, given that there are (in this dataset) about 36000 segments.  This is more work to implement well.

	In my code, distance between a point and a line segment is defined as follows:

If the perpendicular dropped from the point to the (infinite) line on which the line segment lies actually intercepts the line segment, then distance is the length of that perpendicular.  If it does not intercept it (i.e., if the point lies "beyond the ends" of the segment), then distance is simply the distance to the closer endpoint.

If the segment endpoints are s and e, and the point is p, then the test for the perpendicular intercepting the segment is equivalent to insisting that the two dot products {s-e}.{s-p} and {e-s}.{e-p} are both non-negative.  Perpendicular distance from the point to the segment is computed by first computing the area of the triangle the three points form, then dividing by the length of the segment.  Distances are done just by the Pythagorean theorem.  Twice the area of the triangle formed by three points is the determinant of the following matrix:

sx sy 1
ex ey 1
px py 1

	For the second problem, I make the assumption that if a line segment is close to a point, then at least one of its endpoints will be close too.  (This is clearly not always true--consider a very long line segment that happens to come very close to a point--but it must be true for datasets like the Iran road data, in which there are no long segments.)  I then organize all the nodes (line segment endpoints) into a kd-tree, which allows me to search efficiently (time ~ log(number of nodes)) for the set of points within a rectangular region around a point.  I start off looking in a fairly small region (currently, a distance of twice the average map segment length from the point).  If I find a set of points, then I compute the distance (above) to each segment that those points end, and choose the closest one.  If I don't find any, I double the search distance and try again.  For our dataset, this works fine, and is essentially infallible for points that are actually near roads.
	Let me know what you would like.  --Pete


