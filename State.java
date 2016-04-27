package template;

import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;

import java.util.ArrayList;

/**
 * Created by yances on 2014-10-29.
 */
public class State {
    public TaskSet tasks_world;
    public TaskSet tasks_carried;
    public Topology.City currentCity;
    int totalCost = 0;
    public State(State s) {
        this.currentCity = s.currentCity;
        this.tasks_carried = s.tasks_carried.clone();
        this.tasks_world = s.tasks_world.clone();
        this.totalCost = s.totalCost;
    }

    public State(Topology.City c, TaskSet world, TaskSet carry) { //initial state only used at the beginning of algorithm
        this.currentCity = c;
        this.tasks_world = world.clone();
        this.tasks_carried = carry.clone();
    }

    public boolean isGoalState() {
        if (tasks_world.isEmpty() && tasks_carried.isEmpty()) {
            return true;
        }
        return false;
    }
    public static void moveTask(Task t, TaskSet from, TaskSet to) {
        if (to.add(t) && from.remove(t)) {  //ok, do nothing
        } else {
            throw new AssertionError("critical error: task move failed. should never happen");
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(currentCity.id);
        s.append('W');
        for (Task t: this.tasks_world) {
            s.append("-" + t.id);
        }
        s.append('C');
        for (Task t: this.tasks_carried) {
            s.append("-" + t.id);
        }
        return s.toString();
    }

    public ArrayList<State> createAllChildStates (int capacity) {
        ArrayList<State> allChildStates = createChildStatesPickup(capacity);
        allChildStates.addAll(createChildStatesDeliver());
        return allChildStates;
    }

    public ArrayList<State> createChildStatesPickup (int capacity) {
        ArrayList<State> children = new ArrayList<State>();
        int surplusCapacity = capacity- this.tasks_carried.weightSum();
        if (surplusCapacity > 0) {
            for (Task t : this.tasks_world) {
                if (surplusCapacity >= t.weight) {
                    State childState = new State(this);//copy constructor
                    moveTask(t, childState.tasks_world, childState.tasks_carried);
                    childState.currentCity = t.pickupCity;
                    childState.totalCost += this.currentCity.distanceTo(t.pickupCity);
                    children.add (childState);
                }
            }
        }
        return children;
    }

    public ArrayList<State> createChildStatesDeliver () {
        ArrayList<State> children = new ArrayList<State>();
        for (Task t: this.tasks_carried) {
            State childState = new State(this);//copy constructor
            if (!childState.tasks_carried.remove(t)) {throw new AssertionError("could not remove task from carried. should never happen");}
            childState.currentCity = t.deliveryCity;
            childState.totalCost += this.currentCity.distanceTo(t.deliveryCity);
            children.add (childState);
        }
        return children;
    }
}
