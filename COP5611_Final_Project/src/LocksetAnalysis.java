/*
 * Timothy Deligero
 * Apurv Parekh
 * COP5611 Final Project: Data Race Detection
 */

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.lang.*;

/*
 * This class will be used to analyze a multithreading program
 * for detecting race conditions occurring during runtime.
 * This program's data race detection algorithm will be based
 * on the dynamic data race detector Eraser by checking every
 * shared memory location following the lock discipline and
 * using global variables for memory location and heap memory
 * locations as shared memory locations.
 */
public class LocksetAnalysis 
{
	// Memory location states.
	public static final Integer VIRGIN = 0;
	public static final Integer EXCLUSIVE = 1;
	public static final Integer SHARED = 2;
	public static final Integer SHARED_MODIFIED = 3;
	
	// Contains sequence of iids on method executions.
	public static HashMap<Integer, LinkedList<Integer>> threadStacks;
			
	// Represents L(t): Sequence of lock identifiers held by a current thread.
	public static HashMap<Integer, LinkedList<Integer>> threadLocks;
	
	// Represents C(v): Contains sets of candidate locks for each memory.
	public static HashMap<Integer, HashSet<Integer>> candidateMemLocks;
	
	// Contains monitoring states of each memory location.
	public static HashMap<Integer, Integer> memStates;
	
	// Contains the thread that first accesses memory.
	public static HashMap<Integer, Integer> firstMemAccess;
	
	// Contains iids of an operation that accesses memory right before the data race detection.
	public static HashMap<Integer, Integer> lastMemAccess;
	
	// Contains the data race occurrences during lockset analysis.
	public static HashMap<Integer, Integer> races;
	
	// Initialize all variables before using lockset analysis.
	public void intialize()
	{
		threadStacks = new HashMap<Integer, LinkedList<Integer>>();	
		threadLocks = new HashMap<Integer, LinkedList<Integer>>();
		candidateMemLocks = new HashMap<Integer, HashSet<Integer>>();
		memStates = new HashMap<Integer, Integer>();
		firstMemAccess = new HashMap<Integer, Integer>();
		lastMemAccess = new HashMap<Integer, Integer>();
		races = new HashMap<Integer, Integer>();
	}
	
	// Add a new thread from the test to the HashMaps stacks and locks.
	public void AddThread(Integer threadID)
	{
		// Contains a temporary LinkedList for the stacks and locks of threads.
		LinkedList<Integer> stack = new LinkedList<Integer>();
		LinkedList<Integer> lock = new LinkedList<Integer>();
		
		// Insert the thread identifier and its new empty stack and lock set.
		threadStacks.put(threadID, stack);
		threadLocks.put(threadID, lock);
	}
	
	// Adds a stack or the iid of the method enter execution to the current thread.
	public void AddStack(Integer thread, Integer stack)
	{
		threadStacks.get(thread).add(stack);
	}
	
	// Adds a lock to the current thread.
	public void AddLock(Integer thread, Integer lock)
	{
		threadLocks.get(thread).add(lock);
	}
	
	// Removes a lock from the current thread.
	public void RemoveLock(Integer thread, Integer lock)
	{
		
	}
	
	// Add a new memory location to the HashMaps candidateMemLocks and memStates.
	public void AddMemory(Integer memory)
	{
		HashSet<Integer> locks = new HashSet<Integer>();
		
		candidateMemLocks.put(memory, locks);
		memStates.put(memory, VIRGIN);
	}
	
	// Adds a candidate lock to the current memory location.
	public void AddMemLock(Integer memory, Integer lock)
	{
		candidateMemLocks.get(memory).add(lock);
	}
	
	/*
	 * Check the memory state during each read/write operation of the variable.
	 * The memory identifier, thread identifier, and code line location should
	 * be given during each read/write operation. The boolean variable read_write
	 * will represent if a read (false) or write (true) operation was done on the
	 * memory.
	 */
	public void MemState(Integer memory, Integer thread, Integer location, boolean read_write)
	{
		// Get the current state of given memory.
		Integer currentState = memStates.get(memory);
		
		// State changes when the current state of the memory is VIRGIN.
		if(currentState == VIRGIN)
		{
			if(read_write)
			{
				memStates.put(memory, EXCLUSIVE);
				firstMemAccess.put(memory, thread);
			}
		}
		
		// State changes when the current state of the memory is EXCLUSIVE.
		else if(currentState == EXCLUSIVE)
		{
			if(firstMemAccess.get(memory) != thread)
			{
				if(read_write)
				{
					memStates.put(memory, SHARED_MODIFIED);
					
					if(IntersectLocks(memory, thread))
					{
						RaceDetection(memory, location);
					}
				}
				
				else
				{
					memStates.put(memory, SHARED);
				}
			}
		}
		
		// State changes when the current state of the memory is SHARED.
		else if(currentState == SHARED)
		{
			if(read_write)
			{
				if(IntersectLocks(memory, thread))
				{
					RaceDetection(memory, location);
				}
			}
			
			else
			{
				IntersectLocks(memory, thread);
			}
		}
		
		// State changes when the current state of the memory is SHARED-MODIFIED.
		else if(currentState == SHARED_MODIFIED)
		{
			if(IntersectLocks(memory, thread))
			{
				RaceDetection(memory, location);
			}
		}
		
		lastMemAccess.put(memory, location);
	}
	
	/*
	 * Intersect the set of locks between the memory's candidate locks 
	 * and the current thread's locks.
	 */
	private boolean IntersectLocks(Integer memory, Integer thread)
	{
		// Get the current memory's candidate locks.
		HashSet<Integer> memLocks = candidateMemLocks.get(memory);
		
		// Get the current thread's locks.
		LinkedList<Integer> currThreadLocks = threadLocks.get(thread);
		
		/*
		 * Used to contain the intersection between the memory's 
		 * candidate locks and current thread's locks.
		 */
		HashSet<Integer> intersectLocks = new HashSet<Integer>();
		
		/*
		 * Intersection Operation: Add the locks to the HashSet if the lock 
		 * is in both C(v) and L(t).
		 */
		for(Integer lock : memLocks)
		{
			if(currThreadLocks.contains(lock))
			{
				intersectLocks.add(lock);
			}
		}
		
		/*
		 * Have C(v) equal the intersection of the candidate locks of
		 * C(v) and the thread locks of L(t).
		 */
		candidateMemLocks.put(memory, intersectLocks);
		
		// Return false if C(v) = (Empty Set) after C(v) L(t).
		if(memLocks.isEmpty())
		{
			return false;
		}
		
		// Else, return true if the resulting intersection is not an empty set.
		return true;
	}
	
	/*
	 * When a race condition is detected from the algorithm, add the
	 * memory and the code line location of where it occurred.
	 */
	private void RaceDetection(Integer memory, Integer location)
	{
		
	}
	
	/*
	 * This function returns the results of detecting data races during
	 * the runtime of the multithreaded program.
	 */
	public ArrayList<String> Results()
	{
		// Contains strings of the results.
		ArrayList<String> results = new ArrayList<String>();
		results.add("Lockset Analysis -");
		
		// Contains the number of data races detecting within the algorithm.
		String num_raceDetections = "Total # of data race detections: " + lastMemAccess.size();
		
		// Add the # of data race detections into the ArrayList.
		results.add(num_raceDetections);
		
		// Identify the memory locations and code line locations of each data race.
		results.add("Memory Location:\tCode Location:");

		// Get each detection of a data race with its memory location and code line location.
		for (Entry<Integer, Integer> entry : lastMemAccess.entrySet())
		{
			// Get the memory and code locations from the HashMap's Key and Value.
			Integer memory = entry.getKey();
			Integer code = entry.getValue();
			
			// Add the data race occurrence location as a string in the results.
			results.add(memory + "\t" + code);
		}
		
		// Return the results to the testing program.
		return results;
	}
}
