package spade.vis.database;

public class AttributeTypes {
  /**
  * Types of attributes
  */
  public static final char types[]= {'I','R','C','L','T','G','D','M'};
  /**
  * Constants for more convenient addressing of attribute types
  */
  public static final char integer=types[0], real=types[1], character=types[2],
                           logical=types[3], time=types[4], geometry=types[5],
                           dynamic=types[6], moving=types[7];
  /**
  * Possible derivation methods for attributes - results of analysis and
  * computation. 0 ("original") means that the attribute was initially present in
  * the database. "compute" means arbitrary computation.
  */
  public static final int original=0,
    compute=1, sum=2, difference=3,
    ratio=4, ratio_in_sum=5, ratio_in_const=6,
    percent=7, percent_in_sum=8, percent_in_const=9,
    change_ratio=10, change_percent=11,
    average=12, variance=13, value_count=14,
    classify=15, classify_order=16, classify_dominant=17, classify_similar=18,
    evaluate_score=19, evaluate_rank=20, order=21,
    distance=22, dominance_degree=23,
    percentile=24;
  /**
  * Checks whether the given symbol represents a valid attribute type.
  */
  public static boolean isValidType(char t) {
    for (int i=0; i<types.length; i++)
      if (t==types[i]) return true;
    return false;
  }
  /**
  * Returns true if the character represents integer or real attribute type
  */
  public static boolean isNumericType(char t){
    return t==real || t==integer;
  }
  /**
  * hdz, 2004.03.25
  * Returns true if the character represents integer
  */
  public static boolean isIntegerType(char t){
    return t==integer;
  }
  /**
  * Returns true if the character represents character or logical attribute type
  */
  public static boolean isNominalType(char t){
    return t==character || t==logical;
  }

  /**
  * Returns true if the character represents temporal type
  */
  public static boolean isTemporal(char t){
    return t==time;
  }
}
