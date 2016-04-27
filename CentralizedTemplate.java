package template;

//the list of imports
import java.util.*;

import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

    public class SLS {

        public  int iterationtimes = 5000000;
        long startTime = System.currentTimeMillis();
        public C procedureSLS(TaskSet tasks) {
            C init = C.selectInitialSolution();
            if (init.constraints() == false) {
                System.out.println("init solution bad");
            }
            else
            {
                System.out.println("init solution good");
                System.out.println(init);
                System.out.println(init.checkAllTasksHandled());
            }

            System.out.println("starting from initialsolution");
            int thousands = 0;
            for (int i = 0; i < iterationtimes; i++) {
                if (i == 1000) {
                    thousands++;
                    System.out.println("iterationtimes " + thousands  + "000");
                    i = 0;
                    iterationtimes-= 1000;
                    System.out.println(",\ncurrent best solution:" + init);
                }
                C solutionOld = new C(init);
                ArrayList<C> SolutionSet = C.chooseNeighbours(solutionOld);
                if (SolutionSet.size() <= 0) {
                    throw new AssertionError("solutionset 0 assert");
                }
                //System.out.println("solution set size " + SolutionSet.size());
                init = C.localChoice(solutionOld, SolutionSet);//problems may appear
                if (System.currentTimeMillis()-startTime > 150000) { //150 seconds seems fine with logistPlatform
                    System.out.println("time went too long. returning current best solution");
                    System.out.println(init);
                    System.out.println("iterations: " + i);
                    return init;
                }
            }
            return init;
        }

        public Plan slsPlan(Vehicle vehicle, C solution) {
            City current = vehicle.getCurrentCity();
            Plan plan = new Plan(current);
            int taskAction=solution.firstAction[vehicle.id()];
            if (taskAction == C.NULL) {
                return Plan.EMPTY;
            }
            while (taskAction !=C.NULL) {
                City next=C.getCityForAction(taskAction);
                if (current != next) {
                    for(City city: current.pathTo(next)){
                        plan.appendMove(city);
                    }
                }
                plan.append(C.makeLogistAction (taskAction));            //append pikcup or deliver corresponding to makeLogistAction (taskAction)
                taskAction = solution.next[taskAction];
                current = next; //update city for next loop
            }
            System.out.println("receiving plan: ");
            System.out.println(plan);
            return plan;
        }
    }

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;

    //@Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    public void printActionIndexes (TaskSet tasks) {
        for (Task t: tasks) {
            System.out.println("Index: " + 2*t.id + " Task: " + t.id + "  pickup");
            System.out.println("Index: " + (2*t.id +1) + " Task: " + t.id + " deliver ");
        }
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        C.setupOnce(vehicles, tasks); //Static initialisation of C-global variables
        SLS sls = new SLS();
        C solution = sls.procedureSLS(tasks);
        List<Plan> plans = new ArrayList<Plan>();
        for (Vehicle v:vehicles) {
            plans.add (sls.slsPlan(v, solution));
        }
        return plans;
    }


}