import java.util.*;

public class Linker {

  //input seperated by modules
  private static ArrayList<ArrayList<String>> inputs = new ArrayList<ArrayList<String>>();
  private static ArrayList<String> output_Errors = new ArrayList<String>();

  final static String[] ERRORS = new String[]{
    "Error: This variable is multiply defined; first value used.",
    "Error: %s is not defined; zero used.",
    "Warning: %s was defined in module %d but never used.",
    "Error: The definition of %s is outside module %d; zero (relative) used.",
    "Error: Immediate address on use list; treated as External.",
    "Error: E type address not on use chain; treated as I type."
  };

  private static boolean isInt (String x){
    try{
      int val = Integer.parseInt(x);
      return true;
    }
    catch(Exception e){
      return false;
    }
  }

  private static Pair<Integer, Integer> parseAddress (String input) {
    if (isInt(input)) {
      int current = Integer.parseInt(input);
      int lastDigit = current % 10;
      int firstFour = current / 10;
      return new Pair<Integer, Integer>(firstFour, lastDigit);
    }
    else {
      return new Pair<Integer, Integer>(-1, -1);
    }
  }

  private static void PrettyPrintMap (TreeMap<Integer, Pair<Integer, String>> input_List) {
    System.out.println("Memory Map");
    for(Map.Entry<Integer,Pair<Integer, String>> entry : input_List.entrySet()) {
      int key = entry.getKey();
      Pair value = entry.getValue();
      //if there is no error message or first error message
      if (value.getSecondVal().equals("")) {
        System.out.println(key + ":" + value.getFirstVal());

      }
      //if there is an error message
      else {
        System.out.println(key + ":" + value.getFirstVal() + value.getSecondVal());
      }
    }
  }

  private static void PrettyPrintSym (TreeMap<String, Pair<Integer, String>> input_List) {
    System.out.println("Symbol Table");
    for(Map.Entry<String,Pair<Integer, String>> entry : input_List.entrySet()) {
      String key = entry.getKey();
      Pair value = entry.getValue();
      //if there is no error message
      if (value.getSecondVal().equals("")) {
        System.out.println(key + "=" + value.getFirstVal());
      }
      //if there is an error message
      else {
        System.out.println(key + "=" + value.getFirstVal() + value.getSecondVal());
        //if error message already printed, remove error message will not repeat in memory map printing
        input_List.put(key, new Pair<Integer, String>((Integer) value.getFirstVal(), ""));
      }
    }
  }

  private static TreeMap<String, Pair<Integer, String>> firstPass (ArrayList<ArrayList<String>> inputs,
    ArrayList<Integer> addressCount) {

    //var declaration
    int base_Count = 0;
    int count = 0;
    //using a treemap to sort them in order
    TreeMap<String, Pair<Integer, String>> output = new TreeMap<String, Pair<Integer, String>>();

    for (ArrayList<String> input : inputs) {
      //check if there's a defination list. If there isn't, ignore it.
      if (input.get(0).equals("0")) {
        input.remove(0);
      }
      else {
        int numModules = Integer.parseInt(input.remove(0));
        for (int n = 0; n < numModules; n++) {
          Pair<Integer, String> updatedPair;
          //check if key already exists
          if (output.containsKey(input.get(0))) {
            updatedPair = output.get(input.get(0));
            updatedPair.setSecondVal(" " + ERRORS[0]);
          }
          //check if key exists outside of module
          else if (Integer.parseInt(input.get(1)) > addressCount.get(count)-1) {
            updatedPair = new Pair<Integer,String>(base_Count, " " + String.format(ERRORS[3], input.get(0), count));
          }
          else {
            updatedPair = new Pair<Integer,String>((Integer.parseInt(input.get(1)) + base_Count), "");
          }
          output.put(input.remove(0),updatedPair);
          input.remove(0);
        }
      }
      base_Count += addressCount.get(count);
      count++;
    }
    return output;
  }

  private static void secondPass (ArrayList<ArrayList<String>> inputs,
    TreeMap<String, Pair<Integer, String>> sym,
    ArrayList<Integer> addressCount) {

    //var init
    TreeMap<Integer, Pair<Integer, String>> output = new TreeMap<Integer, Pair<Integer, String>>();
    TreeMap<String, Pair<Integer, String>> sym_Copy = new TreeMap<String, Pair<Integer, String>>();
    int addressTracker = 0;
    int modeuleAddressCount = 0;
    int count = 0;
    int useListTracking = 0;
    HashMap <String, Integer> useList;
    //make copy of symbol table
    for(Map.Entry<String, Pair<Integer, String>> entry : sym.entrySet()) {
      sym_Copy.put(entry.getKey(), entry.getValue());
    }

    for (ArrayList<String> input : inputs) {
      useList = new LinkedHashMap <String, Integer>();
      //if there is no use list
      if (input.get(0).equals("0")) {
        input.remove(0);
      }
      //if there is a use list
      else {
        int numModules = Integer.parseInt(input.remove(0));
        for (int n = 0; n < numModules; n++) {
          String currentSym = input.remove(0);
          if (sym.containsKey(currentSym)) {
            if (sym_Copy.containsKey(currentSym)) {
              sym_Copy.remove(currentSym);
            }
          }
          //if uselist item was never defined
          else {
            sym.put(currentSym, new Pair<Integer, String>(0, " " + String.format(ERRORS[1], currentSym)));
          }
          useList.put(currentSym, Integer.parseInt(input.remove(0)));
        }
      }
      int numModules = Integer.parseInt(input.remove(0));
      //make a copy of the address list, so can see what wasn't used at end
      ArrayList<String> entryCopy = new ArrayList<String>();
      for(String entry : input) {
        entryCopy.add(entry);
      }
      for (int n = 0; n < numModules; n++) {
        Pair<Integer, Integer> currentAddress = parseAddress(input.get(n));
        //if last digit is 1 or 2
        if (currentAddress.getSecondVal() == 1 || currentAddress.getSecondVal() == 2) {
          output.put(addressTracker, new Pair<Integer, String>(currentAddress.getFirstVal(), ""));
          entryCopy.remove(input.get(n));
        }
        //if last digit is 3 (relocation)
        else if (currentAddress.getSecondVal() == 3) {
          output.put(addressTracker, new Pair<Integer, String>(currentAddress.getFirstVal() + modeuleAddressCount, ""));
          entryCopy.remove(input.get(n));
        }
        addressTracker++;
      }

      //next do all externals
      for(Map.Entry<String, Integer> entry : useList.entrySet()) {
        String replace = "";
        String key = entry.getKey();
        int value = entry.getValue();
        int exteralValue = sym.get(key).getFirstVal();
        String errorMessage = sym.get(key).getSecondVal();

        //replace with external value steup
        if (exteralValue / 10 > 0) replace = "0"+Integer.toString(exteralValue);
        else replace = "00"+Integer.toString(exteralValue);
        //original 4 decimal string
        while (true) {
          String originalAddress = input.get(value).substring(0, input.get(value).length()-1);
          String originalThreeDigits = originalAddress.substring(1, originalAddress.length());
          String firstDigit = originalAddress.substring(0,1);
          String lastDigit = input.get(value).substring(input.get(value).length()-1);
          int newCurrent = Integer.parseInt(firstDigit + replace);
          //if it is an immediate address, treat as external
          if (lastDigit.equals("1")) {
            output.put(modeuleAddressCount+value, new Pair<Integer, String>(newCurrent, " " + ERRORS[4]));
            entryCopy.remove(input.get(value));
          }
          else {
            output.put(modeuleAddressCount+value, new Pair<Integer, String>(newCurrent, errorMessage));
            entryCopy.remove(input.get(value));
          }
          if (originalThreeDigits.equals("777")) {break;}
          value = Integer.parseInt(originalThreeDigits.substring(originalThreeDigits.length()-1, originalThreeDigits.length()));
        }
      }
      //check if last is 4, if it is on a useList
      for(String entry : entryCopy) {
        output.put(modeuleAddressCount+input.indexOf(entry), new Pair<Integer, String>(Integer.parseInt(entry.substring(0, entry.length()-1)), " " + ERRORS[5]));
      }

      modeuleAddressCount += addressCount.get(count);
      count++;
    }
    PrettyPrintMap(output);
    //check if there are unused uselist items
    for(Map.Entry<String, Pair<Integer, String>> entry : sym_Copy.entrySet()) {
      int module = 0;
      int memory = entry.getValue().getFirstVal();
      //get correct module number +1
      while (memory >= 0) {
        memory -= addressCount.get(module);
        module++;
      }
      output_Errors.add(String.format(ERRORS[2], entry.getKey(), module-1));
    }

  }

  public static void main(String[] args){

    //var init
    ArrayList<Integer> numAddresses = new ArrayList<Integer>();
    TreeMap<String, Pair<Integer, String>> symTable = new TreeMap<String, Pair<Integer, String>>();
    Scanner sc = new Scanner(System.in);

		try {
      //determine the number of modules
			int numModules = Integer.parseInt(sc.next());
      //based on number of modules, loop through each module and add to inputs arraylist
			for(int i = 0; i < numModules; i++){
        ArrayList<String> current_Module = new ArrayList<String>();
				while(true){
					current_Module.add(sc.next());
          //get the last value addded
					String current_Value = current_Module.get(current_Module.size() - 1);
					if(isInt(current_Value) && Integer.parseInt(current_Value) > 10000){
						int size = Integer.parseInt(current_Module.get(current_Module.size() - 2));
						for(int a = 0; a < size - 1; a++) {
              current_Module.add(sc.next());
            }
            numAddresses.add(size);
            inputs.add(current_Module);
						break;
					}
				}
			}
			sc.close();

      //firstPass
			symTable = firstPass(inputs, numAddresses);
      System.out.println("");
      //print Symbol Table
      PrettyPrintSym(symTable);
      System.out.println("");
      //SecondPass
      secondPass(inputs, symTable, numAddresses);
      System.out.println("");
      //print error messaeges
      for (String s : output_Errors) {
        System.out.println(s);
      }
      System.out.println("");
    }
    catch (Exception e){
      System.out.println("Something went wrong. Please Check");
    }
  }
}
