package template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private int[] P;  //policy array size = STATE
	int m=9;
	int n=1;
	public static final int STATE=81;
	public static final int ACTION=10;
	public static final int COSTPERKM=5;
	final static int TOTALCITY=9;

	public static class doubleindex{  //as a container to hold the two coordinates
		public int currentcity=-2;
		public int destination=-2;
	}
	
	//build the state transfer table (know constructor) 
	public static class Statetransfer{
		TaskDistribution task;
		static List<City> citylist;
		
		public Statetransfer(TaskDistribution t, List<City> list){
			this.task=t;
			Statetransfer.citylist= list;		
		}

		double [][][] B=new double[STATE][ACTION][STATE];
		double [][]R=new double[STATE][ACTION];

		public boolean neighbours (int city1, int city2) {
			City c=citylist.get(city1);//object to use the method in topology class
			City d=citylist.get(city2);//this index may not match, we tried and we assume it will
			boolean m=c.hasNeighbor(d);
			return m;			
		}

		public double getdistance(int city1, int city2){
			City c=citylist.get(city1);//object to use the method in topology class
			City d=citylist.get(city2);//this index may not match, we tried and we assume it will
			double distance=c.distanceTo(d);
			return distance;	
		}
		
		
		
		public static City getcity(int index){
			City c=citylist.get(index);
			return c;
		}

		public double[][][] buildtable(){

			for (int i=0; i<STATE; i++){
				for(int j=0;j<ACTION; j++){
					for(int k=0; k<STATE;k++){
						int cityfrom=getcityid(i,TOTALCITY).currentcity;
						int cityto=actionindex(TOTALCITY,j,i);
						int taskcityto=getcityid(k,TOTALCITY).destination;
						int taskcityfrom=getcityid(k,TOTALCITY).currentcity;

						if((cityfrom!=cityto)&&(taskcityfrom==cityto)&&(taskcityto == -1)){
							if((j==9)||(neighbours(cityto,cityfrom))){
								B[i][j][k]=task.probability( citylist.get(taskcityfrom), null);

							}
						}

						else if((cityfrom!=cityto)&&(taskcityfrom==cityto)&&(taskcityto != -1)){
							if((j==9)||(neighbours(cityto,cityfrom))){
								B[i][j][k]=task.probability( citylist.get(taskcityfrom), citylist.get(taskcityto));

							}
						}
						else B[i][j][k]=0;		
					}
				}			
			}
			return B;
		}
		//build the reward table
		public double[][] rewardtable(){
			for (int i=0; i<STATE; i++){
				for(int j=0;j<ACTION; j++){
					int cityfrom=getcityid(i,TOTALCITY).currentcity;
					int cityto=actionindex(TOTALCITY,j,i);
					if(cityto!=-1){
						R[i][j]=-COSTPERKM*getdistance(cityfrom, cityto);

						if(j==9){
							R[i][j]=R[i][j]+task.reward(citylist.get(cityfrom), citylist.get(cityto));
						}
					}
					if(j==9&&cityto==-1){
						R[i][j]=0;
					}
				}
			}
			return R;			
		}		
		public static int getstateindex(final int currentid, final int targetid, final int totalcity){

			final int NOTASK = -1;
			int index;
			if(targetid== NOTASK){
				index=(currentid+1)*totalcity-1;
				return index;
			}

			index=currentid*totalcity+targetid;
			if(targetid>currentid){
				index--;
			}
			return index;
		}

		public int actionindex(final int totalcity, int actionid, int index){			
			if (actionid< totalcity){
				int citymove=actionid;
				return citymove;
			}
			doubleindex xy= getcityid(index, totalcity);
			int citymove=xy.destination;
			return citymove;	
		}

		public static  doubleindex getcityid(final int index, final int totalcity) {
			doubleindex con=new doubleindex();
			final int NOTASK=-1;
			con.currentcity= index/totalcity;
			con.destination=index%totalcity;

			if(con.destination>=con.currentcity && con.destination!=totalcity-1 ){
				con.destination=con.destination+1;	
				return con;
			}
			if (con.destination==totalcity-1){
				con.destination=NOTASK;

			}
			return con;
		}
	}
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {		
		Statetransfer statetransfer=new Statetransfer(td, topology.cities());
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.8);

		ReinforcementLearning Rlearn= new ReinforcementLearning(td, topology.cities(),discount);//cities() has the list of city
		this.P=Rlearn.learningprocess(topology,td);
		//System.out.println("count equals"+Rlearn.count);
		for (int i=0;i<P.length;i++){
			doubleindex xy= Statetransfer.getcityid(i, 9);
			int test1=xy.currentcity;
			int test2=xy.destination;
			System.out.println("state index is "+i);
			System.out.println("current city is "+test1);
			System.out.println("destination is "+test2);
			System.out.println(P[i]);
		}
		
		//print policy for different discount factors
		System.out.println (" 3728 DISCOUNT FACTOR IS : "+ discount);
		for (int i=0;i<P.length;i++) {
			System.out.println ("state : " + i + "   policy : " + P[i]);
		}
	}


	public void checkTtable(double [][][]C){
		System.out.println(Arrays.deepToString(C));
		double matrixvalue=0;
		int count=0;
		for (int i=0; i<STATE; i++){
			for(int j=0;j<ACTION; j++){
				for(int k=0; k<STATE;k++){
					if (C[i][j][k]!=0){
						System.out.println(i);
						System.out.println(j);
						System.out.println(k);
						System.out.println(C[i][j][k]);

						count++;
					}
				}
			}
		}
		for(int k=0; k<STATE;k++){
			matrixvalue=matrixvalue+C[80][6][k];
		}

		System.out.println( matrixvalue);
		System.out.println(count);	 
	}
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {

		System.out.println("new print  task : " + availableTask);

		System.out.println("currentcity "+vehicle.getCurrentCity());
		Action action;
		City cc = vehicle.getCurrentCity();  //our state representation is a currentcity and a targetcity , meaning task 
		int ccid = cc.id;
		int targetCity = -5;
		if (availableTask == null) {
			targetCity = -1;
		}
		else {
			targetCity = availableTask.deliveryCity.id;
		}
		int indexcity=Statetransfer.getstateindex(ccid, targetCity, TOTALCITY);

		System.out.println("currentstate"+indexcity);

		System.out.println("policy"+P[indexcity]);

		if (P[indexcity]!=9){
			City m=Statetransfer.getcity(P[indexcity]);
			action=new Move(m);
		}
		else
		{
			action = new Pickup(availableTask);	
		}
		return action;
	}
	
	//For reference / checkup
	public void printAllStateIndexes (List <City> cities) {
		int index = 0;

		ArrayList <City> cities2 = new ArrayList <City> (cities);

		for (City c : cities) {
			for (City d: cities2) {
				if (d.id == c.id) { continue;}
				System.out.println ("Index: " + index + " : Currentcity = " + c.id + " " + c.name + " task = " + d.id + " " + d.name );
				index++;
			}		
			System.out.println ("Index: " + index + " : Currentcity = " + c.id + " " + c.name + " task = null");
			index++;
		}		
	}
	
//Test that the mapping from stateindex to doubleindex works as intended
	public void testMapping () {
		for (int i = 0; i< 81 ; i++) {
			doubleindex xy = Statetransfer.getcityid(i, TOTALCITY);
			int out = Statetransfer.getstateindex(xy.currentcity, xy.destination, TOTALCITY);
			System.out.println ("Statetransfer testing for index " + i + "  and result: " + (i==out));
		}
	}
	
//Test that the mapping from doubleindex to stateindex works as intended
	public void testMappingBackwards () {
		for (int cc = 0; cc<9; cc++) {
			for (int tc = -1; tc < 9; tc++ ) {
				if (tc == cc ) {continue;}  //we dont have states for this
				int stateindex = Statetransfer.getstateindex (cc,tc,TOTALCITY);
				doubleindex out = Statetransfer.getcityid(stateindex, TOTALCITY);
				System.out.println ("Statetransfer testing backwards index "
						+ cc + ", " + tc + " and result: " 
						+ (out.currentcity == cc && out.destination == tc));
			}				
		}
	}

}











