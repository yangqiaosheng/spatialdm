package spade.lib.util;

public class FloatArray {
  protected float numbers[]=null;
  protected int capacity=0, incr=10, nelements=0;

  public FloatArray(){ initialize(10,10);}
  public FloatArray(int initialCapacity, int increment) {
    initialize(initialCapacity,increment);
  }
  public void initialize(int initialCapacity, int increment) {
    capacity=initialCapacity; incr=increment; nelements=0;
    if (capacity<5) capacity=5;
    if (incr<1) incr=1;
    numbers=new float[capacity];
  }
  public int size(){ return nelements; }
  protected void increaseCapacity(){
    capacity+=incr;
    float ext[]=new float[capacity];
    for (int i=0; i<nelements; i++) ext[i]=numbers[i];
    numbers=ext;
  }
  public void addElement(float element) {
    if (nelements+1>capacity) increaseCapacity();
    numbers[nelements++]=element;
  }
  public void addElementSorted(float element) {
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
  public void insertElementAt(float element, int idx){
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
  public float elementAt(int idx){
    if (idx<0 || idx>=nelements) return Float.NaN;
    return numbers[idx];
  }
  public int indexOf(float number){
    for (int i=0; i<nelements; i++)
      if (number==numbers[i]) return i;
    return -1;
  }
  public void setElementAt(float val, int idx){
    if (idx<0 || idx>=nelements) return;
    numbers[idx]=val;
  }
  public float[] getArray() { return numbers; }

  public float[] getTrimmedArray() {
    if (nelements<1) return null;
    if (nelements==numbers.length) return numbers;
    float res[]=new float[nelements];
    for (int i=0; i<nelements; i++)
      res[i]=numbers[i];
    return res;
  }
}