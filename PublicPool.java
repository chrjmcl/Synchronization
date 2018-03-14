package jnachos.project1;

import jnachos.kern.sync.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import jnachos.kern.*;

public class PublicPool {
	static int K, L, N;
	static Semaphore[] obstacle, lifeguard;
	static Semaphore mutexGuard = new Semaphore("mutexGuard", 1);
	static Semaphore mutexChild = new Semaphore("mutexChild", 1);
	static Semaphore mutexParent = new Semaphore("mutexParent", 1);
	static ArrayList<Integer> finished, toWatch, isWatch, busy, location;
	
	class Lifeguard implements VoidFunctionPtr {
		int myID;
		
		public Lifeguard(int id) {
			myID = id;
		}
		
		public void call(Object haha) {
			while(true) {
				if(!busy.contains(myID)) {
					System.out.println("Guard " + myID + " is going to sleep");
					lifeguard[myID].P();
				}
				for(int i = 0; i < K; i++) {
					if(!toWatch.isEmpty()) {
						if(toWatch.contains(i)) {
							mutexGuard.P();
							System.out.println("Guard " + myID + " is watching obstacle " + i);
							while(toWatch.contains(i)) {
								obstacle[i].V();
								toWatch.remove(toWatch.indexOf(i));
							}
							mutexGuard.V();
						}
					}
				}
				while(busy.contains(myID)) {
					busy.remove(busy.indexOf(myID));
				}
			}
		}	
	}

	class Children implements VoidFunctionPtr {
		int myID;
		
		public Children(int id) {
			myID = id;
		}
		
		public void call(Object haha) {
			for(int i = 0; i < K; i++) {
				boolean flag = true;
				mutexChild.P();
					toWatch.add(i);
				mutexChild.V();
				System.out.println("Child " + myID + " is waiting at obstacle " + i);
				while(flag) {
					mutexChild.P();
					if(busy.size() < L) {
						for(int j = 0; j < L; j++) {
							if(busy.contains(j)) {
								//do nothing
							}
							else {
								busy.add(j);
								lifeguard[j].V();
								flag = false;
								j = L;
							}
						}
					}
					mutexChild.V();
				}
				obstacle[i].P();
				System.out.println("Child " + myID + " is completing obstacle " + i);
			}
			finished.add(myID);
		}
	}
	
	class Parent implements VoidFunctionPtr {
		int myID;
		
		public Parent(int id) {
			myID = id;
		}
		
		public void call(Object haha) {
			System.out.println("Parent " + myID + " is waiting in the sauna");
			boolean sauna = true;
			
			while(sauna) {
				mutexParent.P();
				if(finished.contains(myID)) {
					finished.remove(finished.indexOf(myID));
					System.out.println("Parent " + myID +  " picked up child " + myID);
					sauna = false;
				}
				mutexParent.V();
			}
			
			Parent parent = new Parent(myID);
			(new NachosProcess(new String ("parent "+ myID + "NP"))).fork(parent, null);
			Children child = new Children(myID);
			(new NachosProcess(new String ("child "+ myID + "NP"))).fork(child, null);
		}
		

	}
	
	public PublicPool() {
		runPool();
	}
	
	public void runPool() {
		getParameters();
		initalizeArrays();
		startNachosProcesses();
	}
	
	public void getParameters() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Number of lifeguards on duty (L): ");
			L = (new Integer(reader.readLine())).intValue();
			System.out.println("Number of children allowed in at a time (N): ");
			N = (new Integer(reader.readLine())).intValue();
			System.out.println("Number of obstacles in the pool (K): ");
			K = (new Integer(reader.readLine())).intValue();
				while (K <= L) {
					System.out.println("K must be larger than L");
					System.out.println("Please re enter a K value: ");
					K = (new Integer(reader.readLine())).intValue();
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void initalizeArrays () {		
		obstacle  = new Semaphore[K];
		lifeguard = new Semaphore[L];
		finished  = new ArrayList<Integer>(N);
		busy      = new ArrayList<Integer>(L);
		toWatch   = new ArrayList<Integer>(K);
		isWatch   = new ArrayList<Integer>(K);
		location  = new ArrayList<Integer>(L);
		
		for(int i = 0; i < L; i++) {
			lifeguard[i] = new Semaphore("lifeguard" + i + "Sem", 0);
		}
		for(int i = 0; i < K; i++) {
			obstacle[i] = new Semaphore("obstacle" + i + "Sem", 0);
		}
	}
	
	public void startNachosProcesses() {
		for (int i = 0; i < L; i++) {
			Lifeguard guard = new Lifeguard(i);
			(new NachosProcess(new String ("guard " + i + "NP"))).fork(guard, null);
		}
		for (int i = 0; i < N; i++) {
			Parent parent = new Parent(i);
			(new NachosProcess(new String ("parent "+ i + "NP"))).fork(parent, null);
			Children child = new Children(i);
			(new NachosProcess(new String ("child "+ i + "NP"))).fork(child, null);
		}
	}
}