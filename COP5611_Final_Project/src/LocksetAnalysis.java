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
	
	/*
	 * Contains sequence of iids on method executions.
	 */
	public static HashMap<Integer, LinkedList<Integer>> threadStacks;
			
	/*
	 * Represents L(t): Sequence of lock identifiers held by a current thread.
	 * Let L(t) be the set of locks held by the thread 't'.
	 */
	public static HashMap<Integer, LinkedList<Integer>> threadLocks;
	
	/*
	 * Represents C(v): Contains sets of candidate locks for each memory location.
	 * For each memory location 'v'. initialize C(v) to be the set of all
	 * candidate locks.
	 */
	public static HashMap<Integer, HashSet<Integer>> candidateMemLocks;
	
	/*
	 * Contains monitoring states of each memory location during
	 * multithreading. During each read/write operation, change the
	 * memory states accordingly to the lock discipline.
	 */
	public static HashMap<Integer, Integer> memStates;
	
	/*
	 * Contains the thread that first accesses memory location.
	 * This variable will be used to record the first thread
	 * that accesses the memory location and be checked if a new
	 * thread is accessing the memory location.
	 */
	public static HashMap<Integer, Integer> firstMemAccess;
	
	/*
	 * Contains code line locations of a read/write operation 
	 * that accesses the memory location. This will be used to
	 * record the read/write operations that occur right before 
	 * the data race detection.
	 */
	public static HashMap<Integer, Integer> lastMemAccess;
	
	// Contains the data race occurrences during lockset analysis.
	public static HashMap<Integer, Integer> races;
	
	/*
	 * Initialize all variables before using the lockset analysis 
	 * program functions.
	 */
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
	
	/*
	 * This function adds a new thread from the test to the 
	 * HashMaps stacks and locks. 
	 */
	public void AddThread(Integer threadID)
	{
		// Contains a temporary LinkedList for the stacks of threads.
		LinkedList<Integer> stacks = new LinkedList<Integer>();
		
		// Contains a temporary LinkedList for the locks of threads.
		LinkedList<Integer> locks = new LinkedList<Integer>();
		
		// Insert the thread identifier and its new empty stack and lock set.
		threadStacks.put(threadID, stacks);
		threadLocks.put(threadID, locks);
	}
	
	/*
	 * Adds a stack or the iid of the method enter execution 
	 * to the current thread.
	 */
	public void AddStack(Integer thread, Integer stack)
	{
		threadStacks.get(thread).add(stack);
	}
	
	/*
	 * This function adds a lock to the current thread 
	 * during multithreading. This function will be used 
	 * when a lock is set on a certain thread.
	 */
	public void AddLock(Integer thread, Integer lock)
	{
		threadLocks.get(thread).add(lock);
	}
	
	/*
	 * This function removes a lock from the current thread 
	 * during multithreading. This function will be used 
	 * when a lock is removed or unlocked on a certain 
	 * thread.
	 */
	public void RemoveLock(Integer thread, Integer lock)
	{
		threadLocks.get(thread).remove(lock);
	}
	
	/*
	 * This function adds a new memory location that will 
	 * be read/written to during multithreading. Each memory 
	 * location will have its own set of candidate locks.
	 */
	public void AddMemory(Integer memory)
	{
		
		HashSet<Integer> locks = new HashSet<Integer>();
		
		candidateMemLocks.put(memory, locks);
		memStates.put(memory, VIRGIN);
	}
	
	/*
	 * Adds a candidate lock to the current memory location. Use this
	 * function to initialize all memory locations to the set of all
	 * candidate locks before multithreading starts.
	 */
	public void AddMemLock(Integer memory, Integer lock)
	{
		candidateMemLocks.get(memory).add(lock);
	}
	
	/*
	 * Check the memory state during each read/write operation of the variable.
	 * The memory location identifier, thread identifier, and code line location should
	 * be given during each read/write operation. The boolean variable read_write
	 * will represent if a read (false) or write (true) operation was done on the
	 * memory.
	 */
	public void MemState(Integer memory, Integer thread, Integer location, boolean read_write)
	{
		// Get the current state of given memory.
		Integer currentState = memStates.get(memory);
		
		/*
		 * State changes made when the current state of 
		 * the memory location is VIRGIN:
		 * 
		 * 1) If the next operation after initialization
		 * is a read operation, then the current state of
		 * the memory location will remain as VIRGIN.
		 * 
		 * 2) If the next operation after initialization is
		 * a write operation, then the current state of 
		 * the memory location will be changed to EXCLUSIVE 
		 * and the thread that first accesses the memory 
		 * location will be recorded.
		 * 
		 * 3) C(v), or the candidate locks for the memory
		 * location of variable 'v' will still remain
		 * during initialization in the VIRGIN state and
		 * will not be update when entering the EXCLUSIVE
		 * state.
		 * 
		 */
		if(currentState == VIRGIN)
		{
			if(read_write)
			{
				memStates.put(memory, EXCLUSIVE);
				firstMemAccess.put(memory, thread);
			}
		}
		
		/*
		 * State changes made when the current state of the 
		 * memory location is EXCLUSIVE:
		 * 
		 * 1) If the next read/write operation is in
		 * the first thread that accessed the memory
		 * location, then the state of the memory location
		 * will remain EXCLUSIVE and C(v) will not be
		 * updated.
		 * 
		 * 2) If the next read operation is in an new
		 * thread that accesses the memory location,
		 * then the current state of the memory location
		 * will be changed to SHARED and C(v) will 
		 * be updated to have candidate locks equal to
		 * the intersection of C(v) and L(t), or locks
		 * of the current thread.
		 * 
		 * 3) If the next write operation is in an new
		 * thread that accesses the memory location,
		 * then the current state of the memory location
		 * will be changed to SHARED-MODIFIED and C(v) will 
		 * be updated to have candidate locks equal to
		 * the intersection of C(v) and L(t), or locks
		 * of the current thread. If the resulting C(v)
		 * is an empty set, then a data race detection
		 * is reported.
		 * 
		 */
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
		
		/*
		 * State changes made when the current state of the 
		 * memory location is SHARED:
		 * 
		 * 1) If the next operation is a read operation
		 * that accesses the memory location, then the 
		 * current state of the memory location
		 * will be remain as SHARED and C(v) will 
		 * be updated to have candidate locks equal to
		 * the intersection of C(v) and L(t), or locks
		 * of the current thread.
		 * 
		 * 2) If the next operation is a write operation 
		 * that accesses the memory location, then the 
		 * current state of the memory location will be
		 * changed to SHARED-MODIFIED and C(v) will 
		 * be updated to have candidate locks equal to
		 * the intersection of C(v) and L(t), or locks
		 * of the current thread. If the resulting C(v)
		 * is an empty set, then a data race detection
		 * is reported.
		 * 
		 */
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
		
		/* 
		 * State changes made when the current state of the 
		 * memory location is SHARED-MODIFIED:
		 * 
		 * 1) For the next read/write operation, C(v) 
		 * will be updated to have candidate locks equal 
		 * to the intersection of C(v) and L(t), or locks
		 * of the current thread. If the resulting C(v)
		 * is an empty set, then a data race detection
		 * is reported.
		 * 
		 */
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
	 * This function will be used to intersect the candidate locks
	 * of the current memory location, C(v), and the locks of the
	 * current thread, L(t), where 'v' is the memory location, and
	 * 't' is the current thread. The function will also check if
	 * the resulting intersection is an empty set, which would mean
	 * that a data race is detected. 
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
		 * Intersection Operation: Add the locks to the HashSet 
		 * if the lock is in both C(v) and L(t).
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
	 * memory location and the code line location of where it occurred.
	 */
	private void RaceDetection(Integer memory, Integer location)
	{
		
	}
	
	/*
	 * This function returns the results of detecting data races during
	 * the runtime of the multithreaded program. These results will be
	 * given an ArrayList of Strings to be used by the testing program,
	 * which contain the number of data race detections and the memory
	 * and code locations of where the data races occur.
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
		for (Entry<Integer, Integer> entry : races.entrySet())
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
