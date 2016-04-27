package template;

import java.util.List;

import template.ReactiveTemplate.Statetransfer;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReinforcementLearning {
	TaskDistribution task;
	List<City> citylist;
	public ReinforcementLearning(TaskDistribution t, List<City> list, double lembda){
		this.task=t;
		this.citylist= list;	
		ReinforcementLearning.LEMBDA=lembda; //discount factor
	}
	public static final int STATE=81;
	public static final int ACTION=10;
	public static  double LEMBDA;
	final static int TOTALCITY=9;
	double [][]Q=new double[STATE][ACTION];
    int []Policy1=new int[STATE];
	int []Policy2=new int[STATE];
	double [] V1=new double[STATE];
	double [] V2=new double[STATE];

	public int[] learningprocess(Topology topology, TaskDistribution td){
		Statetransfer statetransfer=new Statetransfer(td, topology.cities());
		double [][]rcopy=statetransfer.rewardtable();
		double[][][] tcopy=statetransfer.buildtable();
		double valuemax=0;
		int index=0;
		int count=0;
		//initial V
		boolean Converge=false;
		for (int i = 0; i < V1.length; i++) {
			V1[i] =0;	
			V2[i] =0;
		}
		while(!Converge){
		for (int i=0; i<STATE; i++){
			valuemax=0;
			for(int j=0;j<ACTION; j++){
				Q[i][j]=rcopy[i][j];
				for(int k=0;k<STATE;k++){
					Q[i][j]=Q[i][j]+LEMBDA*tcopy[i][j][k]*V1[k];
				}
				if(Q[i][j]>valuemax){
					valuemax=Q[i][j];					
					index=j;
				}				
			}
			V1[i]=valuemax;
			Policy1[i]=index;
		}
		count++;
		double totalDiff = 0;
		for (int i = 0; i < V1.length; i++) {
			totalDiff += Math.abs(V1[i] - V2[i]);
			V2[i] = V1[i];
			Policy2[i]=Policy1[i];
		}
		System.out.println("totalDiff="+totalDiff);
	    Converge = (totalDiff < 0.5);
	}
		System.out.println("loop for converge"+count);
	return Policy1;
	}
}