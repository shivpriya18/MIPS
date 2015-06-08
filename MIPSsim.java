
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MIPSsim {
	
	private boolean[] isWriting = new boolean[32];
	private boolean[] isReading = new boolean[32];
	private boolean[] isReadAfterWrite = new boolean[32];
	private boolean[] isWriteAfterWrite = new boolean[32];
	private boolean[] isWriteAfterRead = new boolean[32];
	private int[] countReads = new int[32];
	private int[] countWrites = new int[32];
	
	private int firstFetch = 0;
	private int stallFetchUnit = 0;
	private int stallPipeline = 0;
	private int issueCount = 0;
	private boolean haltStores = false;
	private boolean haltLoads = false;
	
   
    private  int address = 128;
    private int DestinationRegister = 0;
    private int DestinationRegisterPrev = 0;
    private int WBResult = 0;
    private int WBResultPrev = 0;
    private int WBLoad = 0;
    private int WBLoadPrev = 0; 
    
    private int computedMemory = 0;
    private int computedMemoryPrev = 0;
    
    private int LoadRegister = 0;
    private int LoadRegisterPrev = 0;
    
	
	private BlockingQueue<String> preIssueQueue = new ArrayBlockingQueue<String>(4);
	private BlockingQueue<String> preIssueQueuePrev = new ArrayBlockingQueue<String>(4,true,preIssueQueue);
	private BlockingQueue<String> waitingInstruction = new ArrayBlockingQueue<String>(1);
	private BlockingQueue<String> executedInstruction = new ArrayBlockingQueue<String>(1);
	private BlockingQueue<String> preAluBuffer = new ArrayBlockingQueue<String>(2);
	private BlockingQueue<String> preAluBufferPrev = new ArrayBlockingQueue<String>(2,true,preAluBuffer);
	private BlockingQueue<String> postAluBuffer = new ArrayBlockingQueue<String>(1);
	private BlockingQueue<String> postAluBufferPrev = new ArrayBlockingQueue<String>(1, true, postAluBuffer);
	private BlockingQueue<String> preMemBuffer = new ArrayBlockingQueue<String>(1);
	private BlockingQueue<String> preMemBufferPrev = new ArrayBlockingQueue<String>(1,true , preMemBuffer);
	private BlockingQueue<String> postMemBuffer = new ArrayBlockingQueue<String>(1);
	private BlockingQueue<String> postMemBufferPrev = new ArrayBlockingQueue<String>(1,true,postMemBuffer);
	

	
	

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		
	HashMap<String, String> category1 = new HashMap<String, String>();
		
		category1.put("000","J");
		category1.put("010","BEQ");
		category1.put("100","BGTZ");
		category1.put("101","BREAK");
		category1.put("110","SW");
		category1.put("111","LW");
		
		
		HashMap<String, String> category2 = new HashMap<String, String>();
		
		
		category2.put("000","ADD");
		category2.put("001","SUB");
		category2.put("010","MUL");
		category2.put("011","AND");
		category2.put("100","OR");
		category2.put("101","XOR");
		category2.put("110","NOR");
		
		
		HashMap<String, String> category3 = new HashMap<String, String>();
		
		category3.put("000","ADDI");
		category3.put("001","ANDI");
		category3.put("010","ORI");
		category3.put("011","XORI");
		
		
		
       
		
		
		HashMap<Integer, String> instruction = new HashMap<Integer, String>();
		HashMap<Integer,Integer> data = new HashMap<Integer,Integer>();
		int []Registers = new int[32];
		Arrays.fill(Registers,0);
	
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		String line = null;
		String category = null;
		
		
		int add = 128;
		MIPSsim m = new MIPSsim();

	String check = null;
	boolean test = false;
	int dataaddress = 0;
	while((line=br.readLine())!=null)
	{
		if(test == true)
		{
			if(line.substring(0,1).equals("1"))
			{     
				int x = (short)Integer.parseInt(line.substring(1),2);
				data.put(add,x);
			}
			else
			{
			data.put(add,Integer.parseInt(line.substring(1),2));
			}
		}
		else if(test == false)
		{
	   	category = line.substring(0, 3);
	   	if(category.equals("000"))
	   	{
	   		check = line.substring(3,6);
	   		
	   		if(category1.get(check).equals("BREAK"))
	   			{
	   			test=true;
	   			dataaddress = add+4;
	   			}
	   		instruction.put(add, line);
	   	
	   	}
	   	else if(category.equals("110"))
	   	{
	   		instruction.put(add,line);
	   	}
	   	else if(category.equals("111"))
	   	{
	   		instruction.put(add, line);
	   		
	
	   	}
		}
	   	add = add+4;
	}

		
		
		
		
		
		File file = new File("simulation.txt");
		
		
		if(!file.exists())
		{
			file.createNewFile();
		}
		
		FileWriter f = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(f);
		
		int cycle = 1;
		
		
		while(m.stallPipeline != 1)
		{
			
			m.fetch(instruction, Registers);
			m.issue();
		    m.Alu(Registers);
		    m.Mem(data, Registers);
		    m.WriteBack(Registers);
		    
		    
		    m.preIssueTick();
		    m.preAluBufferTick();
		    m.postAluBufferTick();
		    m.preMemBufferTick();
		    m.postMemBufferTick();
		    m.DestinationRegisterTick();
		    m.computedMemoryTick();
		    m.LoadRegisterTick();
		    m.WBResultTick();
		    m.WBLoadTick();
		    
		    
		    
		    
		    
			bw.write("--------------------");
			bw.newLine();
			bw.write("Cycle:"+cycle);
			cycle = cycle + 1;
			bw.newLine();
			bw.newLine();
			bw.write("IF Unit:");
			bw.newLine();
		    bw.write("\t"+"Waiting Instruction:"+ m.Result(m.waitingInstruction.peek(), category1, category2, category3));
		    //System.out.println("waiting:"+ m.Result(m.waitingInstruction.peek(), category1, category2, category3));
		    bw.newLine();
		    bw.write("\t"+"Executed Instruction:"+ m.Result(m.executedInstruction.peek(), category1, category2, category3));
		    //System.out.println("Executed:"+m.Result(m.executedInstruction.peek(), category1, category2, category3));
		  
		    bw.newLine();
		    bw.write("Pre-Issue Queue:");
		    bw.newLine();
		    
		    String issue[] = m.preIssueQueuePrev.toArray(new String[4]);
		    bw.write("\t"+"Entry 0:"+m.Result(issue[0], category1, category2, category3));
		    //System.out.println("Issue");
		    //System.out.println("Entry 0:"+m.Result(issue[0], category1, category2, category3));
		    //System.out.println("Entry 1:"+m.Result(issue[1], category1, category2, category3));
		    //System.out.println("Entry 2:"+m.Result(issue[2], category1, category2, category3));
		    //System.out.println("Entry 3:"+m.Result(issue[3], category1, category2, category3));
		    bw.newLine();
		    bw.write("\t"+"Entry 1:"+m.Result(issue[1], category1, category2, category3));
		    bw.newLine();
		    bw.write("\t"+"Entry 2:"+m.Result(issue[2], category1, category2, category3));
		    bw.newLine();
		    bw.write("\t"+"Entry 3:"+m.Result(issue[3], category1, category2, category3));
		    bw.newLine();
		    bw.write("Pre-ALU Queue:");
		    bw.newLine();
		    String Alu[] = m.preAluBufferPrev.toArray(new String[2]);
		   
		    //System.out.println("Pre ALu");
		    bw.write("\t"+"Entry 0:"+m.Result(Alu[0], category1, category2, category3));
		    //System.out.println("Entry 0:"+m.Result(Alu[0], category1, category2, category3));
		    //System.out.println("Entry 1:"+m.Result(Alu[1], category1, category2, category3));
		    bw.newLine();
		    bw.write("\t"+"Entry 1:"+m.Result(Alu[1], category1, category2, category3));
		    bw.newLine();
		    //System.out.println("Pre MEM");
		    bw.write("Pre-MEM Queue:"+ m.Result(m.preMemBufferPrev.peek(), category1, category2, category3));
		    //System.out.println("Entry 0:"+m.Result(m.preMemBufferPrev.peek(), category1, category2, category3));
		    bw.newLine();
		    bw.write("Post-MEM Queue:"+m.Result(m.postMemBufferPrev.peek(), category1, category2, category3));
		    //System.out.println("post mem");
		    //System.out.println("Entry 0:"+m.Result(m.postMemBufferPrev.peek(), category1, category2, category3));
		    bw.newLine();
		    //System.out.println("post alu");
		    bw.write("Post-ALU Queue:"+m.Result(m.postAluBufferPrev.peek(), category1, category2, category3));
		    //System.out.println("Entry 0:"+m.Result(m.postAluBufferPrev.peek(), category1, category2, category3));
		    bw.newLine();
		    bw.newLine();
		    bw.write("Registers");
		    bw.newLine();
		    bw.write("R00:"+"\t"+Registers[0]+"\t"+Registers[1]+"\t"+Registers[2]+"\t"+Registers[3]+"\t"+Registers[4]+"\t"+Registers[5]+"\t"+Registers[6]+"\t"+Registers[7]);
			bw.newLine();
			bw.write("R08:"+"\t"+Registers[8]+"\t"+Registers[9]+"\t"+Registers[10]+"\t"+Registers[11]+"\t"+Registers[12]+"\t"+Registers[13]+"\t"+Registers[14]+"\t"+Registers[15]);
			bw.newLine();
			bw.write("R16:"+"\t"+Registers[16]+"\t"+Registers[17]+"\t"+Registers[18]+"\t"+Registers[19]+"\t"+Registers[20]+"\t"+Registers[21]+"\t"+Registers[22]+"\t"+Registers[23]);
			bw.newLine();
			bw.write("R24:"+"\t"+Registers[24]+"\t"+Registers[25]+"\t"+Registers[26]+"\t"+Registers[27]+"\t"+Registers[28]+"\t"+Registers[29]+"\t"+Registers[30]+"\t"+Registers[31]);
			bw.newLine();
			bw.newLine();
			bw.write("Data");
			bw.newLine();
			int count =1;
			int daddress =  dataaddress;
			 bw.write(daddress+":");
  		     bw.write("\t"+data.get(daddress));
  		     count = count+1;
  		     daddress = daddress+4;
		      while(data.containsKey(daddress))
		      {
		    	   if(count == 9)
		    	  {
		    		  bw.newLine();
		    		  bw.write(daddress+":");
		    		  bw.write("\t" +data.get(daddress));
		    		  daddress = daddress+4;
		    		  count = 1;
		    		  
		    	   }
		    	  else
		    	  {
		    		  bw.write("\t"+data.get(daddress)); 
		    		  count = count+1;
		    		  daddress = daddress+4;
		    	  }
		      }
		      bw.newLine();
		    
}
		
		bw.close();
		
}
	
	public void increment()
	{
		address = address + 4;
	}
	
	public void resetProgramCounter()
	{
		address = 128;
	}
	
	
	private void fetch( HashMap<Integer, String> instruction, int []Registers)
	{
		String firstInstruction = null;
		String branch = null;
		int instructionCount = 1;
		
		if(!executedInstruction.isEmpty() && stallFetchUnit == 1)
		{
			stallFetchUnit =0;
			executedInstruction.clear();
		}
		
		if(stallFetchUnit == 1)
		{
			branch = waitingInstruction.peek();
			if(branch.substring(3,6).equals("010"))
			{
				if(isWriting[Integer.parseInt(branch.substring(6,11),2)] || isWriting[Integer.parseInt(branch.substring(11,16),2)])
				{
					waitingInstruction.clear();
					waitingInstruction.add(branch);
					stallFetchUnit = 1;
				}
				else 
				{	
					executedInstruction.add(branch);
					computeBranchAddress(branch , Registers , branch.substring(3,6));
					waitingInstruction.clear();
					
				}
			}
			
			if(branch.substring(3,6).equals("100"))
			{
				if(isWriting[Integer.parseInt(branch.substring(6,11),2)])
				{
					waitingInstruction.clear();
					waitingInstruction.add(branch);
					stallFetchUnit = 1;
				}
				else
				{
					executedInstruction.add(branch);
					computeBranchAddress(branch , Registers , branch.substring(3,6));
					waitingInstruction.clear();
				}
			}
		}
		

		
		while(preIssueQueue.remainingCapacity() != 0 && firstFetch == 0 && instructionCount <=2 && stallFetchUnit == 0)
		{
			firstInstruction = instruction.get(address);
			
		if(firstInstruction.substring(0,3).equals("000"))
		{
			if(firstInstruction.substring(3,6).equals("110"))
			{
				
				preIssueQueue.add(firstInstruction);
			
				increment();
			}
			else if(firstInstruction.substring(3,6).equals("111"))
			{
				isWriting[Integer.parseInt(firstInstruction.substring(11,16),2)] = true;
				preIssueQueue.add(firstInstruction);
				increment();
				
			}
			else
			{
		      branchConditionCheck(firstInstruction , Registers);
		      if(instructionCount == 1){
		    	  firstFetch = 1;
		       }
		       }
			}
		
		
		if(firstInstruction.substring(0,3).equals("110"))
		{
			isWriting[Integer.parseInt(firstInstruction.substring(16,21),2)] = true;
			preIssueQueue.add(firstInstruction);
			increment();
		}
		else if(firstInstruction.substring(0,3).equals("111"))
		{
			isWriting[Integer.parseInt(firstInstruction.substring(8, 13),2)] = true;
			preIssueQueue.add(firstInstruction);
			increment();
		}
		
		instructionCount += 1;
	}

		firstFetch = 0;
		instructionCount = 1;
	}
	
	private void branchConditionCheck(String firstInstruction, int []Registers) {
		// TODO Auto-generated method stub
		
		String opcode = firstInstruction.substring(3,6);
		if(opcode.equals("000") )
		{
			executedInstruction.clear();  // remove later if needed
			executedInstruction.add(firstInstruction);
			stallFetchUnit = 1; //remove later if neeeded
			computeBranchAddress(firstInstruction , Registers , opcode);
			
		}
		
		if(opcode.equals("010"))
		{
			if(isWriting[Integer.parseInt(firstInstruction.substring(6,11),2)])
			{
				waitingInstruction.clear();
				waitingInstruction.add(firstInstruction);
				stallFetchUnit = 1;
			}
			else 
			{	
				executedInstruction.clear();
				executedInstruction.add(firstInstruction);
				computeBranchAddress(firstInstruction , Registers , opcode);
				
			}
		}
		
		if(opcode.equals("100"))
		{
			if(isWriting[Integer.parseInt(firstInstruction.substring(6,11),2)])
			{
				waitingInstruction.clear(); //	remove later if needed			
				waitingInstruction.add(firstInstruction);
				stallFetchUnit = 1;
			}
			else
			{
				executedInstruction.clear(); //remove later if needed
				executedInstruction.add(firstInstruction);
				computeBranchAddress(firstInstruction , Registers , opcode);
				
			}
		}
		
		/* test for break instruction*/
		if(opcode.equals("101"))
		{
			executedInstruction.clear();
			executedInstruction.add(firstInstruction);
			stallPipeline = 1;
		}
		
	}
	
	
	
	
	private void computeBranchAddress(String inst, int[] Registers, String opcode)
	{
		
			// TODO Auto-generated method stub
			MIPSsim m = new MIPSsim();
			int i = Integer.parseInt(opcode, 2);
			switch(i)
			{
			case 0:
			{
			     address= Integer.parseInt(inst.substring(7,32)+"00",2);
				 break;
			}
			case 2:
			{
				if(Registers[Integer.parseInt(inst.substring(6,11),2)]==Registers[Integer.parseInt(inst.substring(11,16),2)])
					{
					address = address+4+Integer.parseInt(inst.substring(16)+"00",2);
					}
				else
				{
					address = address+4;
				}
					
				break;
			}
			case 4:
			{
				if(Registers[Integer.parseInt(inst.substring(6,11),2)]>0)
				{
					address = address+4+Integer.parseInt(inst.substring(16)+"00",2);
				}
				else
				{
					address = address+4;
				}
				break;
					
			}
			case 5:
			{
			    m.increment();	
				break;
			}
			}
		
	}

	private void issue()
	{
		String instruction = null;
		Iterator<String> i = preIssueQueuePrev.iterator();
		while( issueCount <= 2 && i.hasNext() && preAluBuffer.remainingCapacity() != 0 )
		{	
			instruction = i.next();
			
			if(instruction.substring(0,3).equals("110"))
			{
				
				if(isWriting[Integer.parseInt(instruction.substring(16,21),2)]&& countWrites[Integer.parseInt(instruction.substring(16,21),2)]!=0)
				{
					isWriteAfterWrite[Integer.parseInt(instruction.substring(16,21),2)] = true;
				}
				if(isReading[Integer.parseInt(instruction.substring(16,21),2)] && countReads[Integer.parseInt(instruction.substring(16,21),2)]!=0)
				{
					isWriteAfterRead[Integer.parseInt(instruction.substring(16,21),2)] = true;
				}
				
				isWriting[Integer.parseInt(instruction.substring(16,21),2)] = true;
				countWrites[Integer.parseInt(instruction.substring(16,21),2)] += 1;
				
				if(isWriting[Integer.parseInt(instruction.substring(8,13),2)]
						&& Integer.parseInt(instruction.substring(8,13),2)!= Integer.parseInt(instruction.substring(16,21),2)
						|| countWrites[Integer.parseInt(instruction.substring(8,13),2)] > 1 )
				{
					isReadAfterWrite[Integer.parseInt(instruction.substring(8,13),2)] = true;
				}
				isReading[Integer.parseInt(instruction.substring(8,13),2)] = true;
				countReads[Integer.parseInt(instruction.substring(8,13),2)] += 1;
				
				if(isWriting[Integer.parseInt(instruction.substring(3,8),2)] 
						&& Integer.parseInt(instruction.substring(3,8),2) != Integer.parseInt(instruction.substring(16,21),2)
						|| countWrites[Integer.parseInt(instruction.substring(3,8),2)] > 1)
				{
					isReadAfterWrite[Integer.parseInt(instruction.substring(3,8),2)] = true;
				}
				isReading[Integer.parseInt(instruction.substring(3,8),2)] = true;
				countReads[Integer.parseInt(instruction.substring(3,8),2)] += 1;
				
				if(!isWriteAfterWrite[Integer.parseInt(instruction.substring(16,21),2)]
						&& !isWriteAfterRead[Integer.parseInt(instruction.substring(16,21),2)]
								&& !isReadAfterWrite[Integer.parseInt(instruction.substring(8,13),2)]
										&& !isReadAfterWrite[Integer.parseInt(instruction.substring(3,8),2)] 
										|| pipelineEmpty())
				{
					preIssueQueue.remove(instruction);
					preIssueQueuePrev.remove(instruction);
					preAluBuffer.add(instruction);
					issueCount += 1;
					
				}
			}
			
			if(instruction.substring(0,3).equals("111"))
			{
				
				if(isWriting[Integer.parseInt(instruction.substring(8,13),2)] && countWrites[Integer.parseInt(instruction.substring(8,13),2)]!=0)
				{
					isWriteAfterWrite[Integer.parseInt(instruction.substring(8,13),2)] = true;
				}
				if(isReading[Integer.parseInt(instruction.substring(8,13),2)]&& countReads[Integer.parseInt(instruction.substring(8,13),2)]!=0)
				{
					isWriteAfterRead[Integer.parseInt(instruction.substring(8,13),2)] = true;
				}
				isWriting[Integer.parseInt(instruction.substring(8,13),2)] = true;
				countWrites[Integer.parseInt(instruction.substring(8,13),2)] += 1;
				if(isWriting[Integer.parseInt(instruction.substring(3, 8),2)] 
						&& Integer.parseInt(instruction.substring(3, 8),2) != Integer.parseInt(instruction.substring(8,13),2)
						|| countWrites[Integer.parseInt(instruction.substring(3, 8),2)] > 1)
				{
					isReadAfterWrite[Integer.parseInt(instruction.substring(3, 8),2)] = true;
				}
				isReading[Integer.parseInt(instruction.substring(3, 8),2)] = true;
				countReads[Integer.parseInt(instruction.substring(3, 8),2)] += 1;
				if(!isWriteAfterWrite[Integer.parseInt(instruction.substring(8,13),2)]
						&& !isWriteAfterRead[Integer.parseInt(instruction.substring(8,13),2)]
								&& !isReadAfterWrite[Integer.parseInt(instruction.substring(3, 8),2)] 
										|| pipelineEmpty())
				{
					preIssueQueue.remove(instruction);
					preIssueQueuePrev.remove(instruction);
					preAluBuffer.add(instruction);
					issueCount += 1;
				
				}
			}
			
			if(instruction.substring(0,3).equals("000"))
			{
				if(instruction.substring(3,6).equals("110"))
				{
					
					if(isWriting[Integer.parseInt(instruction.substring(6,11),2)]&& countWrites[Integer.parseInt(instruction.substring(6,11),2)]!=0)
					{
						isReadAfterWrite[Integer.parseInt(instruction.substring(6,11),2)] = true;
					}
					isReading[Integer.parseInt(instruction.substring(6,11),2)] = true;
					countReads[Integer.parseInt(instruction.substring(6,11),2)] += 1;
					if(isWriting[Integer.parseInt(instruction.substring(11,16),2)]&& countWrites[Integer.parseInt(instruction.substring(11,16),2)]!=0)
					{
						isReadAfterWrite[Integer.parseInt(instruction.substring(11,16),2)] = true;
					}
					isReading[Integer.parseInt(instruction.substring(11,16),2)] = true;
					countReads[Integer.parseInt(instruction.substring(11,16),2)] += 1;
					if(!isReadAfterWrite[Integer.parseInt(instruction.substring(6,11),2)]
							&& !isReadAfterWrite[Integer.parseInt(instruction.substring(11,16),2)]
								&& !haltStores
								|| pipelineEmpty())
					{
						preIssueQueue.remove(instruction);
						preIssueQueuePrev.remove(instruction);
						issueCount =+1;
					    preAluBuffer.add(instruction);
						haltStores = false;
						haltLoads = false;
					}
					else
					{
						//System.out.println("halt loads and stores");
						haltStores = true;
						haltLoads = true;
					}
				}
				else if(instruction.substring(3,6).equals("111"))
				{
					
					if(isWriting[Integer.parseInt(instruction.substring(11,16),2)] && countWrites[Integer.parseInt(instruction.substring(11,16),2)]!=0)
					{
						isWriteAfterWrite[Integer.parseInt(instruction.substring(11,16),2)] = true;
					}
					if(isReading[Integer.parseInt(instruction.substring(11,16),2)]&& countReads[Integer.parseInt(instruction.substring(11,16),2)]!=0)
					{
						isWriteAfterRead[Integer.parseInt(instruction.substring(11,16),2)] = true;
					}
					isWriting[Integer.parseInt(instruction.substring(11,16),2)] = true;
					countWrites[Integer.parseInt(instruction.substring(11,16),2)] += 1;
					if(isWriting[Integer.parseInt(instruction.substring(6,11),2)] 
							&& Integer.parseInt(instruction.substring(6,11),2) != Integer.parseInt(instruction.substring(11,16),2)
							|| countWrites[Integer.parseInt(instruction.substring(6,11),2)] > 1)
					{
						isReadAfterWrite[Integer.parseInt(instruction.substring(6,11),2)] = true;
					}
					isReading[Integer.parseInt(instruction.substring(6,11),2)] = true;
					countReads[Integer.parseInt(instruction.substring(6,11),2)] += 1;
					if(!isWriteAfterWrite[Integer.parseInt(instruction.substring(11,16),2)]
							&& !isWriteAfterRead[Integer.parseInt(instruction.substring(11,16),2)]
									&& !isReadAfterWrite[Integer.parseInt(instruction.substring(6,11),2)] 
											&& !haltLoads
											|| pipelineEmpty())
					{
						preIssueQueue.remove(instruction);
						preIssueQueuePrev.remove(instruction);
						issueCount +=1;
						
						preAluBuffer.add(instruction);
					}
				}
				
				
			
			}
			
		
		}
		haltStores = false;
		haltLoads = false;
		//System.out.println("Release inst");
		issueCount = 0;
		Arrays.fill(countReads, 0);
		Arrays.fill(countWrites, 0);
		Arrays.fill(isWriteAfterRead, false);
		
	}
	
	
	private boolean pipelineEmpty()
	{
		boolean test = false;
		
		test = waitingInstruction.isEmpty() 
				&& executedInstruction.isEmpty()
				&& preAluBuffer.isEmpty()
				&& preAluBufferPrev.isEmpty()
				&& postAluBuffer.isEmpty()
				&& postAluBufferPrev.isEmpty()
				&& preMemBuffer.isEmpty()
				&& postMemBufferPrev.isEmpty()
				&& postMemBuffer.isEmpty()
				&& preMemBufferPrev.isEmpty();
		return test;
	}
	
	private void Alu(int []Registers)
	{
		String instruction = preAluBufferPrev.peek();
		String opcode = null;
		int result;
		if(instruction != null)
		{
			
			if(instruction.substring(0,3).equals("000") && preMemBuffer.remainingCapacity()!=0)
			{
				preAluBuffer.remove();
				preAluBufferPrev.remove(); 
				opcode =  instruction.substring(3,6);
				result = compute1(instruction,Registers,opcode);
				PreMem(instruction,result);
			}
			else if(instruction.substring(0,3).equals("110") && postAluBuffer.remainingCapacity()!=0)
			{
				preAluBuffer.remove();
				preAluBufferPrev.remove(); 
				opcode = instruction.substring(13,16);
				result = compute2(instruction,Registers,opcode);
				postAlu(instruction, result , Registers);
			}
			else if(instruction.substring(0,3).equals("111")&& postAluBuffer.remainingCapacity()!=0)
			{
				preAluBuffer.remove();
				preAluBufferPrev.remove();
				opcode = instruction.substring(13, 16);
				result = compute3(instruction,Registers,opcode);
				postAlu(instruction,result,Registers);
				
			}
		}
	}
	
	private void postAlu(String instruction, int result, int []Registers)
	{
	     postAluBuffer.add(instruction);
	     if(instruction.substring(0,3).equals("110"))
	     {
	    	 DestinationRegister = Integer.parseInt(instruction.substring(16,21),2);
	     }
	     else if(instruction.substring(0,3).equals("111"))
	     {
	    	 DestinationRegister = Integer.parseInt(instruction.substring(8,13),2);
	     }
	     
	     WBResult = result;
	    

	     
	}
	
	private void PreMem(String inst, int result)
	{
		preMemBuffer.add(inst);
		computedMemory = result;
		
	}
	
	private void Mem(HashMap<Integer,Integer> data, int []Registers )
	{
		String instruction = null;
		if(preMemBufferPrev.peek()!=null) // changes prev
		{
			instruction = preMemBufferPrev.remove(); //changes current
			if(instruction.substring(3,6).equals("111"))
			{
			
				WBLoad = data.get(computedMemoryPrev);    // changes prev
			
				PostMem(instruction);
			}
			else if(instruction.substring(3,6).equals("110"))
			{
			   data.put(computedMemoryPrev, Registers[Integer.parseInt(instruction.substring(11,16),2)]);  // changes prev	
			   
				   isReading[Integer.parseInt(instruction.substring(11,16),2)] = false;
				   isWriteAfterRead[Integer.parseInt(instruction.substring(11,16),2)] = false;
				   isReading[Integer.parseInt(instruction.substring(6,11),2)] = false;
				   isWriteAfterRead[Integer.parseInt(instruction.substring(6,11),2)] = false;
			        
			}
			
		}
	}

	private void PostMem(String instruction)
	{
		postMemBuffer.add(instruction);
		LoadRegister = Integer.parseInt(instruction.substring(11,16),2);
		
	}
	
	
	private void WriteBack(int []Registers)
	{
		String instruction;
		
		if(postAluBufferPrev.peek()!=null)
		{
			instruction = postAluBufferPrev.peek();
			Registers[DestinationRegisterPrev] = WBResultPrev;  
			
			if(instruction.substring(0,3).equals("110"))
			{
				
					isWriting[Integer.parseInt(instruction.substring(16,21),2)] = false;
					isWriteAfterWrite[Integer.parseInt(instruction.substring(16,21),2)] = false;
					isReadAfterWrite[Integer.parseInt(instruction.substring(16,21),2)] = false;
				    isReading[Integer.parseInt(instruction.substring(8,13),2)] = false;
					isWriteAfterRead[Integer.parseInt(instruction.substring(8,13),2)] = false;
	                isReading[Integer.parseInt(instruction.substring(3,8),2)] = false;
					isWriteAfterRead[Integer.parseInt(instruction.substring(3,8),2)] = false;
				
			}
			
			
			if(instruction.substring(0,3).equals("111"))
			{
				
					isWriting[Integer.parseInt(instruction.substring(8,13),2)] = false;
					isWriteAfterWrite[Integer.parseInt(instruction.substring(8,13),2)] = false;
					isReadAfterWrite[Integer.parseInt(instruction.substring(8,13),2)] = false;
					isReading[Integer.parseInt(instruction.substring(3, 8),2)] = false;
					isWriteAfterRead[Integer.parseInt(instruction.substring(3, 8),2)] = false;
				
				
			}
			
			
			
			
					
		}
		
		
		if(postMemBufferPrev.peek()!=null)
		{
			instruction = postMemBufferPrev.peek();
			Registers[LoadRegisterPrev] = WBLoadPrev;
			
			
				isWriting[Integer.parseInt(instruction.substring(11,16),2)] = false;
				isWriteAfterWrite[Integer.parseInt(instruction.substring(11,16),2)] = false;
				isReadAfterWrite[Integer.parseInt(instruction.substring(11,16),2)] = false;
			    isReading[Integer.parseInt(instruction.substring(6,11),2)] = false;
				isWriteAfterRead[Integer.parseInt(instruction.substring(6,11),2)] = false;
			
		}
	}
	private static int compute3(String inst, int[] registers, String opcode) {
		// TODO Auto-generated method stub
		int result = 0;
		int i = Integer.parseInt(opcode,2);
		switch(i)
		{
		case 0:
		{
			result = registers[Integer.parseInt(inst.substring(3, 8),2)]+Integer.parseInt(inst.substring(17),2);
			
			break;
		}
		case 1:
		{
			result = registers[Integer.parseInt(inst.substring(3, 8),2)]&Integer.parseInt(inst.substring(17),2);
			
			break;

		}
		case 2:
		{
		    result = registers[Integer.parseInt(inst.substring(3, 8),2)]|Integer.parseInt(inst.substring(17),2);
		
			break;

		}
		case 3:
		{
		     result = registers[Integer.parseInt(inst.substring(3, 8),2)]^Integer.parseInt(inst.substring(17),2);
            
			break;
		}
		}
		return result;
	}
	

	
	private static int compute2(String inst, int[] registers, String opcode) {
		// TODO Auto-generated method stub
		
		int result = 0;
		int i = Integer.parseInt(opcode, 2);
		switch(i)
		{
		case 0:
		{
			result = registers[Integer.parseInt(inst.substring(3,8),2)]+registers[Integer.parseInt(inst.substring(8,13),2)];
			break;
			
		}
		case 1:
		{
			result = registers[Integer.parseInt(inst.substring(3,8),2)-1]-registers[Integer.parseInt(inst.substring(8,13),2)];
			break;
		}
		case 2:
		{
			result = registers[Integer.parseInt(inst.substring(3,8),2)]*registers[Integer.parseInt(inst.substring(8,13),2)];
		
			break;
		}
		case 3:
		{
			result = registers[Integer.parseInt(inst.substring(3,8),2)]&registers[Integer.parseInt(inst.substring(8,13),2)];
			break;
		}
		case 4:
		{
			result = registers[Integer.parseInt(inst.substring(3,8),2)]|registers[Integer.parseInt(inst.substring(8,13),2)];
			break;
		}
		case 5:
		{
			result = registers[Integer.parseInt(inst.substring(3,8),2)]^registers[Integer.parseInt(inst.substring(8,13),2)];
			break;
		}
		case 6:
		{
			result = ~(registers[Integer.parseInt(inst.substring(3,8),2)]|registers[Integer.parseInt(inst.substring(8,13),2)]);
			break;

		}
		
	}
	
	return result;
	}
	private static int compute1(String inst, int[] registers, String opcode) {
		// TODO Auto-generated method stub
		
		int result = 0;
		int i = Integer.parseInt(opcode, 2);
		switch(i)
		{
		/*case "000":
		{
		     m.jumpTo(Integer.parseInt(inst.substring(7,32)+"00",2));
			 break;
		}
		case "010":
		{
			if(registers[Integer.parseInt(inst.substring(6,11),2)]==registers[Integer.parseInt(inst.substring(11,16),2)])
				 m.jumpTo(4+Integer.parseInt(inst.substring(16)+"00",2));
		
				
			break;
		}
		case "100":
		{
			if(registers[Integer.parseInt(inst.substring(6,11),2)]>0)
			{
				m.jumpTo(4+Integer.parseInt(inst.substring(16)+"00",2));
			}
			
			break;
				
		}
		case "101":
		{
		    m.increment();	
			break;
		}*/
		case 7:
		{
			 result = registers[Integer.parseInt(inst.substring(6,11),2)]+Integer.parseInt(inst.substring(16),2);
			
			
			break;
			
		}
		case 6:
		{
			
			result =Integer.parseInt(inst.substring(16),2)+registers[Integer.parseInt(inst.substring(6,11),2)];
			
			break;
		}
		}
		return result;
	
	}

	
	
	
	
	private void preIssueTick()
	{
			preIssueQueuePrev.clear();
			preIssueQueuePrev.addAll(preIssueQueue);
	}
	
	private void preAluBufferTick()
	{
		preAluBufferPrev.clear();
		preAluBufferPrev.addAll(preAluBuffer);
	}
	
	private void postAluBufferTick()
	{
		postAluBufferPrev.clear();
		postAluBufferPrev.addAll(postAluBuffer);
		postAluBuffer.clear();
	}
	
	private void preMemBufferTick()
	{
		preMemBufferPrev.clear();
		preMemBufferPrev.addAll(preMemBuffer);
		preMemBuffer.clear();
	}
	
	private void postMemBufferTick()
	{
		postMemBufferPrev.clear();
		postMemBufferPrev.addAll(postMemBuffer);
		postMemBuffer.clear();
	}
	
	private void DestinationRegisterTick()
	{
		DestinationRegisterPrev = DestinationRegister;
	}
	
    private void WBResultTick()
    {
    	WBResultPrev = WBResult;
    }
    
    private void WBLoadTick()
    {
    	WBLoadPrev = WBLoad;
    }
    
   private void  LoadRegisterTick()
   {
	   LoadRegisterPrev = LoadRegister;
   }
   
   private void computedMemoryTick()
   {
	   computedMemoryPrev = computedMemory;
   }
	private String Result(String inst, HashMap <String, String>category1,HashMap <String,String> category2,HashMap <String,String>category3)
	{
		String Answer = null;
	if(inst != null)
	{
		if(inst.substring(0, 3).equals("000"))
		{
			Answer = category1(inst, category1);
		}
		else if(inst.substring(0, 3).equals("110"))
		{
			Answer = category2(inst, category2);
		}
		else if(inst.substring(0, 3).equals("111"))
		{
			Answer = category3(inst,category3);
		}
	}
	
	else
	{
		Answer = " ";
	}
		return Answer;
	}

	private String category3(String line, HashMap<String, String> category3) {
		// TODO Auto-generated method stub
		String opcode = line.substring(13, 16);
		String Register1 = "R"+Integer.parseInt(line.substring(3, 8),2);
		String Register2 = "R"+Integer.parseInt(line.substring(8,13),2);
		int immvalue = Integer.parseInt(line.substring(16),2);
		
		String opcodevalue = category3.get(opcode);
		
		String answer = "["+opcodevalue+" "+Register2+", "+Register1+", "+"#"+immvalue+"]";
		return answer;
	}

	private String category2(String line, HashMap<String, String> category2) {
		// TODO Auto-generated method stub
		
		
		String opcode = line.substring(13,16);
		String opcodevalue = category2.get(opcode);
		String Register1 = "R"+Integer.parseInt(line.substring(3,8),2);
		String Register2 = "R"+Integer.parseInt(line.substring(8,13),2);
		String Register3 = "R"+Integer.parseInt(line.substring(16,21),2);
		String answer = "["+opcodevalue+" "+Register3+", "+Register1+", "+Register2+"]";
		
		return answer;
	}

	private String category1(String line, HashMap <String,String> category1) {
		// TODO Auto-generated method stub
	
		String opcode = line.substring(3,6);
		String opcodevalue = category1.get(opcode);
		int i = Integer.parseInt(opcode, 2);
		String answer = null;
		String Register1 = null;
		String Register2 = null;
		int immvalue;
		switch(i)
		{
		case 0: {
			answer = "["+opcodevalue+" "+"#"+Integer.parseInt(line.substring(7,32)+"00",2)+"]";
			break;
		}
		case 2: {
			Register1 = "R"+Integer.parseInt(line.substring(6,11),2);
			Register2 = "R"+Integer.parseInt(line.substring(11,16),2);
			immvalue = Integer.parseInt(line.substring(16)+"00",2);
			answer = "["+opcodevalue+" "+Register1+", "+Register2+", #"+immvalue+"]";
			break;
		}
		case 4:{
			Register1 = "R"+Integer.parseInt(line.substring(6,11),2);
			immvalue = Integer.parseInt(line.substring(16)+"00",2);
			answer="["+opcodevalue+" "+Register1+", #"+immvalue+"]";
			break;
		}
		case 5:{
			answer = "["+opcodevalue+"]";
			break;
		}
		case 6:{
			Register1 = "R"+Integer.parseInt(line.substring(6,11),2);
			Register2 = "R"+Integer.parseInt(line.substring(11,16),2);
			immvalue=Integer.parseInt(line.substring(16),2);
			answer = "["+opcodevalue+" "+Register2+", "+immvalue+"("+Register1+")"+"]";
			break;
			}
		case 7:{
			Register1 = "R"+Integer.parseInt(line.substring(6,11),2);
			Register2 = "R"+Integer.parseInt(line.substring(11,16),2);
			immvalue=Integer.parseInt(line.substring(16),2);
			answer = "["+opcodevalue+" "+Register2+", "+immvalue+"("+Register1+")"+"]";
			break;
		}
		}
		
		return answer;
	}
	
	

}
