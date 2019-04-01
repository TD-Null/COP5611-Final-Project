/*
 * Timothy Deligero
 * Apurv Parekh
 * COP5611 Final Project: Data Race Detection
 */

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.*;

class ThreadTest implements Runnable
{
	Thread t;
	
	long balance = 10;

	public ThreadTest(int id)
	{
		
	}
	
	@Override
	public void run() 
	{
		
	}
	
	public void withdraw()
	{
		balance -= 5;
	}
	
	public void deposit()
	{
		balance += 5;
	}
}

public class Test 
{
	public static void main (String[] args)
    {
		int num_threads = 5;
		
    }
}
