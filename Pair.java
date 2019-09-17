public class Pair<K, V>{

	private K firstValue;
	private V secondValue;
	
	public Pair(K first, V second){
		firstValue = first;
		secondValue = second;
	}

	public K getFirstVal(){
		return firstValue;
	}

	public V getSecondVal(){
		return secondValue;
	}

	public void setFirstVal(K value){
		firstValue = value;
	}

	public void setSecondVal(V value){
		secondValue = value;
	}

	public String toString(){
		return String.format("%s\t%s", firstValue, secondValue);
	}
}
