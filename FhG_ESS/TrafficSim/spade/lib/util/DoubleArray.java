package spade.lib.util;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 07-May-2007
 * Time: 11:44:26
 */
public class DoubleArray {
  protected double numbers[]=null;
  protected int capacity=0, incr=10, nelements=0;

  public DoubleArray (){ initialize(10,10);}
  public DoubleArray (int initialCapacity, int increment) {
    initialize(initialCapacity,increment);
  }
  public void initialize(int initialCapacity, int increment) {
    capacity=initialCapacity; incr=increment; nelements=0;
    if (capacity<5) capacity=5;
    if (incr<1) incr=1;
    numbers=new double[capacity];
  }
  public int size(){ return nelements; }
  protected void increaseCapacity(){
    capacity+=incr;
    double ext[]=new double[capacity];
    for (int i=0; i<nelements; i++) ext[i]=numbers[i];
    numbers=ext;
  }
  public void addElement(double element) {
    if (nelements+1>capacity) increaseCapacity();
    numbers[nelements++]=element;
  }
  public void addElementSorted(double element) {
    if (nelements<1) addElement(element);
    else {
      //find the position
      int pos=-1;
      for (int i=0; i<nelements && pos<0; i++)
        if (element<numbers[i]) pos=i;
      if (pos>=0) insertElementAt(element,pos);
      else addElement(element);
    }
  }
  public void insertElementAt(double element, int idx){
    if (idx>nelements-1) {addElement(element); return;}
    if (nelements+1>capacity) increaseCapacity();
    for (int i=nelements-1; i>=idx; i--) numbers[i+1]=numbers[i];
    numbers[idx]=element;
    ++nelements;
  }
  public void removeElementAt(int idx){
    if (idx>=nelements) return;
    for (int i=idx; i<nelements-1; i++) numbers[i]=numbers[i+1];
    --nelements;
  }
  public void removeAllElements(){ nelements=0; }
  public double elementAt(int idx){
    if (idx<0 || idx>=nelements) return Double.NaN;
    return numbers[idx];
  }
  public int indexOf (double number){
    for (int i=0; i<nelements; i++)
      if (number==numbers[i]) return i;
    return -1;
  }
  public void setElementAt (double val, int idx){
    if (idx<0 || idx>=nelements) return;
    numbers[idx]=val;
  }
  public double[] getArray() { return numbers; }

  public double[] getTrimmedArray() {
    if (nelements<1) return null;
    if (nelements==numbers.length) return numbers;
    double res[]=new double[nelements];
    for (int i=0; i<nelements; i++)
      res[i]=numbers[i];
    return res;
  }
  public static float[] double2float (double[] values) {
    if (values==null || values.length<1) return null;
    float num[]=new float[values.length];
    for (int i=0; i<values.length; i++) num[i]=(float)values[i];
    return num;
  }
}
