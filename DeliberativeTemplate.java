package template;

/* import table */
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import javax.swing.tree.DefaultMutableTreeNode;
import  javax.swing.tree.TreeNode;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {
    enum Algorithm {BFS, ASTAR}
    /* Environment */
    Topology topology;
    TaskDistribution td;

    /* the properties of the agent */
    Agent agent;
    int capacity;
    /* the planning class */
    Algorithm algorithm;

    public void setup(Topology topology, TaskDistribution td, Agent agent) {
        this.topology = topology;
        this.td = td;
        this.agent = agent;
        // initialize the planner
        this.capacity = agent.vehicles().get(0).capacity(); //could/should be made safe (one-time set), but we don't have to
        String algorithmName = agent.readProperty("algorithm", String.class, "BFS");
        // Throws IllegalArgumentException if algorithm is unknown
        algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
        // ...
    }


    @Override
    public Plan plan(Vehicle vehicle, TaskSet tasks) {
        //return naivePlan (vehicle,tasks);
        State s = new State (vehicle.getCurrentCity(), tasks, vehicle.getCurrentTasks());
        long t0  = System.currentTimeMillis();
        switch (algorithm) {
            case ASTAR:
                System.out.println("runnign ASTAR");
                Plan p = Optimal(s, Algorithm.ASTAR);
                System.out.println("time took: " + (System.currentTimeMillis()-t0));
                printPlan(p);
                return p;//Optimal(s, Algorithm.BFS);
            case BFS:
                System.out.println("runnign BFS");
                p = Optimal(s, Algorithm.BFS);
                System.out.println("time took: " + (System.currentTimeMillis()-t0));
                printPlan(p);
                return p;//Optimal(s, Algorithm.BFS);
            default:
                throw new AssertionError("Algorithm not in the predefined set of algorithms. Should not happen.");
        }
    }


    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity))
                plan.appendMove(city);

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path())
                plan.appendMove(city);

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }


    public void printPlan(Plan p) {
        for (Action a: p)
            System.out.println(a);
    }

    public Comparator<DefaultMutableTreeNode> getComparator(Algorithm algorithm) {
        if (algorithm == Algorithm.ASTAR) {
            return new Comparator<DefaultMutableTreeNode>() {
                @Override
                public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
                    double c1 = getStateFromTreeNode(o1).totalCost + heuristicCost(o1);
                    double c2 = getStateFromTreeNode(o2).totalCost + heuristicCost(o2);
                    if (c1 > c2) { return 1;}
                    if (c1 < c2) { return -1;}
                    return 0;
                }
            };
        }

        if (algorithm == Algorithm.BFS) {
            return new Comparator<DefaultMutableTreeNode>() {
                @Override
                public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
                    double c1 = getStateFromTreeNode(o1).totalCost;
                    double c2 = getStateFromTreeNode(o2).totalCost;
                    if (c1 > c2) { return 1;}
                    if (c1 < c2) { return -1;}
                    return 0;
                }
            };
        }
        throw new AssertionError("Algorithm not found: " + algorithm);
    }

    public Plan Optimal(State initialState, Algorithm algorithm) {
        Comparator<DefaultMutableTreeNode> comparator = getComparator(algorithm);
        PriorityQueue<DefaultMutableTreeNode> checknodelist = new PriorityQueue<DefaultMutableTreeNode>(600000 , comparator);
        HashMap <String, Integer> alreadyVisited = new HashMap<String, Integer>(100000);
        checknodelist.add(new DefaultMutableTreeNode(initialState));
        int totalStateCheckCounter = 0;
        int stateCountPrinter      = 0;
        while (true) {
            totalStateCheckCounter++;
            stateCountPrinter++;
            if (stateCountPrinter == 100000) {System.out.println("States checked: " + totalStateCheckCounter); stateCountPrinter = 0;}

            DefaultMutableTreeNode currentNode = checknodelist.poll();
            State currentState = (State) currentNode.getUserObject();
            if (  currentState.isGoalState()) {
                System.out.println("Total state check counter " + totalStateCheckCounter);
                System.out.println("Total node cost of solution" + getStateFromTreeNode(currentNode).totalCost);
                return buildPlanWithGoalNode(currentNode, initialState.tasks_world, initialState.currentCity);
            }
            else {
                alreadyVisited.put(currentState.toString(),0);
                ArrayList<State> childStates = currentState.createAllChildStates(this.capacity);
                for (State sChild  : childStates) { //check new states vs the hashmap before adding them to the hashmap or to the tree
                    if (!alreadyVisited.containsKey(sChild.toString())) {
                        DefaultMutableTreeNode nChild  = new DefaultMutableTreeNode(sChild);
                        currentNode.add(nChild); //It appears the order here is critical, must be added to parent (tree) before checklist (prio-queue). WHY?
                        checknodelist.add(nChild);
                    }
                }
            }
        }
    }

    public Plan buildPlanWithGoalNode (DefaultMutableTreeNode n, TaskSet ts, City initialCity) {
        Plan initialplan=new Plan(initialCity);
        TreeNode[] path = n.getPath();
        for (int steps = 0; steps < path.length - 1; steps++) {
            State s0 = getStateFromTreeNode(path[steps]);
            State s1 = getStateFromTreeNode(path[steps + 1]);
            Plan stepplan = partialplan(s0, s1, ts);
            for(Action a:stepplan){
                initialplan.append(a);
            }
        }
        return initialplan;
    }



    public State getStateFromTreeNode(TreeNode n) {
        //assuming the node is in fact a defaultmutable}
        DefaultMutableTreeNode dn = (DefaultMutableTreeNode) n;
        State s = ((State) dn.getUserObject());
        return s;
    }

    private Plan partialplan(State parent, State child,TaskSet tasks ) {
        Plan subPlan= new Plan(parent.currentCity);
        TaskSet delivered = tasks.intersectComplement(parent.tasks_carried, child.tasks_carried);
        //did we deliver something?
        if (!delivered.isEmpty()) {
            for (Task t : delivered) {//there is only 1 task, but no other way to take it out of the TaskSet
                for (City city : parent.currentCity.pathTo(t.deliveryCity)) {
                    subPlan.appendMove(city);
                }
                subPlan.append(new Action.Delivery(t));
            }
            return subPlan;
        }
        TaskSet pickuptask = tasks.intersectComplement(parent.tasks_world, child.tasks_world);
        if (!pickuptask.isEmpty()) {
            for (Task t : pickuptask) { //there is only 1 task, but no other way to take it out of the TaskSet
                for (City city : parent.currentCity.pathTo(t.pickupCity)) {
                    subPlan.appendMove(city);
                }
                subPlan.append(new Action.Pickup(t));
            }
            return subPlan;
        }
        throw new AssertionError(" no action founded.");
    }

    @Override
    public void planCancelled(TaskSet carriedTasks) {
        if (!carriedTasks.isEmpty()) { //typically
            // you will need to consider the carriedTasks
        }
    }

    public City getCityFromTreeNode(TreeNode n) {
        //assuming the node is in fact a defaultmutable}
        DefaultMutableTreeNode dn = (DefaultMutableTreeNode) n;
        City c = ((State) dn.getUserObject()).currentCity;
        return c;
    }

    public int heuristicCost(DefaultMutableTreeNode n) {
        int heuristic = 0;
        State s = getStateFromTreeNode(n);
        double maxDelivery = 0;
        double maxPickDelivery = 0;
        for (Task h : s.tasks_carried) { //Find biggest delivery distance of currently carried tasks
            if (s.currentCity.distanceTo(h.deliveryCity) >maxDelivery) {
                maxDelivery = s.currentCity.distanceTo(h.deliveryCity);
            }
        }
        for (Task p : s.tasks_world) {  //Find biggest cost from pickup->delivery of world tasks
            double costcurrent=p.pickupCity.distanceTo(p.deliveryCity)+s.currentCity.distanceTo(p.pickupCity);
            if(costcurrent>maxPickDelivery) {
                maxPickDelivery = costcurrent;
            }
        }
        if (maxDelivery>maxPickDelivery){
            heuristic=(int)maxDelivery;
        }
        else
            heuristic=(int)maxPickDelivery;
        return heuristic;
    }
}